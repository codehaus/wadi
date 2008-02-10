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
package org.codehaus.wadi.jgroups;

import java.util.Collection;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.AbstractCluster;
import org.codehaus.wadi.group.impl.AbstractDispatcher;
import org.jgroups.ChannelException;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A WADI Dispatcher mapped onto a number of JGroups listeners
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDispatcher extends AbstractDispatcher {

    protected final boolean _excludeSelf = true;
    protected final JGroupsCluster _cluster;
    protected final org.jgroups.Address _localJGAddress;
    protected final MessageDispatcher _dispatcher;

    public JGroupsDispatcher(String clusterName, String localPeerName, EndPoint endPoint, String config) throws ChannelException {
        _cluster = new JGroupsCluster(clusterName, localPeerName, config, this, endPoint);
        _localJGAddress = ((JGroupsPeer) _cluster.getLocalPeer()).getJGAddress();
        _dispatcher = new MessageDispatcher(_cluster.getChannel(), _cluster, _cluster, null);
    }

    public String toString() {
        return "JGroupsDispatcher [" + _cluster + "]";
    }

    public void start() throws MessageExchangeException {
        _dispatcher.start();
        try {
            _cluster.start();
        } catch (ClusterException e) {
            throw new MessageExchangeException(e);
        }
    }

    public void stop() throws MessageExchangeException {
        try {
            _cluster.stop();
        } catch (ClusterException e) {
            throw new MessageExchangeException(e);
        }
        _dispatcher.stop();
    }

    public Envelope createEnvelope() {
        return new JGroupsEnvelope();
    }

    protected void doSend(Address target, Envelope envelope) throws MessageExchangeException {
        _cluster.send(target, envelope);
    }

    public String getPeerName(Address address) {
        JGroupsPeer peer = (JGroupsPeer) address;
        return peer.getName();
    }

    public Cluster getCluster() {
        return _cluster;
    }

    protected void hook() {
        AbstractCluster.clusterThreadLocal.set(_cluster);
        super.hook();
    }

    public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
        throw new UnsupportedOperationException("NYI");
    }

}
