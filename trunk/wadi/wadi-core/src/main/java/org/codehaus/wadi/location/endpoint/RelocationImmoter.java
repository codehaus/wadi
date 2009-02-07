/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.location.endpoint;

import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.motable.AbstractMotable;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.EnvelopeHelper;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * We receive a RelocationRequest and pass a RelocationImmoter down the
 * Contextualiser stack. The Session is passed to us through the Immoter and
 * we pass it back to the Request-ing node...
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
class RelocationImmoter implements Immoter {
    private final Dispatcher dispatcher;
    protected final MovePMToSM pmToSm;
    private final ReplicationManager replicationManager;
    private final long inactiveTime;

    protected boolean sessionFound;
    protected boolean successfulRelocation;
    protected Motable motableToRelocate;

    public RelocationImmoter(Dispatcher dispatcher,
            MovePMToSM request,
            ReplicationManager replicationManager,
            long inactiveTime) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == request) {
            throw new IllegalArgumentException("request is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.dispatcher = dispatcher;
        this.pmToSm = request;
        this.replicationManager = replicationManager;
        this.inactiveTime = inactiveTime;
    }

    public Motable newMotable(Motable emotable) {
        sessionFound = true;
        return new PMToIMEmotable();
    }

    public boolean immote(Motable emotable, Motable immotable) {
        return true;
    }
    
    public boolean contextualise(Invocation invocation, Object id, Motable immotable) throws InvocationException {
        motableToRelocate = immotable;
        return true;
    }

    public boolean isSessionFound() {
        return sessionFound;
    }

    public boolean isSuccessfulRelocation() {
        return successfulRelocation;
    }
    
    public Motable getMotableToRelocate() {
        return motableToRelocate;
    }

    class PMToIMEmotable extends AbstractMotable {

        public byte[] getBodyAsByteArray() throws Exception {
            throw new UnsupportedOperationException();
        }

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            Motable immotable = new SimpleMotable();
            Object id = getAbstractMotableMemento().getId();
            immotable.init(memento.getCreationTime(),
                memento.getLastAccessedTime(),
                memento.getMaxInactiveInterval(),
                id);
            immotable.setBodyAsByteArray(bytes);

            Peer imPeer = pmToSm.getIMPeer();
            ReplicaInfo replicaInfo = replicationManager.releaseReplicaInfo(id, imPeer);
            
            Envelope reply = dispatcher.createEnvelope();
            reply.setPayload(new MoveSMToIM(immotable, replicaInfo));
            EnvelopeHelper.setAsReply(reply);

            // send on state from StateMaster to InvocationMaster...
            Envelope ackFromIM = dispatcher.exchangeSend(imPeer.getAddress(), reply, inactiveTime, pmToSm.getIMCorrelationId());
            if (null != ackFromIM) {
                successfulRelocation = true;
            } else {
                replicationManager.insertReplicaInfo(id, replicaInfo);
            }
        }

    }

}