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
package org.codehaus.wadi.servicespace.admin;

import java.util.Collection;

import junit.framework.TestCase;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AdminServiceSpaceSmokeTest extends TestCase {

    public void testCommandAggregation() throws Exception {
        VMBroker broker = new VMBroker("CLUSTER");

        AdminServiceSpace adminServiceSpace1 = newAdminServiceSpace(broker, "node1");
        newAdminServiceSpace(broker, "node2");
        newAdminServiceSpace(broker, "node3");
        
        // Implementation note: we need to wait for the starting and started events to be delivered.
        Thread.sleep(200);
        
        CommandEndPoint commandEndPoint = adminServiceSpace1.getCommandEndPoint();
        Integer count = (Integer) commandEndPoint.execute(new CountResultsCommand());
        assertEquals(3, count.intValue());
    }

    private AdminServiceSpace newAdminServiceSpace(VMBroker broker, String peerName) throws Exception {
        VMDispatcher dispatcher1 = new VMDispatcher(broker, peerName, null);
        dispatcher1.start();
        
        AdminServiceSpace adminServiceSpace1 = new AdminServiceSpace(dispatcher1);
        adminServiceSpace1.start();
        return adminServiceSpace1;
    }

    private static class CountResultsCommand implements Command {

        public Object execute(LocalPeer localPeer, ServiceSpaceRegistry serviceSpaceRegistry) {
            return Integer.valueOf(1);
        }
        
        public InvocationResultCombiner getInvocationResultCombiner() {
            return new InvocationResultCombiner() {

                public InvocationResult combine(Collection invocationResults) {
                    return new InvocationResult(Integer.valueOf(invocationResults.size()));
                }
                
            };
        }
        
    }
    
}
