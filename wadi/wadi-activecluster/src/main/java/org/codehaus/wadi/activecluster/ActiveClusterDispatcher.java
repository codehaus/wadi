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
package org.codehaus.wadi.activecluster;

import java.io.Serializable;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.command.ClusterCommand;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

/**
 * A Dispatcher for ActiveCluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1788 $
 */
public class ActiveClusterDispatcher extends AbstractDispatcher implements javax.jms.MessageListener {
    
    protected final String _clusterName;
    protected final String _clusterUri;
    protected final String _localPeerName;
    protected final long _inactiveTime;
    private final ActiveClusterCluster _cluster;
    protected final LocalPeer _localPeer;

    public ActiveClusterDispatcher(String clusterName, String localPeerName, String clusterUri, EndPoint endPoint, long inactiveTime) throws Exception {
        _clusterName = clusterName;
        _localPeerName = localPeerName;
        _clusterUri = clusterUri;
        _log = LogFactory.getLog(getClass() + "#" + localPeerName);
        _inactiveTime = inactiveTime;
        _cluster = new ActiveClusterCluster(_clusterName, _localPeerName, _clusterUri, endPoint, this);
        _localPeer = _cluster.getLocalPeer();
    }

    public String toString() {
        return "Cluster [" + _cluster + "]";
    }

    public Cluster getCluster() {
        return _cluster;
    }

    public synchronized void start() throws MessageExchangeException {
        try {
            _cluster.start();
        } catch (ClusterException e) {
            throw new MessageExchangeException(e);
        }
    }

    public synchronized void stop() throws MessageExchangeException {
        try {
            _cluster.stop();
        } catch (ClusterException e) {
            throw new MessageExchangeException(e);
        }
    }

    public Envelope createEnvelope() {
        return new ActiveClusterEnvelope();
    }

    protected void doSend(Address target, Envelope envelope) throws MessageExchangeException {
        Destination targetDestination = ((ActiveClusterPeer) target).getACDestination();
        try {
            ObjectMessage objectMsg = _cluster.getACCluster().createObjectMessage();
            _cluster.getACCluster().send(targetDestination, ((ActiveClusterEnvelope) envelope).fill(objectMsg));
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        }
    }

    public String getPeerName(Address address) {
        return ((ActiveClusterPeer) address).getName();
    }

    public void onMessage(javax.jms.Message message) {
        if (_log.isTraceEnabled()) {
            _log.trace(_localPeerName + " - message arrived: " + message);
        }
        ActiveClusterCluster._cluster.set(_cluster);

        ActiveClusterEnvelope wadiMsg;
        try {
            wadiMsg = new ActiveClusterEnvelope(_cluster, (ObjectMessage) message);
        } catch (JMSException e) {
            _log.error("ActiveCluster issue: could not demarshall incoming message", e);
            return;
        }

        Serializable payload = wadiMsg.getPayload();
        if (payload instanceof ClusterCommand) {
            ((ClusterCommand) payload).execute(wadiMsg, _cluster);
        } else {
            onEnvelope(wadiMsg);
        }
    }
    
}
