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
package org.codehaus.wadi.location.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.SeniorityElectionStrategy;
import org.codehaus.wadi.impl.AbstractChainedEmoter;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.impl.WADIRuntimeException;
import org.codehaus.wadi.location.CoordinatorConfig;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.PartitionManagerConfig;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.location.StateManagerConfig;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.servicespace.ServiceSpace;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class DIndex implements ClusterListener, CoordinatorConfig, SimplePartitionManager.Callback, StateManagerConfig {
    private final Object _coordinatorLock = new Object();
    private final ServiceSpace serviceSpace;
    private final Dispatcher _dispatcher;
    private final Cluster _cluster;
    private final Peer _localPeer;
    private final String _localPeerName;
    private final Log _log;
    private final long _inactiveTime;
    private final PartitionManager _partitionManager;
    private final StateManager _stateManager;

    private Peer _coordinatorPeer;
    private Coordinator _coordinator;
    private PartitionManagerConfig _config;

    public DIndex(int numPartitions, ServiceSpace serviceSpace, PartitionMapper mapper) {
        if (1 > numPartitions) {
            throw new IllegalArgumentException("numPartitions must be > 0");
        } else if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == mapper) {
            throw new IllegalArgumentException("mapper is required");
        }
        this.serviceSpace = serviceSpace;
        _dispatcher = serviceSpace.getDispatcher();
        _cluster = _dispatcher.getCluster();
        _inactiveTime = _cluster.getInactiveTime();
        _localPeer = _cluster.getLocalPeer();
        _localPeerName = _localPeer.getName();
        _log = LogFactory.getLog(getClass().getName() + "#" + _localPeerName);
        _partitionManager = new SimplePartitionManager(_dispatcher, numPartitions, this, mapper);
        _stateManager = new SimpleStateManager(_dispatcher, _partitionManager, _inactiveTime);
    }

    public void init(PartitionManagerConfig config) {
        _config = config;
        _partitionManager.init(config);
        _stateManager.init(this);

        _cluster.setElectionStrategy(new SeniorityElectionStrategy());
    }

    public void start() throws Exception {
        _cluster.addClusterListener(this);

        _partitionManager.start();
        _stateManager.start();
    }

    public void stop() throws Exception {
        _cluster.removeClusterListener(this);

        Thread.interrupted();

        if (_coordinator != null) {
            _coordinator.stop();
            _coordinator = null;
        }

        _partitionManager.stop();
        _stateManager.stop();
    }

    public Cluster getCluster() {
        return _cluster;
    }

    public Dispatcher getDispatcher() {
        return _dispatcher;
    }

    public PartitionManager getPartitionManager() {
        return _partitionManager;
    }

    // ClusterListener
    public void onListenerRegistration(Cluster cluster, Set existing, Peer coordinator) {
        onMembershipChanged(cluster, existing, Collections.EMPTY_SET, coordinator);
    }

    public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers, Peer coordinator) {
        if (_log.isTraceEnabled()) {
            _log.trace("membership changed - local:" + _localPeer + " joiners:" + joiners + " leavers:" + leavers
                    + " coordinator:" + coordinator);
        }

        // start-stop coordinator thread
        synchronized (_coordinatorLock) {
            if (false == coordinator.equals(_coordinatorPeer)) {
                if (null != _coordinatorPeer && _coordinatorPeer.equals(_localPeer)) {
                    onDismissal(coordinator);
                }
                _coordinatorPeer = coordinator;
                if (_coordinator == null && _coordinatorPeer.equals(_localPeer)) {
                    onElection(coordinator);
                }
            } else {
                if (null == _coordinator) {
                    onElection(coordinator);
                }
            }

            if (null != _coordinator) {
                _coordinator.queueRebalancing();
            }
        }

    }

    public void onPartitionEvacuationRequest(ClusterEvent event) {
        if (null != _coordinator) {
            _coordinator.queueRebalancing();
        }
    }

    public void onElection(Peer coordinator) {
        _log.info(_localPeer + " accepting coordinatorship");
        try {
            _coordinator = new Coordinator(this);
            _coordinator.start();
        } catch (Exception e) {
            _log.error("problem starting Coordinator");
        }
    }

    public void onDismissal(Peer coordinator) {
        _log.info(_localPeer + " resigning coordinatorship");
        try {
            _coordinator.stop();
            _coordinator = null;
        } catch (Exception e) {
            _log.error("problem stopping Coordinator");
        }
    }

    public boolean isCoordinator() {
        synchronized (_coordinatorLock) {
            return _localPeer == _coordinatorPeer;
        }
    }

    public Peer getCoordinator() {
        synchronized (_coordinatorLock) {
            return _coordinatorPeer;
        }
    }

    public int getNumPartitions() {
        return _partitionManager.getNumPartitions();
    }

    public boolean insert(String name, long timeout) {
        return _stateManager.insert(name, timeout);
    }

    public void remove(String name) {
        _stateManager.remove(name);
    }

    public void relocate(String name) {
        _stateManager.relocate(name);
    }

    class SMToIMEmoter extends AbstractChainedEmoter {
        protected final Log _log = LogFactory.getLog(getClass());

        protected final String _nodeName;
        protected final Envelope _message;
        protected Sync _invocationLock;
        protected Sync _stateLock;

        public SMToIMEmoter(String nodeName, Envelope message) {
            _nodeName = nodeName;
            _message = message;
        }

        public boolean emote(Motable emotable, Motable immotable) {
            try {
                immotable.rehydrate(emotable.getCreationTime(),
                        emotable.getLastAccessedTime(),
                        emotable.getMaxInactiveInterval(),
                        emotable.getName(),
                        emotable.getBodyAsByteArray());
                MoveIMToSM response = new MoveIMToSM(true);
                _dispatcher.reply(_message, response);
                emotable.destroy();
            } catch (Exception e) {
                _log.warn(e);
                return false;
            }
            return true;
        }
        
    }

    public Motable relocate(Invocation invocation, String sessionName, boolean shuttingDown,
            long timeout, Immoter immoter) throws Exception {
        MoveIMToPM request = new MoveIMToPM(_localPeer, sessionName, !shuttingDown, invocation.getRelocatable());
        Envelope message = _partitionManager.getPartition(sessionName).exchange(request, timeout);

        if (message == null) {
            _log.error("something went wrong - what should we do?"); // TODO
            return null;
        }

        Serializable dm = message.getPayload();
        // the possibilities...
        if (dm instanceof MoveSMToIM) {
        	// We are receiving an incoming state migration...
            MoveSMToIM req = (MoveSMToIM) dm;
            // insert motable into contextualiser stack...
            Motable emotable = req.getMotable();
            if (emotable == null) {
                _log.warn("failed relocation - 0 bytes arrived: " + sessionName);
                return null;
            } else {
                Emoter emoter = new SMToIMEmoter(_config.getPeerName(message.getReplyTo()), message);
                Motable immotable = Utils.mote(emoter, immoter, emotable, sessionName);
                return immotable;
            }
        } else if (dm instanceof MovePMToIM) {
        	// The Partition manager had no record of our session key - either the session
        	// has already been destroyed, or never existed...
            if (_log.isTraceEnabled()) {
                _log.trace("unknown session: " + sessionName);
            }
            return null;
        } else if (dm instanceof MovePMToIMInvocation) {
            // we are going to relocate our Invocation to the SM...
            Peer smPeer = ((MovePMToIMInvocation) dm).getStateMaster();
            EndPoint endPoint=smPeer.getPeerInfo().getEndPoint();
            invocation.relocate(endPoint);
            return null;
        } else {
            throw new WADIRuntimeException("unexpected response returned - what should I do? : " + dm);
        }
    }

    public String getPeerName(Address address) {
        Peer local = _localPeer;
        Peer node = address.equals(local.getAddress()) ? local : (Peer) _cluster.getRemotePeers().get(address);
        return node.getName();
    }

    public long getInactiveTime() {
        return _inactiveTime;
    }

    public StateManager getStateManager() {
        return _stateManager;
    }

    public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock,
            boolean exclusiveOnly) throws InvocationException {
        return _config.contextualise(invocation, id, immoter, motionLock, exclusiveOnly);
    }

    public Sync getInvocationLock(String name) {
        return _config.getInvocationLock(name);
    }
}

