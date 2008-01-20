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
import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.BasicInvocation;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationContext;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.DummyRouter;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.NoOpReplicationManagerFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

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
    
    private Launcher() {
        broker = new VMBroker("brokerName");
    }

    private void start() throws Exception {
        StackContext[] stackContexts = new StackContext[10];
        for (int i = 0; i < stackContexts.length; i++) {
            stackContexts[i] = newStackContext(broker, "node" + i);
        }
        
        stackContexts[0].getServiceSpace().start();
        Manager firstManager = stackContexts[0].getManager();
        
        Session session = firstManager.createWithName(SESSION_ID);
        session.addState(LAST_MANAGER_HASHCODE, firstManager.hashCode());
        session.addState(SESSION_RELOCATION, new Long(0));
        session.addState(ATTRIBUTE, new Long(0));
        session.onEndProcessing();
        
        Thread threads[] = new Thread[stackContexts.length];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ExecuteRunnablesInThread(newRunnables(stackContexts[i], i));
            threads[i].start();
        }
        
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

    private List<Runnable> newRunnables(StackContext stackContext, int index) {
        if (0 == index) {
            List<Runnable> runnables = new ArrayList<Runnable>();
            runnables.add(new InvokationRunner(stackContext, 70000));
            return runnables;
        } else {
            return generateRunnable(stackContext, index);            
        }
    }
    
    private List<Runnable> generateRunnable(StackContext stackContext, int index) {
        List<Runnable> runnables = new ArrayList<Runnable>();
        runnables.add(new WaitRunner(index * 500));
        runnables.add(new StartRunner(stackContext));
        runnables.add(new InvokationRunner(stackContext, 500));
        runnables.add(new StopRunner(stackContext));
        runnables.add(new StartRunner(stackContext));
        runnables.add(new InvokationRunner(stackContext, 500));
        runnables.add(new StopRunner(stackContext));
        runnables.add(new StartRunner(stackContext));
        runnables.add(new InvokationRunner(stackContext, 500));
        runnables.add(new StopRunner(stackContext));
        runnables.add(new StartRunner(stackContext));
        runnables.add(new InvokationRunner(stackContext, 500));
        return runnables;
    }

    private StackContext newStackContext(VMBroker broker, String nodeName) throws MessageExchangeException, Exception {
        VMDispatcher dispatcher = new VMDispatcher(broker, nodeName, null);
        dispatcher.start();

        StackContext stackContext = new StackContext(Thread.currentThread().getContextClassLoader(),
            new ServiceSpaceName(new URI("name")),
                dispatcher,
                30 * 60,
                48,
                1000 * 60 * 60 * 24,
                new RoundRobinBackingStrategyFactory(1)) {
            @Override
            protected ReplicationManagerFactory newReplicationManagerFactory() {
                return new NoOpReplicationManagerFactory();
            }
            protected Router newRouter() {
                return new DummyRouter();
            }
        };
        stackContext.build();
        return stackContext;
    }

    public class ExecuteRunnablesInThread extends Thread {
        private final List<Runnable> runnables;

        public ExecuteRunnablesInThread(List<Runnable> runnables) {
            this.runnables = runnables;
        }

        @Override
        public void run() {
            for (Runnable runnable : runnables) {
                runnable.run();
            }
        }
        
    }
    
    public class StartRunner implements Runnable {
        private final StackContext stackContext;

        public StartRunner(StackContext stackContext) {
            this.stackContext = stackContext;
        }
        
        public void run() {
            try {
                stackContext.getServiceSpace().start();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }    
    
    public class WaitRunner implements Runnable {
        private final long duration;

        public WaitRunner(long duration) {
            this.duration = duration;
        }
        
        public void run() {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }    
    
    public class StopRunner implements Runnable {
        private final StackContext stackContext;
        
        public StopRunner(StackContext stackContext) {
            this.stackContext = stackContext;
        }
        
        public void run() {
            try {
                stackContext.getServiceSpace().stop();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }    
    
    public class InvokationRunner extends Thread {
        private final StackContext stackContext;
        private final long duration;

        public InvokationRunner(StackContext stackContext, long duration) {
            this.stackContext = stackContext;
            this.duration = duration;
        }

        public void run() {
            Manager manager = stackContext.getManager();
            long end = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < end) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                Invocation invocation = new IncrementCptInvocation(SESSION_ID, 8000, manager.hashCode());
                try {
                    manager.contextualise(invocation);
                } catch (InvocationException e) {
                    e.printStackTrace();
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
        protected void doInvoke(InvocationContext context) throws InvocationException {
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
