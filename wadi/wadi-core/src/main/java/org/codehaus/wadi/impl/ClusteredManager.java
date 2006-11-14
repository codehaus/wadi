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
import java.util.Map;

import org.codehaus.wadi.ClusteredContextualiserConfig;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.PartitionManagerConfig;
import org.codehaus.wadi.location.impl.DIndex;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.DistributableSession;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ClusteredManager extends DistributableManager implements ClusteredContextualiserConfig, PartitionManagerConfig {
    public static final ServiceName NAME = new ServiceName("ClusteredManager");
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher _dispatcher;
    private final Collapser _collapser;
    private final int _numPartitions;
    private final InvocationProxy _proxy;
    private final EndPoint _endPoint;
    private final DIndex _dindex;

    public ClusteredManager(WebSessionPool sessionPool, 
            AttributesFactory attributesFactory, 
            ValuePool valuePool,
            WebSessionWrapperFactory sessionWrapperFactory, 
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser, 
            Map sessionMap, 
            Router router, 
            boolean errorIfSessionNotAcquired,
            Streamer streamer, 
            boolean accessOnLoad, 
            ReplicaterFactory replicaterFactory, 
            EndPoint endPoint,
            InvocationProxy proxy,
            ServiceSpace serviceSpace, 
            int numPartitions, 
            Collapser collapser) {
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
        _endPoint = endPoint;
        _proxy = proxy;
        this.serviceSpace = serviceSpace;
        _dispatcher = serviceSpace.getDispatcher();
        _numPartitions = numPartitions;
        _collapser = collapser;

        PartitionMapper mapper = new SimplePartitionMapper(_numPartitions);
        _dindex = new DIndex(_numPartitions, serviceSpace, mapper);
    }

    public void init(ManagerConfig config) {
        // must be done before super.init() so that ContextualiserConfig
        // contains a Cluster
        try {
            _dindex.init(this);
        } catch (Exception e) {
            throw new RuntimeException("problem starting Cluster", e);
        }
        super.init(config);
    }

    public void start() throws Exception {
        _shuttingDown.set(false);
        _dindex.start();
        super.start();
    }

    public void aboutToStop() throws Exception {
        _dindex.getPartitionManager().evacuate();
    }

    public void stop() throws Exception {
        _shuttingDown.set(true);
        super.stop();
        _dindex.stop();
    }

    static class HelperPair {
        final Class _type;
        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
            _type = type;
            _helper = helper;
        }
    }

    public void destroy(String key) {
        WebSession session = (WebSession) _map.get(key);
        if (null == session) {
            throw new IllegalArgumentException("Provided session id is unknown.");
        }
        destroy(null, session);
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

    public String getNodeName() {
        return _dispatcher.getCluster().getLocalPeer().getName();
    }

    public long getInactiveTime() {
        return _dispatcher.getCluster().getInactiveTime();
    }

    public int getNumPartitions() {
        return _numPartitions;
    }

    public Dispatcher getDispatcher() {
        return _dispatcher;
    }

    public DIndex getDIndex() {
        return _dindex;
    }

    // TODO - this should not be a notification - it is too late to reject the ID if it is already in use...
    public void notifySessionInsertion(String name) {
        super.notifySessionInsertion(name);
    }

    public void notifySessionDeletion(String name) {
        super.notifySessionDeletion(name);
        _dindex.remove(name);
    }

    public void notifySessionRelocation(String name) {
        super.notifySessionRelocation(name);
        _dindex.relocate(name);
    }

    protected boolean validateSessionName(String name) {
        return _dindex.insert(name, getInactiveTime());
    }

    public void findRelevantSessionNames(PartitionMapper mapper, Collection[] resultSet) {
        _contextualiser.findRelevantSessionNames(mapper, resultSet);
    }

    public InvocationProxy getInvocationProxy() {
        return _proxy;
    }

    public EndPoint getEndPoint() {
        return _endPoint;
    }

    public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock,
            boolean exclusiveOnly) throws InvocationException {
        return _contextualiser.contextualise(invocation, id, immoter, motionLock, exclusiveOnly);
    }

    public Immoter getImmoter(String name, Motable immotable) {
        return _contextualiser.getDemoter(name, immotable);
    }

    public String getPeerName(Address address) {
        return _dispatcher.getPeerName(address);
    }

    public Sync getInvocationLock(String name) {
        return _collapser.getLock(name);
    }

    public Peer getCoordinator() {
        return _dindex.getCoordinator();
    }

}
