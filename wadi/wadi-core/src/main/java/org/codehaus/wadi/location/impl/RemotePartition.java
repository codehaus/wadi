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
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class RemotePartition extends AbstractPartition {
    private final Log _log;
    private final Dispatcher dispatcher;
    private final Peer _peer;

    public RemotePartition(int key, Dispatcher dispatcher, Peer peer) {
        super(key);
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        }
        this.dispatcher = dispatcher;
        _peer = peer;
        
        _log = LogFactory.getLog(getClass().getName() + "#" + _key + "@" + dispatcher.getCluster().getLocalPeer());
    }

    public String toString() {
        return "RemotePartition [" + _key + "@" + dispatcher.getCluster().getLocalPeer() + "->" + _peer + "]";
    }

    public boolean isLocal() {
        return false;
    }

    protected void forward(Envelope message) {
        if (_log.isTraceEnabled()) {
            _log.trace("indirecting: " + message + " via " + _peer);
        }
        try {
            dispatcher.forward(message, _peer.getAddress());
        } catch (MessageExchangeException e) {
            _log.warn("could not forward message", e);
        }
    }

    public void onMessage(Envelope message, InsertIMToPM request) {
        forward(message);
    }

    public void onMessage(Envelope message, DeleteIMToPM request) {
        forward(message);
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        forward(message);
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        forward(message);
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws Exception {
        if (_log.isTraceEnabled()) {
            _log.trace("exchanging message (" + request + ") with node: " + _peer);
        }
        return dispatcher.exchangeSend(_peer.getAddress(), request, timeout);
    }

}
