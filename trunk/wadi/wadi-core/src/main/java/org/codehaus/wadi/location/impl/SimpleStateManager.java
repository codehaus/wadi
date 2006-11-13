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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.impl.AbstractMotable;
import org.codehaus.wadi.impl.RankedRWLock;
import org.codehaus.wadi.impl.SimpleMotable;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.impl.WADIRuntimeException;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.PartitionFacadeException;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.location.StateManagerConfig;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.DeletePMToIM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.EvacuatePMToIM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.InsertPMToIM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.location.session.MoveSMToPM;
import org.codehaus.wadi.location.session.ReleaseEntryRequest;
import org.codehaus.wadi.location.session.ReleaseEntryResponse;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public class SimpleStateManager implements StateManager, StateManagerMessageListener {
	protected static final Log _lockLog = LogFactory.getLog("org.codehaus.wadi.LOCKS");
    
    private final Dispatcher _dispatcher;
    private final LocalPeer localPeer;
    private final PartitionManager partitionManager;
    private final long _inactiveTime;
    private final ServiceEndpointBuilder _endpointBuilder;
    private StateManagerConfig _config;
    private Log _log = LogFactory.getLog(getClass());
    private ImmigrationListener _listener;

    public SimpleStateManager(Dispatcher dispatcher, PartitionManager partitionManager, long inactiveTime) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        }
        _dispatcher = dispatcher;
        this.partitionManager = partitionManager;
        _inactiveTime = inactiveTime;

        localPeer = dispatcher.getCluster().getLocalPeer();
        _endpointBuilder = new ServiceEndpointBuilder();
    }

    public void init(StateManagerConfig config) {
        _config = config;
        _log = LogFactory.getLog(getClass().getName() + "#" + _dispatcher.getCluster().getLocalPeer().getName());
    }

    public void start() throws Exception {
        _endpointBuilder.addSEI(_dispatcher, StateManagerMessageListener.class, this);
        _endpointBuilder.addCallback(_dispatcher, InsertPMToIM.class);
        _endpointBuilder.addCallback(_dispatcher, DeletePMToIM.class);
        _endpointBuilder.addCallback(_dispatcher, EvacuatePMToIM.class);

        // GridState - Relocate - 5 messages - IM->PM->SM->IM->SM->PM
        _endpointBuilder.addCallback(_dispatcher, MoveSMToIM.class);
        _endpointBuilder.addCallback(_dispatcher, MoveIMToSM.class);
        _endpointBuilder.addCallback(_dispatcher, MoveSMToPM.class);
        // or possibly - IM->PM->IM (failure)
        _endpointBuilder.addCallback(_dispatcher, MovePMToIM.class);
        // or possibly - IM->PM->IM (Invocation relocation)
        _endpointBuilder.addCallback(_dispatcher, MovePMToIMInvocation.class);

        _endpointBuilder.addCallback(_dispatcher, ReleaseEntryResponse.class);
    }

    public void stop() throws Exception {
        _endpointBuilder.dispose(10, 500);
    }

    public boolean insert(String name, long timeout) {
        try {
            InsertIMToPM request = new InsertIMToPM(name, localPeer);
            PartitionFacade pf = partitionManager.getPartition(name);
            Envelope reply = pf.exchange(request, timeout);
            return ((InsertPMToIM) reply.getPayload()).getSuccess();
        } catch (MessageExchangeException e) {
            _log.error("See nested", e);
            return false;
        } catch (PartitionFacadeException e) {
            _log.error("See nested", e);
            return false;
        }
    }

    public void remove(String name) {
        try {
            DeleteIMToPM request = new DeleteIMToPM(name);
            partitionManager.getPartition(name).exchange(request, _inactiveTime);
        } catch (MessageExchangeException e) {
            _log.error("See nested", e);
        } catch (PartitionFacadeException e) {
            _log.error("See nested", e);
        }
    }

    public void relocate(String name) {
        try {
            EvacuateIMToPM request = new EvacuateIMToPM(name, localPeer);
            partitionManager.getPartition(name).exchange(request, _inactiveTime);
        } catch (MessageExchangeException e) {
            _log.info("See nested", e);
        } catch (PartitionFacadeException e) {
            _log.error("See nested", e);
        }
    }

    public void onDIndexInsertionRequest(Envelope om, InsertIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onDIndexDeletionRequest(Envelope om, DeleteIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onDIndexRelocationRequest(Envelope om, EvacuateIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(message, request);
    }

    class PMToIMEmotable extends AbstractMotable {
        protected final String _name;
        protected final String _tgtNodeName;
        protected Envelope _message1;
        protected final MovePMToSM _get;

        public PMToIMEmotable(String name, String nodeName, Envelope message1, MovePMToSM get) {
            _name = name;
            _tgtNodeName = nodeName;
            _message1 = message1;
            _get = get;
        }

        public byte[] getBodyAsByteArray() throws Exception {
            throw new UnsupportedOperationException();
        }

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            Motable immotable = new SimpleMotable();
            immotable.init(_creationTime, _lastAccessedTime, _maxInactiveInterval, _name);
            immotable.setBodyAsByteArray(bytes);

            long timeout = _config.getInactiveTime();
            LocalPeer smPeer = _dispatcher.getCluster().getLocalPeer();
            Peer imPeer = _get.getIMPeer();
            MoveSMToIM request = new MoveSMToIM(immotable);
            // send on state from StateMaster to InvocationMaster...
            if (_log.isTraceEnabled()) {
                _log.trace("exchanging MoveSMToIM between [" + smPeer + "]->[" + imPeer + "]");
            }
            Envelope message2 = _dispatcher.exchangeSend(imPeer.getAddress(), request, timeout, _get
                    .getIMCorrelationId());
            // should receive response from IM confirming safe receipt...
            if (message2 == null) {
                // TODO throw exception
                _log.error("NO REPLY RECEIVED FOR MESSAGE IN TIMEFRAME - PANIC!");
            } else {
                MoveIMToSM response = (MoveIMToSM) message2.getPayload();
                assert (response != null && response.getSuccess()); 
                _dispatcher.reply(_message1, new MoveSMToPM(true));
            }
        }

    }

    /**
     * We receive a RelocationRequest and pass a RelocationImmoter down the
     * Contextualiser stack. The Session is passed to us through the Immoter and
     * we pass it back to the Request-ing node...
     * 
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision:1815 $
     */
    class RelocationImmoter implements Immoter {
        protected final Log _log = LogFactory.getLog(getClass());
        protected final String _tgtNodeName;
        protected Envelope _message;
        protected final MovePMToSM _request;
        protected boolean _found = false;
        protected Sync _invocationLock;

        public RelocationImmoter(String nodeName, Envelope message, MovePMToSM request) {
            _tgtNodeName = nodeName;
            _message = message;
            _request = request;
        }

        public Motable nextMotable(String name, Motable emotable) {
            return new PMToIMEmotable(name, _tgtNodeName, _message, _request);
        }

        public boolean prepare(String name, Motable emotable, Motable immotable) {
            return true;
        }

        public void commit(String name, Motable immotable) {
            _found = true;
        }

        public void rollback(String name, Motable immotable) {
            // this probably has to by NYI... - nasty...
        }

        public boolean contextualise(Invocation invocation, String id, Motable immotable, Sync motionLock)
                throws InvocationException {
            return false;
        }

        public String getInfo() {
            return "emigration:" + _tgtNodeName;
        }

        public boolean getFound() {
            return _found;
        }

    }

    public void onMessage(Envelope message1, MovePMToSM request) {
        Object key = request.getKey();
        try {
            RankedRWLock.setPriority(RankedRWLock.EMIGRATION_PRIORITY);

            Sync invocationLock = _config.getInvocationLock((String) key);
            try {
                Utils.acquireUninterrupted("Invocation", (String) key, invocationLock);
            } catch (TimeoutException e) {
                _log.error("unexpected timeout - proceding without lock", e);
                throw new WADIRuntimeException(e);
            }

            try {
                // Tricky - we need to call a Moter at this point and start removal of State to other node...
                Peer imPeer = request.getIMPeer();
                RelocationImmoter promoter = new RelocationImmoter(imPeer.getName(), message1, request);

                // if we own session, this will send the correct response...
                _config.contextualise(null, (String) key, promoter, invocationLock, true);
                if (!promoter.getFound()) {
                    _log.warn("state not found - perhaps it has just been destroyed: " + key);
                    MoveSMToIM req = new MoveSMToIM(null);
                    // send on null state from StateMaster to InvocationMaster...
                    long timeout = _config.getInactiveTime();
                    _log.info("sending 0 bytes to : " + imPeer);
                    Envelope ignore = _dispatcher.exchangeSend(imPeer.getAddress(), req, timeout, request
                            .getIMCorrelationId());
                    _log.info("received: " + ignore);
                    // StateMaster replies to PartitionMaster indicating
                    // failure...
                    _log.info("reporting failure to PM");
                    _dispatcher.reply(message1, new MoveSMToPM(false));
                }
            } catch (Exception e) {
                if (_log.isWarnEnabled())
                    _log.warn("problem handling relocation request: " + key, e);
            } finally {
                RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
                invocationLock.release();
            }
        } finally {
        }
    }

    public boolean offerEmigrant(String key, Motable emotable, long timeout) {
        Partition partition = partitionManager.getPartition(key);
        ReleaseEntryRequest pojo = new ReleaseEntryRequest(emotable);
        Envelope response = null;
        try {
            response = partition.exchange(pojo, timeout);
            ReleaseEntryResponse releaseResponse = (ReleaseEntryResponse) response.getPayload();
            if (_log.isTraceEnabled()) {
                _log.trace("received acknowledgement (" + (releaseResponse.isSuccess() ? "good" : "bad")
                        + ") within timeframe (" + timeout + " millis): " + key);
            }
            return releaseResponse.isSuccess();
        } catch (Exception e) {
            _log.error("no acknowledgement within timeframe (" + timeout + " millis): " + key, e);
            return false;
        }
    }

    public void acceptImmigrant(Envelope message, String name, Motable motable) {
        try {
            _dispatcher.reply(message, new ReleaseEntryResponse(true));
        } catch (MessageExchangeException e) {
            _log.error("could not acknowledge safe receipt: " + name);
        }
    }

    public void setImmigrationListener(ImmigrationListener listener) {
        _listener = listener;
    }

    public void unsetImmigrationListener(ImmigrationListener listener) {
        if (_listener == listener) {
            _listener = null;
        }
    }

    public void onEmigrationRequest(Envelope message, ReleaseEntryRequest request) {
        if (null == _listener) {
            return;
        }
        _listener.onImmigration(message, request.getMotable());
    }

}
