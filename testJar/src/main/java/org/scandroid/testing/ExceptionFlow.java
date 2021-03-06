/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
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
package org.scandroid.testing;

public class ExceptionFlow {

	public static void exceptionFlow(String[] args) {
		SourceSink.sink(useFlow(args));
	}

	private static String useFlow(String[] args) {
		try {
			thrower(args[0]);
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}
	
	private static Exception useFlow2(Exception e) {
		try {
			throwerE(e);
		}
		catch (Exception e1) {
			return e1;
		}
		return null;
	}
		
	private static String noFlow(String[] args) {
		try {
			thrower_nf(args[0]);
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}
	
	private static String noFlow2(String[] args) {
		try {
			thrower(args[0]);
		} catch (Exception e) {
			Exception e1 = new Exception("no flow");
			return e1.getMessage();
		}
		return null;
	}
	
	public static void thrower(String str) throws Exception {
		throw new Exception(str);
	}
	
	public static void throwerI(Integer i) throws Exception {
		throw new Exception(i.toString());
	}
	
	public static void throwerE(Exception e) throws Exception {
//		throw new Exception("no_flow");
		throw e;
	}
	
	public static void thrower_nf(String str) throws Exception {
		throw new Exception("no flow");
	}

}
