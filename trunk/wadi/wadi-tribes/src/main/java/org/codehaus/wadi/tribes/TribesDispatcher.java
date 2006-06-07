package org.codehaus.wadi.tribes;

import java.io.Serializable;
import java.util.Map;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.AbstractDispatcher;
import org.codehaus.wadi.group.impl.ThreadPool;
import org.apache.catalina.tribes.ChannelListener;

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
    
    public TribesDispatcher(ThreadPool executor) {
        super(executor);
        cluster = new TribesCluster();
    }

    public TribesDispatcher(String localPeerName, String clusterName, long inactiveTime, String config) {
        super(inactiveTime);
        cluster = new TribesCluster();
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
            cluster.channel.send(new Member[] {(TribesPeer)target},(TribesMessage)message,Channel.SEND_OPTIONS_DEFAULT);
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
            cluster.channel.getMembershipService().setPayload(XByteBuffer.serialize((Serializable)state));
        } catch ( Exception x ) {
            throw new MessageExchangeException(x);
        }
    }
    
    public void init(DispatcherConfig config) throws Exception {
        super.init(config);
    }
    
    public void messageReceived(Serializable serializable, Member member) {
        TribesMessage msg = (TribesMessage)serializable;
        msg.setAddress((Address)member);
        super.onMessage(msg);
    }
    public boolean accept(Serializable serializable, Member member) {
        return true;
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
}