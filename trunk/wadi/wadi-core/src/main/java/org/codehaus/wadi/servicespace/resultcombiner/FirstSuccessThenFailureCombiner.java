/**
 * Copyright 2006 The Apache Software Foundation
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
import java.util.Iterator;

import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FirstSuccessThenFailureCombiner implements InvocationResultCombiner {
    public static final InvocationResultCombiner COMBINER = new FirstSuccessThenFailureCombiner();

    protected FirstSuccessThenFailureCombiner() {
    }
    
    public InvocationResult combine(Collection invocationResults) {
        if (null == invocationResults || invocationResults.isEmpty()) {
            throw new IllegalArgumentException("No InvocationResults are provided");
        }
        
        InvocationResult firstFailure = null;
        for (Iterator iter = invocationResults.iterator(); iter.hasNext();) {
            InvocationResult result = (InvocationResult) iter.next();
            if (result.isSuccess()) {
                if (shouldReturn(result)) {
                    return result;
                }
            } else {
                firstFailure = result;
            }
        }
        return firstFailure;
    }
    
    protected boolean shouldReturn(InvocationResult result) {
        return true;
    }
    
}
