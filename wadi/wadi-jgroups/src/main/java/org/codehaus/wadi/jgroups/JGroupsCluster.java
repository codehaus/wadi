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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractCluster;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.jgroups.messages.StateRequest;
import org.codehaus.wadi.jgroups.messages.StateResponse;
import org.codehaus.wadi.jgroups.messages.StateUpdate;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.MessageListener;
import org.jgroups.View;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Slot;

//TODO - regular state updates

/**
 * A WADI Cluster mapped onto a JGroups Channel
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

// TODO
// pass testsuite
// more work on Cluster & Dispatcher
// lose getRemotePeers()

public class JGroupsCluster extends AbstractCluster implements MembershipListener, MessageListener, JGroupsClusterMessageListener {

    protected static final String _prefix="<"+Utils.basename(JGroupsCluster.class)+": ";
    protected static final String _suffix=">";

    public static final String TEST_CLUSTER_NAME="org.codehaus.wadi.TEST-"+Math.random();
    public static final String TEST_CLUSTER_CONFIG="default.xml";
    //public static final String TEST_CLUSTER_CONFIG="default-minimalthreads.xml";
    
    protected final boolean _excludeSelf=true;
    // should probably be initialised in start() and dumped in stop()
    protected final Latch _viewLatch=new Latch();
    protected final ViewThread _viewThread=new ViewThread("WADI/JGroups View Thread");
    protected final Channel _channel;
    // initialised in init()
    protected JGroupsDispatcher _dispatcher;
    protected org.jgroups.Address _localJGAddress;
    
    public JGroupsCluster(String clusterName, String localPeerName, String config) throws ChannelException {
        super(clusterName, localPeerName);
        _clusterPeer=new JGroupsClusterPeer(this);
        _localPeer=new JGroupsLocalPeer(this);
        ((JGroupsLocalPeer)_localPeer).setAttribute(Peer._peerNameKey, _localPeerName);
        _channel=new JChannel(config);
        _cluster.set(this); // set ThreadLocal
    }

    // 'Object' API

    public String toString() {
        return _prefix+_localPeerName+"/"+_clusterName+_suffix;
    }

    // 'Cluster' API

    public LocalPeer getLocalPeer() {
        return _localPeer;
    }

    public void start() throws ClusterException {
        try {
            //_channel.setOpt(Channel.LOCAL, Boolean.FALSE); // exclude ourselves from our own broadcasts... - BUT also from Unicasts :-(
            _channel.connect(_clusterName);
            long joinTime=System.currentTimeMillis();
            _log.info(_localPeerName+" - "+"connected to channel");
            _localJGAddress=_channel.getLocalAddress();
            ((JGroupsLocalPeer)_localPeer).init(_localJGAddress);
            Map localState=_localPeer.getState();
            localState.put(Peer._peerNameKey, _localPeerName);
            localState.put(Peer._birthTimeKey, new Long(joinTime));
            _backendToPeer.put(_localJGAddress, _localPeer);
        } catch (Exception e) {
            _log.warn("unexpected JGroups problem", e);
            throw new ClusterException(e);
        }

        // start accepting new views...
        _viewThread.start();
        // wait for the first view to be accepted before continuing...
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"acquiring viewLatch...");
        try {
            _viewLatch.acquire();
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
        }
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"...acquired viewLatch");
    }

    public void stop() throws ClusterException {
        _viewThread.stop();
        _channel.disconnect();
        _channel.close();
        _log.info(_localPeerName+" - "+"disconnected from channel");
        // TODO
        // hmmm... sometimes we get messages hitting the Dispatcher even after we have closed the channel - we need to wait for them somehow...
        //_clusterTopic=null;
        //_localDestination=null;
        //_localAddress=null;
    }

    // JGroups MembershipListener API

    protected final EDU.oswego.cs.dl.util.concurrent.Channel _viewQueue=new Slot();

    public synchronized void viewAccepted(View newView) {
        // this is meant to happen if a network split is healed and two
        // clusters try to reconcile their separate states into one -
        // I have a plan...
        if(newView instanceof MergeView)
            if (_log.isWarnEnabled()) _log.warn("NYI - merging: view is " + newView);

        Set members=new TreeSet(newView.getMembers());
        try {
            if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"handling JGroups viewAccepted("+newView+")...");
            _viewQueue.put(members);
        } catch (InterruptedException e) {
            if (_log.isWarnEnabled()) _log.warn("unexpected interruption", e);
        }
    }

    public class ViewThread implements Runnable {

        protected boolean _running;
        protected Thread _thread;

        ViewThread(String name) {
            _thread=new Thread(this, name);
        }

        public void start() {
            _running=true;
            _thread.start();
        }

        public void stop() {
            _running=false;
        }

        public void run() {
            while (_running) {
                try {
                    Set members=(Set)_viewQueue.poll(2000);
                    if (members!=null)
                        nextView(members);
                } catch (InterruptedException e) {
                    _log.warn("unexpected interruption", e);
                }
            }
        }
    }

    public void nextView(Set newMembers) {
        Set joiners=new TreeSet();
        Set leavers=new TreeSet();

        newMembers.remove(_localJGAddress);
        // this is being locked for quite a while - can we improve ? - TODO
        synchronized (_addressToPeer) {
            // leavers :
            for (Iterator i=_addressToPeer.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry=(Map.Entry)i.next();
                JGroupsPeer address=(JGroupsPeer)entry.getKey();
                Peer peer=(Peer)entry.getValue();
                _log.trace("checking (leaver?): "+peer);
                if (!newMembers.contains(address.getJGAddress())) {
                    leavers.add(peer);
                    i.remove(); // remove from _AddressToPeer
                    // remove peer
                    synchronized (_backendToPeer) {_backendToPeer.remove(address.getJGAddress());}
                }
            }
            // joiners :
            for (Iterator i=newMembers.iterator(); i.hasNext(); ) {
                org.jgroups.Address jgaddress=(org.jgroups.Address)i.next();
                JGroupsPeer peer=(JGroupsPeer)getPeer(jgaddress);
                _log.trace("checking (joiner?): "+peer);
                if (peer.getState().isEmpty()) {
                    // fetch state - a quick round trip to fetch the distributed data for this node - yeugh !
                    try {
                        long timeout=100000; // TODO - parameterise...
                        JGroupsMessage tmp=(JGroupsMessage)_dispatcher.exchangeSend(peer, new StateRequest(_localPeer.getState()), timeout);
                        Map state=((StateResponse)tmp.getPayload()).getState();
                        peer.setState(state);

                    } catch (Exception e) {
                        _log.warn("problem retrieving remote state for fresh joiner...", e);
                        return;
                    }
                }

                if (!_addressToPeer.containsKey(peer)) {
                    _addressToPeer.put(peer, peer); // synched above...
                    joiners.add(peer);
                }
            }
        }

        // ensure that all joiners are entered into our model...

        joiners=Collections.unmodifiableSet(joiners);
        leavers=Collections.unmodifiableSet(leavers);

        // elect coordinator - should be run before onMembershipChanged called, since the coordinator might have to take specific action...
        Peer coordinator=findCoordinator();

        // notify listeners of changed membership
        notifyMembershipChanged(joiners, leavers, coordinator);

        // release latch so that start() can complete
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"releasing viewLatch (viewAccepted)...");
        _viewLatch.release();
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"...released viewLatch (viewAccepted)");
    }

    // JGroups 'MembershipListener' API

    public void suspect(org.jgroups.Address suspected_mbr) {
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"handling suspect("+suspected_mbr+")...");
        if (_log.isWarnEnabled()) _log.warn("cluster suspects member may have been lost: " + suspected_mbr);
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"...suspect() handled");
    }

    // Block sending and receiving of messages until viewAccepted() is called
    public void block() {
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"handling block()...");
        // NYI
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"... block() handled");

    }

    // JGroups 'MessageListener' API

    public void receive(org.jgroups.Message msg) {
        // TODO: this may be a little contentious - think of a better way...
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+_localPeer+" - RECEIVED SOMETHING: "+msg);
        synchronized (_channel) {
            org.jgroups.Address src=msg.getSrc();
            org.jgroups.Address dest=msg.getDest();
            if (_excludeSelf && (dest==null || dest.isMulticastAddress()) && src==_localJGAddress) {
                if (_log.isDebugEnabled()) _log.debug(_localPeerName+" - "+"ignoring message from self: "+msg);
            } else {
                _cluster.set(this); // setup a ThreadLocal to be read during deserialisation...
                Object o=msg.getObject();

                JGroupsMessage message=(JGroupsMessage)o;
                message.setCluster(this);
                message.setReplyTo((JGroupsPeer)getPeer(src));
                message.setAddress((JGroupsPeer)getPeer(dest));
                _dispatcher.onMessage(message);
            }
        }
    }

    public byte[] getState() {
        throw new UnsupportedOperationException("we do not use JGroups' state exchange protocol");
    }

    public void setState(byte[] state) {
        throw new UnsupportedOperationException("we do not use JGroups' state exchange protocol");
    }

    // we should be able to pull this up into AbstractCluster...
        public Address getAddress() {
        return (JGroupsPeer)_clusterPeer;
    }

    // WADI 'JGroupsClusterMessageListener' API

    public void onMessage(Message message, StateUpdate update) throws Exception {
        // incorporate incoming state into our model
        JGroupsRemotePeer peer=(JGroupsRemotePeer)message.getReplyTo();
        peer.setState(update.getState());
        notifyPeerUpdated(peer);
    }

    public void onMessage(Message message, StateRequest request) throws Exception {
        // incorporate incoming state into our model
        JGroupsRemotePeer peer=(JGroupsRemotePeer)message.getReplyTo();
        peer.setState(request.getState());
        // send our own state back in response
        _dispatcher.reply(message, new StateResponse(_localPeer.getState()));
    }

    // 'JGroupsCluster' API

    public void init(JGroupsDispatcher dispatcher) throws Exception {
        _dispatcher=dispatcher;
        ServiceEndpointBuilder endpointBuilder = new ServiceEndpointBuilder();
        endpointBuilder.addSEI(_dispatcher, JGroupsClusterMessageListener.class, this);
        endpointBuilder.addCallback(_dispatcher, StateResponse.class);
    }

    public void send(Address target, Message message) throws MessageExchangeException {
        JGroupsMessage msg=(JGroupsMessage)message;
        JGroupsPeer tgt=(JGroupsPeer)target;
        try {
            msg.setCluster(this);
            msg.setAddress(target);
            if (_messageLog.isTraceEnabled()) _messageLog.trace("outgoing: "+msg.getPayload()+" {"+_localPeer.getName()+"->"+tgt.getName()+"} - "+msg.getTargetCorrelationId()+"/"+msg.getSourceCorrelationId());
            msg.setCluster(null);
            _channel.send(tgt.getJGAddress(), _localJGAddress, msg);
        } catch (Exception e) {
            _log.warn("unexpected JGroups problem", e);
            throw new MessageExchangeException(e);
        }
    }

    public Channel getChannel() {
        return _channel;
    }


    
    protected Peer create(Object backend) {
        org.jgroups.Address jgAddress=(org.jgroups.Address)backend;
        JGroupsPeer peer;
        if (jgAddress.isMulticastAddress()) {
            peer=(JGroupsPeer)_clusterPeer;
            // I can't find a way to initialise the clusterAddress from the client side
            // so I wait to receive a message, sent to the cluster, then use the address from that - clumsy
            if (peer.getJGAddress()==null)
                peer.init(jgAddress);
        } else {
            peer=new JGroupsRemotePeer(this, jgAddress);
        }
        return peer;
    }
    
}
