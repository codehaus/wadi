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
package org.codehaus.wadi.relocation;

import org.codehaus.wadi.core.MotableBusyException;
import org.codehaus.wadi.core.contextualiser.BasicInvocation;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.test.AbstractTwoNodesTest;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractTestRelocation extends AbstractTwoNodesTest {

    public void testSuccessfulSessionRelocation() throws Exception {
        Session session = red.getManager().create(null);

        session = executeTestSuccessfulRelocation(session);
        
        executeTestThrowExceptionIfSessionIsBuzy(session);
    }

    private Session executeTestSuccessfulRelocation(Session session) throws Exception {
        Invocation invocation = new BasicInvocation(session.getName(), 1000);
        invocation.setErrorIfSessionNotAcquired(true);
        boolean success = green.getManager().contextualise(invocation);
        assertTrue(success);

        success = red.getManager().contextualise(invocation);
        assertTrue(success);
        return invocation.getSession();
    }

    private void executeTestThrowExceptionIfSessionIsBuzy(Session session) throws Exception {
        session.getReadWriteLock().writeLock().acquire();
        
        Invocation invocation = new BasicInvocation(session.getName(), 1000);
        invocation.setErrorIfSessionNotAcquired(true);
        try {
            green.getManager().contextualise(invocation);
            fail();
        } catch (MotableBusyException e) {
        }
    }
    
}
