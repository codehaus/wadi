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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.jgroups.messages.StateRequest;
import org.codehaus.wadi.jgroups.messages.StateResponse;
import org.codehaus.wadi.jgroups.messages.StateUpdate;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.MessageListener;
import org.jgroups.View;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

//TODO - regular state updates

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster implements Cluster, MembershipListener, MessageListener, JGroupsClusterMessageListener {

    protected final static String _nodeNameKey = "nodeName";
    protected final static String _birthTimeKey="birthTime";

    public static final ThreadLocal _cluster=new ThreadLocal();

    protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
    protected final Log _log=LogFactory.getLog(getClass());
    protected final String _clusterName;
    protected final Channel _channel;
    protected final List _clusterListeners=new ArrayList();
    protected final Map _jgaddressToPeer=new HashMap(); // we don't need this, but i/f does - yeugh !
    protected final boolean _excludeSelf=true;
    protected final Map _clusterState=new HashMap(); // a Map (Cluster) of Maps (Nodes) associating a Key (Address) with a Value (State)
    protected final String _localPeerName;
    protected final JGroupsLocalPeer _localPeer=new JGroupsLocalPeer(this, _clusterState);
    protected final Latch _latch=new Latch();
    protected final Latch _initLatch=new Latch();
    protected final ViewThread _viewThread=new ViewThread("WADI/JGroups View Thread");
    public final Map _addressToJGAddress=new ConcurrentHashMap();

    protected ElectionStrategy _electionStrategy;
    protected Peer _coordinator;
    protected JGroupsDispatcher _dispatcher;
    protected Address _localAddress;
    protected JGroupsAddress _localDestination;
    protected JGroupsTopic _clusterTopic;

    public JGroupsCluster(String clusterName, String localPeerName) throws ChannelException {
        super();
        _clusterName=clusterName;
        _localPeerName=localPeerName;
        _channel=new JChannel("default.xml"); // uses an xml stack config file from JGroups distro
        _cluster.set(this); // set ThreadLocal
    }

    // ActiveCluster 'Cluster' API
    public Map getRemotePeers() {
        return Collections.unmodifiableMap(_jgaddressToPeer); // could we cache this ? - TODO
    }

    public void addClusterListener(ClusterListener listener) {
        synchronized (_clusterListeners) {
            _clusterListeners.add(listener);
        }
    }

    public void removeClusterListener(ClusterListener listener) {
        synchronized (_clusterListeners) {
            _clusterListeners.remove(listener);
        }
    }

    public LocalPeer getLocalPeer() {
        return _localPeer;
    }

    public void setElectionStrategy(ElectionStrategy strategy) {
        _electionStrategy=strategy;
    }

    public void send(org.codehaus.wadi.group.Address destination, Message message) throws MessageExchangeException {
        JGroupsMessage msg=(JGroupsMessage)message;
        JGroupsAddress dest=(JGroupsAddress)destination;
        try {
            msg.setCluster(this);
            msg.setAddress(destination);
            if (_messageLog.isTraceEnabled()) _messageLog.trace("outgoing: "+msg.getPayload()+" {"+_localDestination.getName()+"->"+dest.getName()+"} - "+msg.getIncomingCorrelationId()+"/"+msg.getOutgoingCorrelationId());
            msg.setCluster(null);
            _channel.send(dest.getAddress(), _localAddress, msg);
        } catch (Exception e) {
            _log.warn("unexpected JGroups problem", e);
            throw new MessageExchangeException(e);
        }
    }

    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException {
        assert (membershipCount>0);
        membershipCount--; // remove ourselves from the equation...
        long expired=0;
        while ((getRemotePeers().size())!=membershipCount && expired<timeout) {
            Thread.sleep(1000);
            expired+=1000;
        }
        return (getRemotePeers().size())==membershipCount;
    }

    public void start() throws ClusterException {
        try {
            //_channel.setOpt(Channel.LOCAL, Boolean.FALSE); // exclude ourselves from our own broadcasts... - BUT also from Unicasts :-(
            _channel.connect(_clusterName);
            long joinTime=System.currentTimeMillis();
            _log.info(_localPeerName+" - "+"connected to channel");
            _clusterTopic=new JGroupsTopic("CLUSTER", null); // null address means broadcast to all members
            _localAddress=_channel.getLocalAddress();
            _clusterState.put(_localAddress, new HashMap()); // initialise our distributed state
            _localDestination=new JGroupsAddress(_localAddress);
            _localDestination.init(_localPeer);
            _localPeer.init(_localDestination);
            Map localState=_localPeer.getState();
            localState.put(_nodeNameKey, _localPeerName);
            localState.put(_birthTimeKey, new Long(joinTime));
            _addressToJGAddress.put(_localAddress, _localDestination);
            _log.trace(_localPeerName+" - "+"releasing latch...");
            _latch.release(); // allow new view to be accepted
            _log.trace(_localPeerName+" - "+"...released latch");
        } catch (Exception e) {
            _log.warn("unexpected JGroups problem", e);
            throw new ClusterException(e);
        }

        // we don't really want anyone else running until we have initialised properly... - how ?
        _log.trace(_localPeerName+" - "+"releasing initLatch...");
        _initLatch.release();
        _log.trace(_localPeerName+" - "+"...released initLatch");
        _viewThread.start();
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
        _clusterState.clear();
    }

    // JGroups MembershipListener API

    protected final EDU.oswego.cs.dl.util.concurrent.Channel _viewQueue=new LinkedQueue();
    
    public synchronized void viewAccepted(View newView) {
        // this is meant to happen if a network split is healed and two
        // clusters try to reconcile their separate states into one -
        // I have a plan...
        if(newView instanceof MergeView)
            if (_log.isWarnEnabled()) _log.warn("NYI - merging: view is " + newView);

        Set newMembers=new TreeSet(newView.getMembers());
        try {
            if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"handling JGroups viewAccepted("+newView+")...");
            _viewQueue.put(newMembers);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
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
                    Set newMembers=(Set)_viewQueue.poll(2000);
                    if (newMembers!=null)
                        nextView(newMembers);
                } catch (InterruptedException e) {
                    _log.warn("unexpected interruption", e);
                }
            }
        }
    }
    
    public void nextView(Set newMembers) {        
        // must not run until we are initialised...
        _log.trace(_localPeerName+" - "+"acquiring initLatch (viewAccepted)...");
        try {_initLatch.acquire();} catch (Exception e) {_log.error("problem syncing Cluster initialisation", e);}
        _log.trace(_localPeerName+" - "+"...acquired initLatch (viewAccepted)");
        // we don't want to overtake the thread that is initialising this object...
        try {
            _log.trace(_localPeerName+" - "+"acquiring latch (viewAccepted)...");
            _latch.acquire();
            _log.trace(_localPeerName+" - "+"...acquired latch (viewAccepted)");
        } catch (InterruptedException e) {
            // hmmm...
        }

        Set joiners=new TreeSet();
        Set leavers=new TreeSet();

        synchronized (_jgaddressToPeer) {
            // joiners :

            for (Iterator i=newMembers.iterator(); i.hasNext(); ) {
                Address address=(Address)i.next();
                JGroupsAddress jgaddress=JGroupsAddress.get(JGroupsCluster.this, address);
                if (jgaddress!=_localDestination) {
                    if (_clusterState.get(address)==null) {
                        // fetch state - a quick round trip to fetch the distributed data for this node - yeugh !
                        // may cause problems if dispatcher note yet initialised...
                        try {
                            long timeout=100000;
                            JGroupsMessage tmp=(JGroupsMessage)_dispatcher.exchangeSend(jgaddress, new StateRequest(_localPeer.getState()), timeout);
                            Map state=((StateResponse)tmp.getPayload()).getState();
                            synchronized (_clusterState) {
                                _clusterState.put(address, state);
                            }

                            Peer peer=jgaddress.getPeer();
                            // insert node
                            synchronized (_jgaddressToPeer) {
                                _jgaddressToPeer.put(jgaddress, peer);
                            }

                            joiners.add(peer);
                        } catch (Exception e) {
                            _log.warn("problem retrieving remote state for fresh joiner...", e);
                            return;
                        }
                    }
                }
            }

            // leavers :

            for (Iterator i=_jgaddressToPeer.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry=(Map.Entry)i.next();
                JGroupsAddress jgaddress=(JGroupsAddress)entry.getKey();
                Peer peer=(Peer)entry.getValue();
                if (!newMembers.contains(jgaddress.getAddress())) {
                    leavers.add(peer);
                    i.remove(); // remove from _jgAddressToPeer
                    // garbage collect this nodes share of cluster state...
                    synchronized (_clusterState) {
                        _clusterState.remove(jgaddress.getAddress());
                    }
                    // remove peer
                    _addressToJGAddress.remove(jgaddress.getAddress());
                }
            }

        }

        // ensure that all joiners are entered into our model...

        joiners=Collections.unmodifiableSet(joiners);
        leavers=Collections.unmodifiableSet(leavers);

        // elect coordinator - should be run before onMembershipChanged called, since the coordinator might have to take specific action...
        runCoordinatorElection();

        // notify listeners of changed membership
        if (_clusterListeners.size()>0) {
            for (int j=0; j<_clusterListeners.size(); j++)
                ((ClusterListener)_clusterListeners.get(j)).onMembershipChanged(this, joiners, leavers);
        }
    }


    protected void runCoordinatorElection() {
        if (_clusterListeners.size()>0 && _electionStrategy!=null) {
            Peer coordinator=_localPeer;
            if (_jgaddressToPeer.size()>0) {
                coordinator=_electionStrategy.doElection(JGroupsCluster.this);
            }
            if (_coordinator==null || !_coordinator.equals(coordinator)) {
                _coordinator=coordinator;
                ClusterEvent event=new ClusterEvent(JGroupsCluster.this, coordinator, ClusterEvent.COORDINATOR_ELECTED);
                for (int i=0; i<_clusterListeners.size(); i++)
                    ((ClusterListener)_clusterListeners.get(i)).onCoordinatorChanged(event);
            }
        }
    }

    public void suspect(Address suspected_mbr) {
        _log.trace(_localPeerName+" - "+"acquiring latch (suspect)...");
        try {_initLatch.acquire();} catch (Exception e) {_log.error("problem syncing Cluster initialisation", e);}
        _log.trace(_localPeerName+" - "+"...acquired latch (suspect)");
        if (_log.isTraceEnabled()) _log.trace(_localPeerName+" - "+"handling suspect("+suspected_mbr+")...");
        if (_log.isWarnEnabled()) _log.warn("cluster suspects member may have been lost: " + suspected_mbr);
        _log.trace(_localPeerName+" - "+"...suspect() handled");
    }

    // Block sending and receiving of messages until viewAccepted() is called
    public void block() {
        _log.trace(_localPeerName+" - "+"handling block()...");
        // NYI
        _log.trace(_localPeerName+" - "+"... block() handled");

    }

    //-----------------------------------------------------------------------------------------------
    // JGroups MessageListener API

    public void receive(org.jgroups.Message msg) {
        // TODO: this may be a little contentious - think of a better way...
        _log.trace(_localPeerName+" - "+_localPeer+" - RECEIVED SOMETHING: "+msg);
        _log.trace(_localPeerName+" - "+"acquiring initLatch (receive)...");
        try {_initLatch.acquire();} catch (Exception e) {_log.error("problem syncing Cluster initialisation", e);}
        _log.trace(_localPeerName+" - "+"...acquired initLatch (receive)");
        synchronized (_channel) {
            Address src=msg.getSrc();
            Address dest=msg.getDest();
            if (_excludeSelf && (dest==null || dest.isMulticastAddress()) && src==_localAddress) {
                _log.debug(_localPeerName+" - "+"ignoring message from self: "+msg);
            } else {
                _cluster.set(this); // setup a ThreadLocal to be read during deserialisation...
                Object o=msg.getObject();

                JGroupsMessage message=(JGroupsMessage)o;
                message.setCluster(this);
                org.codehaus.wadi.group.Address replyTo=getAddress(src);
                message.setReplyTo(replyTo);
                message.setAddress(getAddress(dest));
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

    // WADI 'JGroupsCluster' API

    public void init(JGroupsDispatcher dispatcher) throws Exception {
        _dispatcher=dispatcher;
        ServiceEndpointBuilder endpointBuilder = new ServiceEndpointBuilder();
        endpointBuilder.addSEI(_dispatcher, JGroupsClusterMessageListener.class, this);
        endpointBuilder.addCallback(_dispatcher, StateResponse.class);
    }

    public Channel getChannel() {
        return _channel;
    }

    public int getNumNodes() {
        synchronized (_jgaddressToPeer) {
            return _jgaddressToPeer.size()+1; // TODO - resolve - getNumNodes() returns N, but getNodes() returns N-1
        }
    }

    public Address getLocalAddress() {
        return _localAddress;
    }

    public JGroupsAddress getLocalDestination() {
        return _localDestination;
    }

    /**
     * Translate a JGroups Address into an ActiveCluster Destination.
     * @param address
     * @return
     */
    public JGroupsAddress getAddress(Address address) {
        JGroupsAddress jgaddress;
        if (address==null || address.isMulticastAddress())
            jgaddress=_clusterTopic;
        else
            jgaddress=JGroupsAddress.get(this, address);

        return jgaddress;
    }

    // Dispatcher API

    public void onMessage(Message message, StateUpdate update) throws Exception {
        JGroupsAddress jgaddress=(JGroupsAddress)message.getReplyTo();
        Peer node=jgaddress.getPeer();
        Map state=update.getState();
        if (node instanceof JGroupsRemotePeer) {
            ((JGroupsRemotePeer)node).setState(state);
            if (_clusterListeners.size()>0) {
                ClusterEvent event=new ClusterEvent(this, node, ClusterEvent.PEER_UPDATED);
                for (int i=0; i<_clusterListeners.size(); i++)
                    ((ClusterListener)_clusterListeners.get(i)).onPeerUpdated(event);
            }
        } else {
            _log.warn(_localPeerName+" - "+"state update from non-remote node: "+node);
        }
    }

    public void onMessage(Message message, StateRequest request) throws Exception {
        Map remoteState=request.getState();
        synchronized (_clusterState) {
            _clusterState.put(((JGroupsAddress)message.getReplyTo()).getAddress(), remoteState);
        }
        Map localState=_localPeer.getState();
        _dispatcher.reply(message, new StateResponse(localState));
    }

    public Map getClusterState() {
        return _clusterState;
    }

    protected static final String _prefix="<"+JGroupsCluster.class.getPackage().getName()+": ";
    protected static final String _suffix=">";

    public String toString() {
        return _prefix+_localPeerName+"@"+_clusterName+_suffix;
    }

    public org.codehaus.wadi.group.Address getAddress() {
        return _clusterTopic;
    }
}
