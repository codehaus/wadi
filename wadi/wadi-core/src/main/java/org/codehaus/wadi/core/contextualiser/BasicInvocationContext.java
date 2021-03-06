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
package org.codehaus.wadi.core.contextualiser;



/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInvocationContext implements InvocationContext {

    private final Invocation invocation;

    public BasicInvocationContext(Invocation invocation) {
        if (null == invocation) {
            throw new IllegalArgumentException("invocation is required");
        }
        this.invocation = invocation;
    }


    public Invocation getInvocation() {
        return invocation;
    }

}
