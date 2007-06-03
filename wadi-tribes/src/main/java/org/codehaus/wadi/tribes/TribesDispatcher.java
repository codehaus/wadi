package org.codehaus.wadi.tribes;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

public class TribesDispatcher extends AbstractDispatcher implements ChannelListener {
    protected TribesCluster cluster;
    protected final String localPeerName;
    protected final Collection staticMembers;
    
    public TribesDispatcher(String clusterName, String localPeerName, EndPoint endPoint) {
        this(clusterName, localPeerName, endPoint, Collections.EMPTY_LIST);
    }

    public TribesDispatcher(String clusterName, String localPeerName, EndPoint endPoint, Collection staticMembers) {
        if (null == staticMembers) {
            throw new IllegalArgumentException("staticMembers is required");
        }
        //todo, create some sort of config file
        byte[] domain = getBytes(clusterName);
        this.localPeerName = localPeerName;
        this.staticMembers = staticMembers;
        
        PeerInfo localPeerInfo = new PeerInfo(endPoint);
        cluster = new TribesCluster(domain, this, localPeerInfo);
    }

    public Collection getStaticMembers() {
        return staticMembers;
    }

    private byte[] getBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch ( Exception x ) {
            return s.getBytes();
        }
    }

    public Envelope createEnvelope() {
        return new TribesEnvelope();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public String getPeerName(Address address) {
        return ((TribesPeer)address).getName();
    }

    protected void doSend(Address target, Envelope envelope) throws MessageExchangeException {
        Member[] peers;
        if (target instanceof TribesClusterAddress) {
            TribesClusterAddress clusterAddress = (TribesClusterAddress) target;
            peers = clusterAddress.getPeers();
        } else {
            peers = new Member[] { (TribesPeer) target };
        }
        try {
            cluster.channel.send(peers, envelope, Channel.SEND_OPTIONS_ASYNCHRONOUS);
        }catch ( ChannelException x ) {
            throw new MessageExchangeException(x);
        }
    }

    public void messageReceived(Serializable serializable, Member member) {
        if (serializable instanceof TribesEnvelope) {
            final TribesEnvelope msg = (TribesEnvelope) serializable;
            msg.setReplyTo((Address) member); //do we need this?
            msg.setAddress((Address) cluster.channel.getLocalMember(false));
            Runnable r = new Runnable() {
                public void run() {
                    onEnvelope(msg);
                }
            };
            try {
                _executor.execute(r);
            } catch ( InterruptedException x ) {
                this._log.error("Interrupted when a TribesMessage received, unable to hand it off to the thread pool.",x);
            }
        } 
    }
    
    public boolean accept(Serializable serializable, Member member) {
        return serializable instanceof TribesEnvelope;
    }

    /**
     * start
     *
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void start() throws MessageExchangeException {
        try {
            cluster.init();
            ((TribesPeer)cluster.getLocalPeer()).setName(localPeerName);
            cluster.channel.addChannelListener(this);
            cluster.start();
        } catch ( Exception x ) {
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
    
}