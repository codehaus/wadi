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
package org.codehaus.wadi.location.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.impl.SimplePartitionMapper;
import org.codehaus.wadi.partition.PartitionBalancingInfoUpdate;
import org.codehaus.wadi.partition.PartitionInfo;
import org.codehaus.wadi.partition.PartitionInfoUpdate;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * @version $Revision:1815 $
 */
public class SimplePartitionManagerSmokeTest extends TestCase {
    private static final long EXCHANGE_TIMEOUT = 6000;

    private Exception failureException;
    private Latch failureLatch;
    private int nbPartitions;
    private volatile int nbOperations;
    private volatile int nbRebalancing;

    protected void setUp() throws Exception {
        nbPartitions = 12;
        failureLatch = new Latch();
    }

    public void testRebalancingUnderLoad() throws Exception {
        VMBroker broker = new VMBroker("broker");
        
        List managers = new ArrayList();
        Collection loadThreads = new ArrayList();
        for (int i = 0; i < 6; i++) {
            SimplePartitionManager manager = newManager(broker, i);
            managers.add(manager);
            SimpleStateManager stateManager = new SimpleStateManager(manager.getDispatcher(), manager, EXCHANGE_TIMEOUT);
            stateManager.start();
            loadThreads.add(new LoadThread(stateManager));
            loadThreads.add(new LoadThread(stateManager));
        }
        
        RebalanceThread rebalanceThread = new RebalanceThread(managers);
        rebalanceThread.start();
        
        for (Iterator iter = loadThreads.iterator(); iter.hasNext();) {
            Thread loadThread = (Thread) iter.next();
            loadThread.start();
        }
        
        boolean success = failureLatch.attempt(20000);
        rebalanceThread.interrupt();
        for (Iterator iter = loadThreads.iterator(); iter.hasNext();) {
            Thread loadThread = (Thread) iter.next();
            loadThread.interrupt();
        }
        if (success) {
            failureException.printStackTrace();
            fail();
        }
        System.out.println("[" + nbRebalancing + "] successful rebalancing.");
        System.out.println("[" + nbOperations + "] successful invocations.");
    }

    private SimplePartitionManager newManager(VMBroker broker, int index) throws Exception {
        final Dispatcher dispatcher = new VMDispatcher(broker, Integer.toString(index), null, 1000);
        dispatcher.start();

        SimplePartitionManager manager = new SimplePartitionManager(dispatcher, nbPartitions, null,
                new SimplePartitionMapper(nbPartitions)) {
            protected void waitForBoot(long attemptPeriod) throws InterruptedException, PartitionManagerException {
            }  
        };
        manager.start();
        return manager;
    }

    private class RebalanceThread extends Thread {
        private List managers;
        private int version = 1;
        
        public RebalanceThread(List managers) {
            this.managers = managers;
            
            bootManagers();
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(3000);
                    version++;
                    doRun();
                    nbRebalancing++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        protected void doRun() {
            Iterator managerIter = managers.iterator();
            
            PartitionInfoUpdate[] updates = new PartitionInfoUpdate[nbPartitions];
            for (int i = 0; i < updates.length; i++) {
                if (!managerIter.hasNext()) {
                    managerIter = managers.iterator();
                }
                SimplePartitionManager manager = (SimplePartitionManager) managerIter.next();
                Peer owner = manager.getCluster().getLocalPeer();
                PartitionInfo partitionInfo = new PartitionInfo(version, i, owner);
                updates[i] = new PartitionInfoUpdate(false, partitionInfo);
            }
            Object object = managers.remove(0);
            managers.add(object);
            
            final PartitionBalancingInfoUpdate update = new PartitionBalancingInfoUpdate(updates, false, false);
            for (Iterator iter = managers.iterator(); iter.hasNext();) {
                final SimplePartitionManager manager = (SimplePartitionManager) iter.next();
                new Thread() {
                    public void run() {
                        try {
                            manager.onPartitionBalancingInfoUpdate(null, update);
                        } catch (Exception e) {
                            synchronized (failureLatch) {
                                failureException = e;
                            }
                            failureLatch.release();
                        }
                    }
                }.start();
            }
        }
        
        protected void bootManagers() {
            Iterator managerIter = managers.iterator();
            SimplePartitionManager manager = (SimplePartitionManager) managerIter.next();
            Peer owner = manager.getCluster().getLocalPeer();
            PartitionInfoUpdate[] updates = new PartitionInfoUpdate[nbPartitions];
            for (int i = 0; i < updates.length; i++) {
                PartitionInfo partitionInfo = new PartitionInfo(version, i, owner);
                updates[i] = new PartitionInfoUpdate(false, partitionInfo);
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
                    failureLatch.release();
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        protected void doRun() {
            int nbInsert = 30;
            String prefix = Thread.currentThread().getName() + "||";
            for (int i = 0; i < nbInsert; i++) {
                boolean success = manager.insert(prefix + i, EXCHANGE_TIMEOUT);
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
