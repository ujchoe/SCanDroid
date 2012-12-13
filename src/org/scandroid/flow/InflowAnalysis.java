/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.flow;


//import static util.MyLogger.LogLevel.DEBUG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.scandroid.domain.CodeElement;
import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.IKFlow;
import org.scandroid.spec.CallArgSourceSpec;
import org.scandroid.spec.CallRetSourceSpec;
import org.scandroid.spec.EntryArgSourceSpec;
import org.scandroid.spec.ISpecs;
import org.scandroid.spec.SourceSpec;
import org.scandroid.spec.StaticFieldSourceSpec;
import org.scandroid.util.CGAnalysisContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.collect.Maps;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;


public class InflowAnalysis <E extends ISSABasicBlock> {
	private static final Logger logger = LoggerFactory.getLogger(InflowAnalysis.class);

    public static <E extends ISSABasicBlock>
    void addDomainElements(
            Map<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> taintMap, 
            BasicBlockInContext<E> block, 
            FlowType taintType, 
            Set<CodeElement> newElements) {
        Map<FlowType<E>,Set<CodeElement>> blockMap = taintMap.get(block);
        if(blockMap == null) {
            blockMap = new HashMap<FlowType<E>,Set<CodeElement>>();
            taintMap.put(block, blockMap);
        }
        
        Set<CodeElement> elements = blockMap.get(taintType);
        if(elements == null) {
            elements = new HashSet<CodeElement>();
            blockMap.put(taintType, elements);
        }
        elements.addAll(newElements);
    }

    public static <E extends ISSABasicBlock>
    void addDomainElement(
            Map<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> taintMap,
            BasicBlockInContext<E> block,
            FlowType taintType,
            CodeElement element) {
        
        Set<CodeElement> elements = new HashSet<CodeElement>();
        elements.add(element);
        addDomainElements(taintMap, block, taintType, elements);
    }

    
    private static<E extends ISSABasicBlock> 
    void processInputSource(CGAnalysisContext<E> ctx,
    						Map<BasicBlockInContext<E>,
                            Map<FlowType<E>,Set<CodeElement>>> taintMap, 
                            SourceSpec ss, 
                            CallGraph cg, 
                            ISupergraph<BasicBlockInContext<E>, CGNode> graph,
                            ClassHierarchy cha,
                            PointerAnalysis pa) {
    	int[] newArgNums;
    	for (IMethod im:ss.getNamePattern().getPossibleTargets(cha)) {
            newArgNums = (ss.getArgNums() == null) ? SourceSpec.getNewArgNums((im.isStatic())?im.getNumberOfParameters():im.getNumberOfParameters()-1) : ss.getArgNums();
            for (CGNode node: cg.getNodes(im.getReference())) {
                BasicBlockInContext<E>[] entriesForProcedure = graph.getEntriesForProcedure(node);
                if (entriesForProcedure == null || 0 == entriesForProcedure.length) {
                    continue;
                }
            
                for (BasicBlockInContext<E> bb:entriesForProcedure) {
                    ss.addDomainElements(ctx, taintMap, im, bb, null, newArgNums, graph, pa, cg);
                }
            }
    	}
    }
    
    private static<E extends ISSABasicBlock> 
    void processStaticFieldSource(CGAnalysisContext<E> ctx, 
    						Map<BasicBlockInContext<E>,
                            Map<FlowType<E>,Set<CodeElement>>> taintMap, 
                            StaticFieldSourceSpec ss, 
                            CallGraph cg, 
                            ISupergraph<BasicBlockInContext<E>, CGNode> graph,
                            ClassHierarchy cha,
                            PointerAnalysis pa) {
    	// get the first block:
    	BasicBlockInContext<E> bb = null;
    	for (CGNode n : cg.getEntrypointNodes() ){
    		 bb = graph.getEntriesForProcedure(n)[0];
    	}
    	
    	if ( null == bb ) {
    		logger.error("Could not find entry basic block.");
    	}
    	
    	ss.addDomainElements(ctx, taintMap, bb.getMethod(), bb, null, null, graph, pa, cg);
    }
    
    private static<E extends ISSABasicBlock> 
    void processFunctionCalls(CGAnalysisContext<E> ctx,
    						  Map<BasicBlockInContext<E>,
                              Map<FlowType<E>,Set<CodeElement>>> taintMap, 
                              ArrayList<SourceSpec> ssAL, ISupergraph<BasicBlockInContext<E>, CGNode> graph, 
                              PointerAnalysis pa, 
                              ClassHierarchy cha, CallGraph cg) {
    	Collection<IMethod> targets = new HashSet<IMethod>();
    	ArrayList<Collection<IMethod>> targetList = new ArrayList<Collection<IMethod>>();
    	
    	for (int i = 0; i < ssAL.size(); i++) {
    		Collection<IMethod> tempList = ssAL.get(i).getNamePattern().getPossibleTargets(cha);
    		targets.addAll(tempList);
    		targetList.add(tempList);
    	}
    	
		Iterator<BasicBlockInContext<E>> graphIt = graph.iterator();
		while (graphIt.hasNext()) {
			BasicBlockInContext<E> block = graphIt.next();
			Iterator<SSAInstruction> instructions = block.iterator();

			while (instructions.hasNext()) {
				SSAInstruction inst = instructions.next();

				if (!(inst instanceof SSAInvokeInstruction)) {
					continue;
				}

				SSAInvokeInstruction invInst = (SSAInvokeInstruction) inst;
				for (IMethod target : cha.getPossibleTargets(invInst.getDeclaredTarget())) {
					if (targets.contains(target)) {
						for (int i = 0; i < targetList.size(); i++) {
							if (targetList.get(i).contains(target)) {

								int[] argNums = ssAL.get(i).getArgNums();
								argNums = (argNums == null) ? SourceSpec.getNewArgNums((target.isStatic())?target.getNumberOfParameters():target.getNumberOfParameters()-1) : argNums;

								ssAL.get(i).addDomainElements(ctx, taintMap, target, block, invInst, argNums, graph, pa, cg);

							}
						}
					}
				}
			}
		}
    }

    public static <E extends ISSABasicBlock>
      Map<BasicBlockInContext<E>,Map<FlowType<E>,Set<CodeElement>>> analyze(
            CGAnalysisContext<E> analysisContext, Map<InstanceKey, String> prefixes,
            ISpecs s) {
        return analyze(analysisContext, analysisContext.cg, analysisContext.getClassHierarchy(), analysisContext.graph, analysisContext.pa, prefixes, s);
    }

    public static <E extends ISSABasicBlock>
      Map<BasicBlockInContext<E>,Map<FlowType<E>,Set<CodeElement>>> 
    analyze(CGAnalysisContext<E> ctx,
    	  CallGraph cg, 
          ClassHierarchy cha, 
          ISupergraph<BasicBlockInContext<E>, CGNode> graph,
          PointerAnalysis pa, 
          Map<InstanceKey, String> prefixes,
          ISpecs s) {

        logger.debug("***************************");
        logger.debug("* Running inflow analysis *");
        logger.debug("***************************");

        Map<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> taintMap = Maps.newHashMap();

        SourceSpec[] ss = s.getSourceSpecs();
        logger.debug(ss.length + " Source Specs. ");
        
        ArrayList<SourceSpec> ssAL = new ArrayList<SourceSpec>();
        for (int i = 0; i < ss.length; i++) {
        	if (ss[i] instanceof EntryArgSourceSpec)
        		processInputSource(ctx, taintMap, ss[i], cg, graph, cha, pa);
        	else if (ss[i] instanceof CallRetSourceSpec || ss[i] instanceof CallArgSourceSpec)
        		ssAL.add(ss[i]);
        	else if (ss[i] instanceof StaticFieldSourceSpec) {
        		processStaticFieldSource(ctx, taintMap, (StaticFieldSourceSpec)ss[i], cg, graph, cha, pa);
        	} else 
        		throw new UnsupportedOperationException("Unrecognized SourceSpec");
        } 
        if (!ssAL.isEmpty())
        	processFunctionCalls(ctx, taintMap, ssAL, graph, pa, cha, cg);

        logger.info("************");
        logger.info("* Results: *");
        logger.info("************");
        for(Entry<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> e:taintMap.entrySet())
        {
            for(Entry<FlowType<E>,Set<CodeElement>> e2:e.getValue().entrySet())
            {
                for(CodeElement o:e2.getValue())
                {
                    if (e2.getKey() instanceof IKFlow) {
                    	InstanceKey e2IK = ((IKFlow)e2.getKey()).getIK();
                    	if (prefixes.containsKey(e2IK))
                    			logger.debug("Uri Prefix: " + prefixes.get(e2IK));                    	
                    }
                    logger.debug("\tBasicBlockInContext: "+e.getKey()+"\n\tFlowType: "+e2.getKey()+"\n\tCodeElement: "+o+"\n");

                }
            }
        }

        return taintMap;
    }

}
