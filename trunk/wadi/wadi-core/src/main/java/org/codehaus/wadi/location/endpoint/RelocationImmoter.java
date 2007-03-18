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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.motable.AbstractMotable;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.location.session.MoveSMToPM;

/**
 * We receive a RelocationRequest and pass a RelocationImmoter down the
 * Contextualiser stack. The Session is passed to us through the Immoter and
 * we pass it back to the Request-ing node...
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
class RelocationImmoter implements Immoter {
    private static final Log log = LogFactory.getLog(MovePMToSMEndPoint.class);

    private final Dispatcher dispatcher;
    private final long inactiveTime;
    protected final Envelope message;
    protected final MovePMToSM pmToSm;
    protected boolean found = false;

    public RelocationImmoter(Dispatcher dispatcher,
            Envelope message,
            MovePMToSM request,
            long inactiveTime) {
        this.dispatcher = dispatcher;
        this.message = message;
        this.pmToSm = request;
        this.inactiveTime = inactiveTime;
    }

    public Motable newMotable(Motable emotable) {
        return new PMToIMEmotable(message);
    }

    public boolean immote(Motable emotable, Motable immotable) {
        found = true;
        return true;
    }
    
    public boolean contextualise(Invocation invocation, String id, Motable immotable)
            throws InvocationException {
        return false;
    }

    public boolean isFound() {
        return found;
    }

    class PMToIMEmotable extends AbstractMotable {
        private final Envelope message;

        public PMToIMEmotable(Envelope message) {
            this.message = message;
        }

        public byte[] getBodyAsByteArray() throws Exception {
            throw new UnsupportedOperationException();
        }

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            Motable immotable = new SimpleMotable();
            immotable.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
            immotable.setBodyAsByteArray(bytes);

            LocalPeer smPeer = dispatcher.getCluster().getLocalPeer();
            Peer imPeer = pmToSm.getIMPeer();
            MoveSMToIM request = new MoveSMToIM(immotable);
            // send on state from StateMaster to InvocationMaster...
            if (log.isTraceEnabled()) {
                log.trace("exchanging MoveSMToIM between [" + smPeer + "]->[" + imPeer + "]");
            }
            Envelope message2 = dispatcher.exchangeSend(imPeer.getAddress(), request, inactiveTime, pmToSm.getIMCorrelationId());
            // should receive response from IM confirming safe receipt...
            if (message2 == null) {
                // TODO throw exception
                log.error("NO REPLY RECEIVED FOR MESSAGE IN TIMEFRAME - PANIC!");
            } else {
                MoveIMToSM response = (MoveIMToSM) message2.getPayload();
                assert (response != null && response.getSuccess()); 
                dispatcher.reply(message, new MoveSMToPM(true));
            }
        }

    }
}