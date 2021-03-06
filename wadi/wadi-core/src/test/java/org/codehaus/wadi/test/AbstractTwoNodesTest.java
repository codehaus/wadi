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
package org.codehaus.wadi.test;

import java.net.URI;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.manager.DummyRouter;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractTwoNodesTest extends TestCase {
    protected Log log = LogFactory.getLog(getClass());

    protected Dispatcher redD;
    protected StackContext red;
    protected Dispatcher redSSDispatcher;

    protected Dispatcher greenD;
    protected StackContext green;
    protected Dispatcher greenSSDispatcher;

    protected void setUp() throws Exception {
        redD = newDispatcher("red");
        redD.start();

        greenD = newDispatcher("green");
        greenD.start();

        red = newStack(redD);
        ServiceSpace redServiceSpace = red.getServiceSpace();
        redServiceSpace.start();
        redSSDispatcher = redServiceSpace.getDispatcher();

        green = newStack(greenD);
        ServiceSpace greenServiceSpace = green.getServiceSpace();
        greenServiceSpace.start();
        greenSSDispatcher = greenServiceSpace.getDispatcher();
        
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redSSDispatcher, greenSSDispatcher }, 5000);
    }

    private StackContext newStack(Dispatcher dispatcher) throws Exception {
        StackContext stack = new StackContext(new ServiceSpaceName(new URI("Space")), dispatcher) {
            protected Router newRouter() {
                return new DummyRouter();
            }
        };
        stack.build();
        return stack;
    }

    protected void tearDown() throws Exception {
        redD.stop();
        greenD.stop();
    }
    
    protected abstract Dispatcher newDispatcher(String name) throws Exception;

}
