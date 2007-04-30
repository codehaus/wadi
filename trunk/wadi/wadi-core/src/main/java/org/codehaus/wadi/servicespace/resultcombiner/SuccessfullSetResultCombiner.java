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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SuccessfullSetResultCombiner implements InvocationResultCombiner {
    public static final InvocationResultCombiner COMBINER = new SuccessfullSetResultCombiner();

    protected SuccessfullSetResultCombiner() {
    }

    public InvocationResult combine(Collection invocationResults) {
        Set results = new HashSet();
        
        for (Iterator iter = invocationResults.iterator(); iter.hasNext();) {
            InvocationResult invocationResult = (InvocationResult) iter.next();
            if (invocationResult.isSuccess()) {
                Set tmpResults = (Set) invocationResult.getResult();
                results.addAll(tmpResults);
            }
        }
        
        return new InvocationResult(results);
    }
    
}