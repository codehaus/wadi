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
package org.codehaus.wadi.location.partitionmanager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.location.balancing.PartitionBalancerSingletonServiceHolder;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfoUpdate;
import org.codehaus.wadi.location.balancing.PartitionInfo;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdate;
import org.codehaus.wadi.location.partition.PartitionRepopulationException;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition;
import org.codehaus.wadi.location.statemanager.SimpleStateManager;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * @version $Revision:1815 $
 */
public class SimplePartitionManagerSmokeTest extends RMockTestCase {
    private static final long EXCHANGE_TIMEOUT = 6000;

    private Exception failureException;
    private CountDownLatch failureLatch;
    private int nbPartitions;
    private volatile int nbOperations;
    private volatile int nbRebalancing;

    protected void setUp() throws Exception {
        nbPartitions = 12;
        failureLatch = new CountDownLatch(1);
    }

    public void testRebalancingUnderLoad() throws Exception {
        VMBroker broker = new VMBroker("broker");
        
        List<SimplePartitionManager> managers = new ArrayList<SimplePartitionManager>();
        Collection<Thread> loadThreads = new ArrayList<Thread>();
        for (int i = 0; i < 6; i++) {
            Contextualiser contextualiser = (Contextualiser) mock(Contextualiser.class);
            SimplePartitionManager manager = newManager(broker, i, contextualiser);
            managers.add(manager);
            ServiceSpace serviceSpace = manager.getServiceSpace();
            SimpleStateManager stateManager = new SimpleStateManager(serviceSpace, manager, EXCHANGE_TIMEOUT);
            stateManager.start();
            serviceSpace.start();
            loadThreads.add(new LoadThread(stateManager));
            loadThreads.add(new LoadThread(stateManager));
        }
        
        RebalanceThread rebalanceThread = new RebalanceThread(managers);
        rebalanceThread.start();
        
        for (Thread loadThread : loadThreads) {
            loadThread.start();
        }
        
        boolean success = failureLatch.await(10000, TimeUnit.MILLISECONDS);
        rebalanceThread.interrupt();
        for (Thread loadThread : loadThreads) {
            loadThread.interrupt();
        }
        if (success) {
            failureException.printStackTrace();
            fail();
        }
        System.out.println("[" + nbRebalancing + "] successful rebalancing.");
        System.out.println("[" + nbOperations + "] successful invocations.");
    }

    private SimplePartitionManager newManager(VMBroker broker, int index, Contextualiser contextualiser) throws Exception {
        final Dispatcher dispatcher = new VMDispatcher(broker, Integer.toString(index), null);
        dispatcher.start();

        ServiceSpace serviceSpace = new BasicServiceSpace(new ServiceSpaceName(URI.create("serviceSpace")),
            dispatcher,
            new JDKClassIndexerRegistry(new DeclaredMemberFilter()),
            new SimpleStreamer());

        PartitionBalancerSingletonServiceHolder holder =
            (PartitionBalancerSingletonServiceHolder) mock(PartitionBalancerSingletonServiceHolder.class);
        
        SimplePartitionManager manager = new SimplePartitionManager(serviceSpace, nbPartitions,
                new SimplePartitionMapper(nbPartitions),
                holder,
                new SimplePartitionManagerTiming()) {
            @Override
            protected void waitForBoot() throws InterruptedException, PartitionManagerException {
            }  
            @Override
            protected void queueRebalancing() {
            }
            @Override
            protected void repopulate(LocalPartition[] toBePopulated) throws MessageExchangeException, PartitionRepopulationException {
            }
        };
        manager.start();
        return manager;
    }

    private class RebalanceThread extends Thread {
        private List<SimplePartitionManager> managers;
        private int version = 1;
        
        public RebalanceThread(List<SimplePartitionManager> managers) {
            this.managers = managers;
            
            bootManagers();
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(50);
                    version++;
                    doRun();
                    nbRebalancing++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        protected void doRun() {
            Iterator<SimplePartitionManager> managerIter = managers.iterator();
            
            PartitionInfoUpdate[] updates = new PartitionInfoUpdate[nbPartitions];
            for (int i = 0; i < updates.length; i++) {
                if (!managerIter.hasNext()) {
                    managerIter = managers.iterator();
                }
                SimplePartitionManager manager = managerIter.next();
                Peer owner = manager.getServiceSpace().getDispatcher().getCluster().getLocalPeer();
                PartitionInfo partitionInfo = new PartitionInfo(version, i, owner);
                updates[i] = new PartitionInfoUpdate(false, partitionInfo);
            }
            managers.add(managers.remove(0));
            
            final PartitionBalancingInfoUpdate update = new PartitionBalancingInfoUpdate(updates, false, false);
            for (final SimplePartitionManager manager : managers) {
                new Thread() {
                    public void run() {
                        try {
                            manager.onPartitionBalancingInfoUpdate(null, update);
                        } catch (Exception e) {
                            synchronized (failureLatch) {
                                failureException = e;
                            }
                            failureLatch.countDown();
                        }
                    }
                }.start();
            }
        }
        
        protected void bootManagers() {
            Iterator<SimplePartitionManager> managerIter = managers.iterator();
            SimplePartitionManager manager = managerIter.next();
            Peer owner = manager.getServiceSpace().getDispatcher().getCluster().getLocalPeer();
            PartitionInfoUpdate[] updates = new PartitionInfoUpdate[nbPartitions];
            for (int i = 0; i < updates.length; i++) {
                PartitionInfo partitionInfo = new PartitionInfo(version, i, owner);
                updates[i] = new PartitionInfoUpdate(true, partitionInfo);
            }
            PartitionBalancingInfoUpdate update = new PartitionBalancingInfoUpdate(updates, true, false);
            manager.onPartitionBalancingInfoUpdate(null, update);

            update = new PartitionBalancingInfoUpdate(updates, false, false);
            while (managerIter.hasNext()) {
                manager = (SimplePartitionManager) managerIter.next();
                manager.onPartitionBalancingInfoUpdate(null, update);
            }
        }
        
    }
    
    private class LoadThread extends Thread {
        private final SimpleStateManager manager;
        
        public LoadThread(SimpleStateManager stateManager) {
            this.manager = stateManager;
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    doRun();
                } catch (Exception e) {
                    synchronized (failureLatch) {
                        failureException = e;
                    }
                    failureLatch.countDown();
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        protected void doRun() {
            int nbInsert = 30;
            String prefix = Thread.currentThread().getName() + "||";
            for (int i = 0; i < nbInsert; i++) {
                boolean success = manager.insert(prefix + i);
                if (!success) {
                    throw new IllegalStateException();
                }
                nbOperations++;
            }
            
            for (int i = 0; i < nbInsert; i++) {
                manager.remove(prefix + i);
                nbOperations++;
            }
        }

    }
}
