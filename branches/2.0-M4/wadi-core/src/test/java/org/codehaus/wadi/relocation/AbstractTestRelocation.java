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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.contextualiser.BasicInvocation;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.test.MyStack;
import org.codehaus.wadi.test.TestUtil;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractTestRelocation extends TestCase {
    protected Log _log = LogFactory.getLog(getClass());
    private Dispatcher redD;
    private Dispatcher greenD;

    protected void setUp() throws Exception {
        redD = newDispatcher("red");
        redD.start();
        // Implementation note: we really need to wait some time to have a "stable" Dispatcher. For instance, in the
        // case of ActiveCluster, 
        Thread.sleep(1000);
        
        greenD = newDispatcher("green");
        greenD.start();
        // Implementation note: we really need to wait some time to have a "stable" Dispatcher. For instance, in the
        // case of ActiveCluster, 
        Thread.sleep(1000);
    }

    protected void tearDown() throws Exception {
        redD.stop();
        greenD.stop();
    }
    
    protected abstract Dispatcher newDispatcher(String name) throws Exception;

    public void testSessionRelocation() throws Exception {
        MyStack red = new MyStack(redD);
        red.start();
        redD = red.getServiceSpace().getDispatcher();

        MyStack green = new MyStack(greenD);
        green.start();
        greenD = green.getServiceSpace().getDispatcher();

        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redD, greenD }, 5000);

        Session session = red.getManager().create(null);
        session.onEndProcessing();
        String name = session.getName();

        assertTrue(name != null);

        Invocation invocation = new BasicInvocation(name);
        boolean success = green.getManager().contextualise(invocation);
        assertTrue(success);

        success = red.getManager().contextualise(invocation);
        assertTrue(success);
    }
    
}
