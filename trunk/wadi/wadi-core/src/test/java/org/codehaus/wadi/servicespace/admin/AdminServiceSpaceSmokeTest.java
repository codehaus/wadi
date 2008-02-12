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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;

import junit.framework.TestCase;

import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfo;
import org.codehaus.wadi.location.balancing.PartitionInfo;
import org.codehaus.wadi.location.partitionmanager.PartitionMapper;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.admin.commands.GetPartitionBalancingInfos;
import org.codehaus.wadi.servicespace.admin.commands.GetServiceSpaceInfos;
import org.codehaus.wadi.servicespace.admin.commands.GetSessionInfos;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AdminServiceSpaceSmokeTest extends TestCase {

    public void testCommandAggregation() throws Exception {
        VMBroker broker = new VMBroker("CLUSTER");

        AdminServiceSpace adminServiceSpace1 = newAdminServiceSpace(newDispatcher(broker, "node1"));
        newAdminServiceSpace(newDispatcher(broker, "node2"));
        newAdminServiceSpace(newDispatcher(broker, "node3"));
        
        // Implementation note: we need to wait for the starting and started events to be delivered.
        Thread.sleep(200);
        
        CommandEndPoint commandEndPoint = adminServiceSpace1.getCommandEndPoint();
        Integer count = (Integer) commandEndPoint.execute(new CountResultsCommand());
        assertEquals(3, count.intValue());
    }

    public void testSmokeWithStackContext() throws Exception {
        VMBroker broker = new VMBroker("CLUSTER");
        
        VMDispatcher disp1 = newDispatcher(broker, "node1");
        AdminServiceSpace adminServiceSpace1 = newAdminServiceSpace(disp1);
        
        VMDispatcher disp2 = newDispatcher(broker, "node2");
        newAdminServiceSpace(disp2);
        
        VMDispatcher disp3 = newDispatcher(broker, "node3");
        newAdminServiceSpace(disp3);
        
        StackContext ctx1OnPeer1 = newStackContext(disp1, "space1");
        ServiceSpace space1OnPeer1 = ctx1OnPeer1.getServiceSpace();
        Manager mngOnSpace1OnPeer1 = (Manager) space1OnPeer1.getServiceRegistry().getStartedService(Manager.NAME);
        newStackContext(disp2, "space1");
        newStackContext(disp2, "space2");
        newStackContext(disp3, "space2");
        
        // Implementation note: we need to wait for the starting and started events to be delivered.
        Thread.sleep(200);
        
        testGetServiceSpaceInfos(adminServiceSpace1);
        testGetPartitionBalancingInfos(adminServiceSpace1, disp1, disp2);
        testGetSessionInfos(adminServiceSpace1, mngOnSpace1OnPeer1);
    }

    private void testGetSessionInfos(AdminServiceSpace adminServiceSpace, Manager manager) throws Exception {
        CommandEndPoint commandEndPoint = adminServiceSpace.getCommandEndPoint();
        
        manager.createWithName("1");
        manager.createWithName("2");
        
        Set sessionInfos = (Set) commandEndPoint.execute(new GetSessionInfos(new ServiceSpaceName(new URI("space1"))));
        assertEquals(2, sessionInfos.size());
    }

    private void testGetPartitionBalancingInfos(AdminServiceSpace adminServiceSpace, VMDispatcher disp1, VMDispatcher disp2) throws URISyntaxException {
        CommandEndPoint commandEndPoint = adminServiceSpace.getCommandEndPoint();
        GetPartitionBalancingInfos command = new GetPartitionBalancingInfos(new ServiceSpaceName(new URI("space1")));
        PartitionBalancingInfo balancingInfo = (PartitionBalancingInfo) commandEndPoint.execute(command);
        PartitionInfo[] partitionInfos = balancingInfo.getPartitionInfos();
        int nbPartitionsOnPeer1 = 0;
        int nbPartitionsOnPeer2 = 0;
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            if (partitionInfo.getOwner().equals(disp1.getCluster().getLocalPeer())) {
                nbPartitionsOnPeer1++;
            } else if (partitionInfo.getOwner().equals(disp2.getCluster().getLocalPeer())) {
                nbPartitionsOnPeer2++;
            } else {
                fail();
            }
        }
        assertEquals(12, nbPartitionsOnPeer1);
        assertEquals(12, nbPartitionsOnPeer2);
    }

    private CommandEndPoint testGetServiceSpaceInfos(AdminServiceSpace adminServiceSpace) {
        CommandEndPoint commandEndPoint = adminServiceSpace.getCommandEndPoint();
        Set serviceSpaceInfos = (Set) commandEndPoint.execute(new GetServiceSpaceInfos());
        // 3 Admin ServiceSpaces plus 4 customs
        assertEquals(3 + 4, serviceSpaceInfos.size());
        return commandEndPoint;
    }

    private StackContext newStackContext(VMDispatcher dispatcher, String spaceName) throws Exception {
        StackContext context = new StackContext(new ServiceSpaceName(new URI(spaceName)), dispatcher) {
            protected Contextualiser newReplicaAwareContextualiser(Contextualiser next) {
                return next;
            }
            protected PartitionMapper newPartitionMapper() {
                return new PartitionMapper() {
                    public int map(Object key) {
                        return Integer.parseInt((String) key);
                    }
                };
            }
        };
        context.build();
        context.getServiceSpace().start();
        return context;
    }

    private AdminServiceSpace newAdminServiceSpace(VMDispatcher dispatcher) throws Exception {
        AdminServiceSpace adminServiceSpace = new AdminServiceSpace(dispatcher,
            new JDKClassIndexerRegistry(new DeclaredMemberFilter()),
            new SimpleStreamer());
        adminServiceSpace.start();
        return adminServiceSpace;
    }

    private VMDispatcher newDispatcher(VMBroker broker, String peerName) throws MessageExchangeException {
        VMDispatcher dispatcher = new VMDispatcher(broker, peerName, null);
        dispatcher.start();
        return dispatcher;
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
