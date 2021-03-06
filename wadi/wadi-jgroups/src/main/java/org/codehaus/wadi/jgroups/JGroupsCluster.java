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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.command.BootRemotePeer;
import org.codehaus.wadi.group.command.ClusterCommand;
import org.codehaus.wadi.group.impl.AbstractCluster;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.MessageListener;
import org.jgroups.View;


/**
 * A WADI Cluster mapped onto a JGroups Channel
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster extends AbstractCluster implements MembershipListener, MessageListener {
    protected final boolean _excludeSelf = true;
    // should probably be initialised in start() and dumped in stop()
    protected final CountDownLatch _viewLatch = new CountDownLatch(1);
    protected final ViewThread _viewThread = new ViewThread("WADI/JGroups View Thread");
    protected final Channel _channel;
    protected final JGroupsDispatcher _dispatcher;

    // initialised in init()
    protected org.jgroups.Address _localJGAddress;

    public JGroupsCluster(String clusterName, String localPeerName, String config, JGroupsDispatcher dispatcher, EndPoint endPoint) throws ChannelException {
        super(clusterName, localPeerName, dispatcher);
        _dispatcher = dispatcher;
        clusterPeer = new JGroupsClusterPeer(this, clusterName);
        localPeer = new JGroupsLocalPeer(this, localPeerName, endPoint);
        _channel = new JChannel(config);
        clusterThreadLocal.set(this);
    }

    public String toString() {
        return "JGroupsCluster [" + localPeerName + "/" + clusterName + "]";
    }

    public LocalPeer getLocalPeer() {
        return localPeer;
    }

    public Peer getPeerFromAddress(Address address) {
        return (JGroupsPeer) address;
    }

    public synchronized void start() throws ClusterException {
        try {
            // _channel.setOpt(Channel.LOCAL, Boolean.FALSE); // exclude
            // ourselves from our own broadcasts... - BUT also from Unicasts :-(
            _channel.connect(clusterName);
            log.info(localPeerName + " - " + "connected to channel");
            _localJGAddress = _channel.getLocalAddress();
            ((JGroupsLocalPeer) localPeer).init(_localJGAddress);
            backendKeyToPeer.put(_localJGAddress, localPeer);
        } catch (Exception e) {
            log.warn("unexpected JGroups problem", e);
            throw new ClusterException(e);
        }

        // start accepting new views...
        _viewThread.start();
        // wait for the first view to be accepted before continuing...
        if (log.isTraceEnabled())
            log.trace(localPeerName + " - " + "acquiring viewLatch...");
        try {
            _viewLatch.await();
        } catch (InterruptedException e) {
            log.warn("unexpected interruption", e);
        }
        if (log.isTraceEnabled())
            log.trace(localPeerName + " - " + "...acquired viewLatch");
    }

    public synchronized void stop() throws ClusterException {
        _viewThread.stop();
        _channel.disconnect();
        _channel.close();
        log.info(localPeerName + " - " + "disconnected from channel");
        // TODO
        // hmmm... sometimes we get messages hitting the Dispatcher even after
        // we have closed the channel - we need to wait for them somehow...
        // _clusterTopic=null;
        // _localDestination=null;
        // _localAddress=null;
    }

    // JGroups MembershipListener API

    protected final SynchronousQueue<Set> _viewQueue = new SynchronousQueue<Set>();

    public void viewAccepted(View newView) {
        // this is meant to happen if a network split is healed and two
        // clusters try to reconcile their separate states into one -
        // I have a plan...
        if (newView instanceof MergeView) {
            log.warn("NYI - merging: view is " + newView);
        }

        Set members = new TreeSet(newView.getMembers());
        try {
            if (log.isTraceEnabled()) {
                log.trace(localPeerName + " - " + "handling JGroups viewAccepted(" + newView + ")...");
            }
            _viewQueue.put(members);
        } catch (InterruptedException e) {
            log.warn("unexpected interruption", e);
        }
    }

    public class ViewThread implements Runnable {
        protected boolean _running;
        protected Thread _thread;

        ViewThread(String name) {
            _thread = new Thread(this, name);
        }

        public void start() {
            _running = true;
            _thread.start();
        }

        public void stop() {
            _running = false;
        }

        public void run() {
            while (_running) {
                try {
                    Set members = (Set) _viewQueue.poll(2000, TimeUnit.MILLISECONDS);
                    if (members != null) {
                        nextView(members);
                    }
                } catch (InterruptedException e) {
                    log.warn("unexpected interruption", e);
                }
            }
        }
    }

    public void nextView(Set newMembers) {
        Set joiners = new TreeSet();
        Set leavers = new TreeSet();

        newMembers.remove(_localJGAddress);
        synchronized (addressToPeer) {
            // leavers :
            for (Iterator i = addressToPeer.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                JGroupsPeer address = (JGroupsPeer) entry.getKey();
                Peer peer = (Peer) entry.getValue();
                log.trace("checking (leaver?): " + peer);
                if (!newMembers.contains(address.getJGAddress())) {
                    leavers.add(peer);
                    i.remove(); // remove from _AddressToPeer
                    // remove peer
                    synchronized (backendKeyToPeer) {
                        backendKeyToPeer.remove(address.getJGAddress());
                    }
                }
            }
            // joiners :
            for (Iterator i = newMembers.iterator(); i.hasNext();) {
                org.jgroups.Address jgaddress = (org.jgroups.Address) i.next();

                JGroupsPeer remotePeer = new JGroupsPeer(this, "UNDEFINED", null);
                remotePeer.init(jgaddress);
                BootRemotePeer command = new BootRemotePeer(this, remotePeer);
                remotePeer = (JGroupsPeer) command.getSerializedPeer();
                if (null == remotePeer) {
                    return;
                }
                
                JGroupsPeer peer = (JGroupsPeer) getPeer(remotePeer);
                log.trace("checking (joiner?): " + peer);
                if (!addressToPeer.containsKey(peer)) {
                    addressToPeer.put(peer, peer);
                    joiners.add(peer);
                }
            }
        }

        // ensure that all joiners are entered into our model...
        joiners = Collections.unmodifiableSet(joiners);
        leavers = Collections.unmodifiableSet(leavers);

        // notify listeners of changed membership
        notifyMembershipChanged(joiners, leavers);

        // release latch so that start() can complete
        if (log.isTraceEnabled()) {
            log.trace(localPeerName + " - " + "releasing viewLatch (viewAccepted)...");
        }
        _viewLatch.countDown();
        if (log.isTraceEnabled()) {
            log.trace(localPeerName + " - " + "...released viewLatch (viewAccepted)");
        }
    }

    // JGroups 'MembershipListener' API
    public void suspect(org.jgroups.Address suspected_mbr) {
        if (log.isTraceEnabled())
            log.trace(localPeerName + " - " + "handling suspect(" + suspected_mbr + ")...");
        if (log.isWarnEnabled())
            log.warn("cluster suspects member may have been lost: " + suspected_mbr);
        if (log.isTraceEnabled())
            log.trace(localPeerName + " - " + "...suspect() handled");
    }

    // Block sending and receiving of messages until viewAccepted() is called
    public void block() {
        if (log.isTraceEnabled())
            log.trace(localPeerName + " - " + "handling block()...");
        // NYI
        if (log.isTraceEnabled())
            log.trace(localPeerName + " - " + "... block() handled");

    }

    // JGroups 'MessageListener' API
    public void receive(org.jgroups.Message msg) {
        if (log.isTraceEnabled()) {
            log.trace(localPeerName + " - message arrived: " + msg);
        }
        org.jgroups.Address src = msg.getSrc();
        org.jgroups.Address dest = msg.getDest();
        if (_excludeSelf && (dest == null || dest.isMulticastAddress()) && src == _localJGAddress) {
            if (log.isTraceEnabled()) {
                log.trace(localPeerName + " - " + "ignoring message from self: " + msg);
            }
        } else {
            // setup a ThreadLocal to be read during deserialisation...
            clusterThreadLocal.set(this);
            Object o = msg.getObject();
            JGroupsEnvelope wadiMsg = (JGroupsEnvelope) o;
            Serializable payload = wadiMsg.getPayload();
            if (payload instanceof ClusterCommand) {
                ((ClusterCommand) payload).execute(wadiMsg, this);
                return;
            }

            wadiMsg.setCluster(this);
            wadiMsg.setAddress((JGroupsPeer) getPeer(wadiMsg.getAddress()));
            wadiMsg.setReplyTo((JGroupsPeer) getPeer(wadiMsg.getReplyTo()));
            _dispatcher.onEnvelope(wadiMsg);
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
        return (JGroupsPeer) clusterPeer;
    }

    // 'JGroupsCluster' API
    public void send(Address target, Envelope message) throws MessageExchangeException {
        JGroupsEnvelope msg = (JGroupsEnvelope) message;
        JGroupsPeer tgt = (JGroupsPeer) target;
        try {
            msg.setCluster(this);
            msg.setAddress(target);
            msg.setCluster(null);
            _channel.send(tgt.getJGAddress(), _localJGAddress, msg);
        } catch (Exception e) {
            log.warn("unexpected JGroups problem", e);
            throw new MessageExchangeException(e);
        }
    }

    public Channel getChannel() {
        return _channel;
    }

    protected Object extractKeyFromPeerSerialization(Object backend) {
        JGroupsPeer remotePeer = (JGroupsPeer) backend;
        return remotePeer.getJGAddress();
    }
    
    protected Peer createPeerFromPeerSerialization(Object backend) {
        JGroupsPeer remotePeer = (JGroupsPeer) backend;
        org.jgroups.Address jgAddress = remotePeer.getJGAddress();
        JGroupsPeer peer;
        if (jgAddress.isMulticastAddress()) {
            peer = (JGroupsPeer) clusterPeer;
            // I can't find a way to initialise the clusterAddress from the client side
            // so I wait to receive a message, sent to the cluster, then use the address from that - clumsy
            if (peer.getJGAddress() == null) {
                peer.init(jgAddress);
            }
        } else {
            peer = new JGroupsRemotePeer(this, remotePeer);
        }
        return peer;
    }

}
