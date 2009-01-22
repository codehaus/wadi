/**
 * Copyright 2007 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.servicespace.resultcombiner;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.wadi.servicespace.InvocationResult;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FirstSuccessThenFailureCombinerTest extends TestCase {

    public void testFirstSuccessIsReturned() throws Exception {
        Collection results = new ArrayList();
        results.add(new InvocationResult(new Throwable()));
        InvocationResult expectedResult = new InvocationResult(new Object());
        results.add(expectedResult);
        results.add(new InvocationResult(new Object()));
        
        InvocationResult result = FirstSuccessThenFailureCombiner.COMBINER.combine(results);
        assertSame(expectedResult, result);
    }

    public void testFailureIsReturned() throws Exception {
        Collection results = new ArrayList();
        InvocationResult expectedResult = new InvocationResult(new Throwable());
        results.add(expectedResult);
        results.add(expectedResult);
        
        InvocationResult result = FirstSuccessThenFailureCombiner.COMBINER.combine(results);
        assertSame(expectedResult, result);
    }

}
