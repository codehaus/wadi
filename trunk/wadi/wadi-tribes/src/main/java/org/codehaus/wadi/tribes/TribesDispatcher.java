package org.codehaus.wadi.tribes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractDispatcher;
import org.codehaus.wadi.group.impl.ThreadPool;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class TribesDispatcher extends AbstractDispatcher implements ChannelListener {
    protected TribesCluster cluster;
    protected String localPeerName = null;
    public TribesDispatcher(ThreadPool executor) {
        super(executor);
        cluster = new TribesCluster("WADI".getBytes());
    }

    public TribesDispatcher(String clusterName, String localPeerName, long inactiveTime, String config) {
        super(inactiveTime);
        //todo, create some sort of config file
        byte[] domain = getBytes(clusterName);
        cluster = new TribesCluster(domain);
        this.localPeerName = localPeerName;
    }
    
    public byte[] getBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch ( Exception x ) {
            return s.getBytes();
        }
    }

    /**
     * createMessage
     *
     * @return Message
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public Message createMessage() {
        return new TribesMessage();
    }

    /**
     * getAddress
     *
     * @param name String
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public Address getAddress(String name) {
        return (Address)cluster.getLocalPeer();
    }

    /**
     * getCluster
     *
     * @return Cluster
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public Cluster getCluster() {
        return cluster;
    }

    /**
     * getPeerName
     *
     * @param address Address
     * @return String
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public String getPeerName(Address address) {
        return ((TribesPeer)address).getName();
    }

    /**
     * Send a ready-made Message to the Peer at the 'target' Address.
     *
     * @param target The Address of the Peer to which the Message should be sent
     * @param message The Message itself
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void send(Address target, Message message) throws MessageExchangeException {
        try {
            cluster.channel.send(new Member[] {(TribesPeer)target},(TribesMessage)message,Channel.SEND_OPTIONS_ASYNCHRONOUS);
        }catch ( ChannelException x ) {
            throw new MessageExchangeException(x);
        }
    }

    /**
     * setDistributedState
     *
     * @param state Map
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void setDistributedState(Map state) throws MessageExchangeException {
        try {
            byte[] data = XByteBuffer.serialize((Serializable)state);
            cluster.channel.getMembershipService().setPayload(data);
            PeerUpdateMsg msg = new PeerUpdateMsg((TribesPeer)getCluster().getLocalPeer(),data);
            Member[] destination = cluster.channel.getMembers();
            if ( destination != null && destination.length > 0 )
                cluster.channel.send(destination,msg,Channel.SEND_OPTIONS_ASYNCHRONOUS);
        } catch ( Exception x ) {
            throw new MessageExchangeException(x);
        }
    }
    
    public void init(DispatcherConfig config) throws Exception {
        super.init(config);
        cluster.init();
        ((TribesPeer)cluster.getLocalPeer()).setName(localPeerName);
    }
    
    public void messageReceived(Serializable serializable, Member member) {
        if (serializable instanceof TribesMessage) {
            final TribesMessage msg = (TribesMessage) serializable;
            msg.setReplyTo((Address) member); //do we need this?
            msg.setAddress((Address) cluster.channel.getLocalMember(false));
            Runnable r = new Runnable() {
                public void run() {
                    onMessage(msg);
                }
            };
            try {
                _executor.execute(r);
            } catch ( InterruptedException x ) {
                this._log.error("Interrupted when a TribesMessage received, unable to hand it off to the thread pool.",x);
            }
        } else if (serializable instanceof PeerUpdateMsg) {
            final PeerUpdateMsg msg = (PeerUpdateMsg)serializable;
            final ArrayList list = cluster.getClusterListeners();
            final ClusterEvent event = new ClusterEvent(cluster,(Peer)member,ClusterEvent.PEER_UPDATED);
            TribesPeer peer = (TribesPeer)member;
            if ( !Arrays.equals(msg.getState(),peer.getPayload()) ) {
                peer.setPayload(msg.getState());
            }
            Runnable r = new Runnable() {
                public void run() {
                    for (int i=0; i<list.size(); i++ ) {
                        ClusterListener listener = (ClusterListener)list.get(i);
                        listener.onPeerUpdated(event);
                    }

                }
            };
            try {
                _executor.execute(r);
            } catch (InterruptedException x) {
                this._log.error("Interrupted when a TribesMessage received, unable to hand it off to the thread pool.",x);
            }
            
        }
    }
    public boolean accept(Serializable serializable, Member member) {

        boolean result = (serializable instanceof TribesMessage ||
                          serializable instanceof PeerUpdateMsg);
        //System.out.println("\n\n\nMEGA DEBUG\nAccept called on:"+this+" with "+serializable+ " result:"+result);
        return result;
    }
    

    /**
     * start
     *
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void start() throws MessageExchangeException {
        try {
            cluster.channel.addChannelListener(this);
            cluster.start();
        }catch ( Exception x ) {
            throw new MessageExchangeException(x);
        }
    }

    /**
     * stop
     *
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void stop() throws MessageExchangeException {
        try {
            cluster.stop();
        }catch ( Exception x ) {
            throw new MessageExchangeException(x);
        }
    }
    
    public static class PeerUpdateMsg implements Serializable {
        protected TribesPeer peer;
        protected byte[] state;
        
        public PeerUpdateMsg(TribesPeer peer, byte[] state) {
            this.peer = peer;
            this.state = state;
        }
        
        public TribesPeer getPeer() { return peer; }
        public byte[] getState() { return state;}
    } 
}