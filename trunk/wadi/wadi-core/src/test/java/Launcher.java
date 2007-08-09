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
import java.net.URI;

import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.BasicInvocation;
import org.codehaus.wadi.core.contextualiser.BasicInvocationContextFactory;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationContext;
import org.codehaus.wadi.core.contextualiser.InvocationContextFactory;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.DummyRouter;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * 
 * @version $Revision: 1538 $
 */
public class Launcher {
    private static final String SESSION_ID = "myID";
    private static final String ATTRIBUTE = "attribute";
    private static final String LAST_MANAGER_HASHCODE = "lastManagerHashCode";
    private static final String SESSION_RELOCATION = "sessionRelocation";

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.start();
    }

    private final VMBroker broker;
    private final Latch latch;
    
    private Launcher() {
        broker = new VMBroker("brokerName");
        latch = new Latch();
    }

    private void start() throws Exception {
        Manager[] managers = new Manager[26];
        for (int i = 0; i < managers.length; i++) {
            managers[i] = newManager(broker, "node" + i);
        }
        Manager firstManager = managers[0];
        
        Session session = firstManager.createWithName(SESSION_ID);
        session.addState(LAST_MANAGER_HASHCODE, firstManager.hashCode());
        session.addState(SESSION_RELOCATION, new Long(0));
        session.addState(ATTRIBUTE, new Long(0));
        session.onEndProcessing();
        
        Thread threads[] = new Thread[managers.length];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ThreadRunner(managers[i]);
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        latch.release();
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long end = System.currentTimeMillis();

        Invocation invocation = new IncrementCptInvocation(SESSION_ID, 1000, firstManager.hashCode());
        try {
            firstManager.contextualise(invocation);
        } catch (InvocationException e) {
            e.printStackTrace();
        }
        session = invocation.getSession();
        
        System.out.println("Done in " + (end - start) + "ms");
        Long cpt = (Long) session.getState(ATTRIBUTE);
        System.out.println("Number of invocations " + cpt.longValue());
        
        Long nbReloc = (Long) session.getState(SESSION_RELOCATION);
        System.out.println("Number of relocations " + nbReloc.longValue());

        System.exit(0);
    }

    private Manager newManager(VMBroker broker, String nodeName) throws MessageExchangeException,
            Exception {
        VMDispatcher dispatcher = new VMDispatcher(broker, nodeName, null);
        dispatcher.start();

        StackContext stackContext = new StackContext(Thread.currentThread().getContextClassLoader(),
            new ServiceSpaceName(new URI("name")),
                dispatcher,
                30 * 60,
                48,
                1000 * 60 * 60 * 24,
                new RoundRobinBackingStrategyFactory(1)) {
            protected Router newRouter() {
                return new DummyRouter();
            }
            @Override
            protected InvocationContextFactory newInvocationContextFactory() {
                return new BasicInvocationContextFactory();
            }
        };
        stackContext.build();
        stackContext.getServiceSpace().start();
        return stackContext.getManager();
    }

    public class ThreadRunner extends Thread {
        private final Manager manager;

        public ThreadRunner(Manager manager) {
            this.manager = manager;
        }

        public void run() {
            try {
                latch.acquire();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Cannot acquire latch");
            }

            for (int i = 0; i < 20000; i++) {
                Invocation invocation = new IncrementCptInvocation(SESSION_ID, 1000, manager.hashCode());
                try {
                    manager.contextualise(invocation);
                } catch (InvocationException e) {
                    e.printStackTrace();
                    System.out.println("loop [" + i + "]");
                    break;
                }
            }
        }
    }
    
    private static class IncrementCptInvocation extends BasicInvocation {
        private final int managerHashCode;

        public IncrementCptInvocation(String sessionKey, long exclusiveSessionLockWaitTime, int managerHashCode) {
            super(sessionKey, exclusiveSessionLockWaitTime);
            this.managerHashCode = managerHashCode;
        }
        
        @Override
        public void invoke(InvocationContext context) throws InvocationException {
            Session session = getSession();
            Long cpt = (Long) session.getState(ATTRIBUTE);
            if (null == cpt) {
                throw new InvocationException("cpt is null");
            }
            session.addState(ATTRIBUTE, new Long(cpt.longValue() + 1));
            
            Integer lastManagerHashCode = (Integer) session.getState(LAST_MANAGER_HASHCODE);
            if (lastManagerHashCode.intValue() != managerHashCode) {
                session.addState(LAST_MANAGER_HASHCODE, new Integer(managerHashCode));
                
                Long nbReloc = (Long) session.getState(SESSION_RELOCATION);
                session.addState(SESSION_RELOCATION, new Long(nbReloc.longValue() + 1));
            }
        }  

    }
}
