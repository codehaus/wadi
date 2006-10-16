package org.codehaus.wadi.tribes;

import java.io.Serializable;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

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
    
    public TribesDispatcher(String clusterName, String localPeerName, long inactiveTime, String config) {
        super(inactiveTime);
        //todo, create some sort of config file
        byte[] domain = getBytes(clusterName);
        cluster = new TribesCluster(domain, this);
        this.localPeerName = localPeerName;
    }
    
    private byte[] getBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch ( Exception x ) {
            return s.getBytes();
        }
    }

    public Envelope createMessage() {
        return new TribesEnvelope();
    }

    public Address getAddress(String name) {
        return (Address)cluster.getLocalPeer();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public String getPeerName(Address address) {
        return ((TribesPeer)address).getName();
    }

    public void send(Address target, Envelope message) throws MessageExchangeException {
        try {
            cluster.channel.send(new Member[] {(TribesPeer)target},(TribesEnvelope)message,Channel.SEND_OPTIONS_ASYNCHRONOUS);
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
                    onMessage(msg);
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
        boolean result = (serializable instanceof TribesEnvelope);
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