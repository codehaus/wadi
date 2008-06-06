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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.wadi.servicespace.InvocationResult;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SuccessfullSetResultCombinerTest extends RMockTestCase {

    public void testCombineSet() throws Exception {
        Integer one = Integer.valueOf(1);
        InvocationResult result1 = new InvocationResult(Collections.singleton(one));
        Integer two = Integer.valueOf(2);
        InvocationResult result2 = new InvocationResult(Collections.singleton(two));
        Collection results = new ArrayList();
        results.add(result1);
        results.add(result2);
        
        InvocationResult result = SuccessfullSetResultCombiner.COMBINER.combine(results);
        Set combinedResults = (Set) result.getResult();
        Set expectedResults = new HashSet();
        expectedResults.add(one);
        expectedResults.add(two);
        assertEquals(expectedResults, combinedResults);
    }
    
    public void testSkipFailure() throws Exception {
        Integer one = Integer.valueOf(1);
        InvocationResult result1 = new InvocationResult(Collections.singleton(one));
        InvocationResult result2 = new InvocationResult(new Exception());
        Collection results = new ArrayList();
        results.add(result1);
        results.add(result2);
        
        InvocationResult result = SuccessfullSetResultCombiner.COMBINER.combine(results);
        Set combinedResults = (Set) result.getResult();
        assertEquals(Collections.singleton(one), combinedResults);
    }
    
}
