/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.core.manager;

import org.codehaus.wadi.Router;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.contextualiser.InvocationProxy;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.web.WebSession;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ClusteredManager extends DistributableManager {
    private final StateManager stateManager;
    private final PartitionManager partitionManager;
    private final InvocationProxy proxy;

    public ClusteredManager(StateManager stateManager,
            PartitionManager partitionManager,
            SessionFactory sessionFactory, 
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser, 
            ConcurrentMotableMap sessionMap, 
            Router router, 
            SessionMonitor sessionMonitor,
            boolean errorIfSessionNotAcquired,
            InvocationProxy proxy) {
        super(sessionFactory,
                sessionIdFactory, 
                contextualiser,
                sessionMap, 
                router, 
                sessionMonitor,
                errorIfSessionNotAcquired);
        if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        } else if (null == proxy) {
            throw new IllegalArgumentException("proxy is required");
        }
        this.stateManager = stateManager;
        this.partitionManager = partitionManager;
        this.proxy = proxy;
    }

    public void start() throws Exception {
        partitionManager.start();
        stateManager.start();
        super.start();
    }

    public void stop() throws Exception {
        partitionManager.evacuate();
        super.stop();
        stateManager.stop();
        partitionManager.stop();
    }

    protected void onSessionDestruction(WebSession session) {
        super.onSessionDestruction(session);
        stateManager.remove(session.getName());
    }

    protected boolean validateSessionName(String name) {
        return stateManager.insert(name);
    }
    
    public boolean contextualise(Invocation invocation) throws InvocationException {
        invocation.setInvocationProxy(proxy);
        return super.contextualise(invocation);
    }

}