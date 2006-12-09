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
package org.codehaus.wadi.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.DistributableSession;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ClusteredManager extends DistributableManager {
    public static final ServiceName NAME = new ServiceName("ClusteredManager");
    
    private final StateManager stateManager;
    private final PartitionManager partitionManager;
    private final InvocationProxy proxy;

    public ClusteredManager(StateManager stateManager,
            PartitionManager partitionManager,
            WebSessionPool sessionPool, 
            AttributesFactory attributesFactory, 
            ValuePool valuePool,
            WebSessionWrapperFactory sessionWrapperFactory, 
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser, 
            ConcurrentMotableMap sessionMap, 
            Router router, 
            boolean errorIfSessionNotAcquired,
            Streamer streamer, 
            boolean accessOnLoad, 
            ReplicaterFactory replicaterFactory, 
            InvocationProxy proxy) {
        super(sessionPool, 
                attributesFactory, 
                valuePool, 
                sessionWrapperFactory, 
                sessionIdFactory, 
                contextualiser,
                sessionMap, 
                router, 
                errorIfSessionNotAcquired, 
                streamer, 
                accessOnLoad, 
                replicaterFactory);
        if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        }
        this.stateManager = stateManager;
        this.partitionManager = partitionManager;
        this.proxy = proxy;
    }

    public void start() throws Exception {
        shuttingDown.set(false);
        partitionManager.start();
        stateManager.start();
        super.start();
    }

    public void stop() throws Exception {
        shuttingDown.set(true);
        partitionManager.evacuate();
        super.stop();
        stateManager.stop();
        partitionManager.stop();
    }

    public void destroy(Invocation invocation, WebSession session) {
        // this destroySession method must not chain the one in super - otherwise the notification aspect fires twice 
        // - once around each invocation... - DOH !
        Collection names = new ArrayList((_attributeListeners.length > 0) ? (Collection) session.getAttributeNameSet()
                : ((DistributableSession) session).getListenerNames());
        for (Iterator i = names.iterator(); i.hasNext();) {
            // ALLOC ?
            session.removeAttribute((String) i.next());
        }

        // TODO - remove from Contextualiser....at end of initial request ? Think more about this
        String name = session.getName();
        notifySessionDeletion(name);
        _map.remove(name);
        try {
            session.destroy();
        } catch (Exception e) {
            _log.warn("unexpected problem destroying session", e);
        }
        _sessionPool.put(session);
        if (_log.isDebugEnabled()) {
            _log.debug("destroyed: " + name);
        }
    }

    public void notifySessionDeletion(String name) {
        super.notifySessionDeletion(name);
        stateManager.remove(name);
    }

    public void notifySessionRelocation(String name) {
        super.notifySessionRelocation(name);
        stateManager.relocate(name);
    }

    protected boolean validateSessionName(String name) {
        return stateManager.insert(name);
    }

    public InvocationProxy getInvocationProxy() {
        return proxy;
    }

}
