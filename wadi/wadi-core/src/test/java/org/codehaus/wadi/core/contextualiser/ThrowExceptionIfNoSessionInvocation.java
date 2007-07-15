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

import org.codehaus.wadi.core.session.Session;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ThrowExceptionIfNoSessionInvocation extends BasicInvocation {

    public ThrowExceptionIfNoSessionInvocation(String sessionKey, long exclusiveSessionLockWaitTime) {
        super(sessionKey, exclusiveSessionLockWaitTime);
    }

    public void invoke() throws InvocationException {
        Session session = getSession();
        if (null == session) {
            throw new InvocationException("No session bound to invocation");
        }
    }

}
