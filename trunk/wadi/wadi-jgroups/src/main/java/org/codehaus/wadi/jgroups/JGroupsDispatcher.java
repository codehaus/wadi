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
import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
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

    protected static final String _prefix="<"+Utils.basename(JGroupsDispatcher.class)+": ";
    protected static final String _suffix=">";
    
    protected final boolean _excludeSelf=true;

    protected final JGroupsCluster _cluster;
    protected final org.jgroups.Address _localJGAddress;
    protected MessageDispatcher _dispatcher;

    public JGroupsDispatcher(String clusterName, String localPeerName, long inactiveTime, String config) throws ChannelException {
        super(inactiveTime);
        _cluster=new JGroupsCluster(clusterName, localPeerName, config);
        _localJGAddress=((JGroupsPeer)_cluster.getLocalPeer()).getJGAddress();

        // TODO - where is this method.
        //      register(_cluster, "onMessage", StateUpdate.class);
    }
    
    // 'java.lang.Object' API
    
    public String toString() {
        return _prefix+_cluster+_suffix;
    }

    // org.codehaus.wadi.group.Dispatcher' API

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

    public Message createMessage() {
        return new JGroupsMessage();
    }

    public void send(Address target, Message message) throws MessageExchangeException {
        _cluster.send(target, message);
    }

    public String getPeerName(Address address) {
        JGroupsPeer peer=(JGroupsPeer)address;
        assert(peer!=null);
        assert(_cluster!=null);
        return peer.getName();
    }

    public Cluster getCluster() {
        return _cluster;
    }

    public synchronized void setDistributedState(Map state) throws MessageExchangeException {
        // this seems to be the only test that ActiveCluster does, so there is no point in us doing any more...
        LocalPeer localNode=_cluster.getLocalPeer();
        //if (localNode.getState()!=state) {
        localNode.setState(state);
        // }
    }

    public Address getAddress(String name) {
        throw new UnsupportedOperationException();
    }

    public void init(DispatcherConfig config) throws Exception {
        super.init(config);
        _dispatcher=new MessageDispatcher(_cluster.getChannel(), _cluster, _cluster, null);
        _cluster.init(this);
    }

    protected void hook() {
        AbstractCluster._cluster.set(_cluster);
        super.hook();
    }

    // 'org.codehaus.wadi.jgroups.JGroupsDispatcher' API

    public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
        throw new UnsupportedOperationException("NYI");
    }

}
