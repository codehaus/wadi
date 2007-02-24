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
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.web.impl.WebInvocation;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * 
 * @version $Revision: 1538 $
 */
public class Launcher {
    private static final String SESSION_ID = "myID";

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.start();
    }

    private final VMBroker broker;
    private final FilterChain filterChain;
    private final Latch latch;
    private ClusteredManager redManager;
    private ClusteredManager greenManager;
    private ClusteredManager yellowManager;
    private volatile Long cpt;
    
    private Launcher() {
        broker = new VMBroker("brokerName");
        
        filterChain = new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                HttpSession session = httpServletRequest.getSession(false);
                Long currCpt = (Long) session.getAttribute("attr1");
                if (null == currCpt) {
                    throw new IllegalStateException();
                } else if (cpt.longValue() != currCpt.longValue()) {
                    throw new IllegalStateException("cpt [" + cpt + "]; currCpt [" + currCpt + "]");
                }
                cpt = new Long(currCpt.longValue() + 1);
                session.setAttribute("attr1", cpt);
            }
        };
        
        latch = new Latch();
    }

    private void start() throws Exception {
        redManager = newClusteredManager(broker, "red");
        greenManager = newClusteredManager(broker, "green");
        yellowManager = newClusteredManager(broker, "yellow");
        
        Session session = greenManager.createWithName(SESSION_ID);
        cpt = new Long(0);
        session.addState("attr1", cpt);
        session.onEndProcessing();
        
        ClusteredManager managers[] = new ClusteredManager[] {redManager, greenManager, yellowManager};
        Thread threads[] = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ThreadRunner(managers[i % 3], filterChain);
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

        System.out.println("Done in [" + (end - start) + "]");
        System.out.println(cpt.longValue());
        System.exit(0);
    }

    private ClusteredManager newClusteredManager(VMBroker broker, String nodeName) throws MessageExchangeException,
            Exception {
        VMDispatcher dispatcher = new VMDispatcher(broker, nodeName, null, 1000);
        dispatcher.start();

        SampleStack stack = new SampleStack();
        stack.create(dispatcher);
        stack.start();
        return stack.getManager();
    }

    private static MockHttpServletResponse newResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        return response;
    }

    private static MockHttpServletRequest newRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            public String getRequestedSessionId() {
                return SESSION_ID;
            }
        };
        request.setMethod("POST");
        return request;
    }

    public class ThreadRunner extends Thread {
        private final ClusteredManager manager;

        private final FilterChain filterChain;

        public ThreadRunner(ClusteredManager manager, FilterChain filterChain) {
            this.manager = manager;
            this.filterChain = filterChain;
        }

        public void run() {
            
            try {
                latch.acquire();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Cannot acquire latch");
            }

            for (int i = 0; i < 1000; i++) {
                MockHttpServletRequest request = newRequest();
                MockHttpServletResponse response = newResponse();
                WebInvocation invocation = new WebInvocation();
                invocation.init(request, response, filterChain);
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
}
