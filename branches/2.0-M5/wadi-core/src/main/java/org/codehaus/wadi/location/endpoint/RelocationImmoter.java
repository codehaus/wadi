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
    private final long inactiveTime;
    protected final MovePMToSM pmToSm;
    protected boolean sessionFound;
    protected boolean successfulRelocation;

    public RelocationImmoter(Dispatcher dispatcher,
            Envelope message,
            MovePMToSM request,
            long inactiveTime) {
        this.dispatcher = dispatcher;
        this.pmToSm = request;
        this.inactiveTime = inactiveTime;
    }

    public Motable newMotable(Motable emotable) {
        sessionFound = true;
        return new PMToIMEmotable();
    }

    public boolean immote(Motable emotable, Motable immotable) {
        return true;
    }
    
    public boolean contextualise(Invocation invocation, String id, Motable immotable) throws InvocationException {
        return true;
    }

    public boolean isSessionFound() {
        return sessionFound;
    }

    public boolean isSuccessfulRelocation() {
        return successfulRelocation;
    }

    class PMToIMEmotable extends AbstractMotable {

        public byte[] getBodyAsByteArray() throws Exception {
            throw new UnsupportedOperationException();
        }

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            Motable immotable = new SimpleMotable();
            immotable.init(memento.getCreationTime(),
                memento.getLastAccessedTime(),
                memento.getMaxInactiveInterval(),
                getAbstractMotableMemento().getName());
            immotable.setBodyAsByteArray(bytes);

            Envelope reply = dispatcher.createEnvelope();
            reply.setPayload(new MoveSMToIM(immotable));
            EnvelopeHelper.setAsReply(reply);
            
            Peer imPeer = pmToSm.getIMPeer();

            // send on state from StateMaster to InvocationMaster...
            Envelope ackFromIM = dispatcher.exchangeSend(imPeer.getAddress(), reply, inactiveTime, pmToSm.getIMCorrelationId());
            if (null != ackFromIM) {
                successfulRelocation = true;
            }
        }

    }

}