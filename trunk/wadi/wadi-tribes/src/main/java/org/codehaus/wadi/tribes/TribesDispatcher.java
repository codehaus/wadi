package org.codehaus.wadi.tribes;

import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
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
public class TribesDispatcher extends AbstractDispatcher {
    public TribesDispatcher(ThreadPool executor) {
        super(executor);
    }

    public TribesDispatcher(long inactiveTime) {
        super(inactiveTime);
    }

    /**
     * createMessage
     *
     * @return Message
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public Message createMessage() {
        return null;
    }

    /**
     * getAddress
     *
     * @param name String
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public Address getAddress(String name) {
        return null;
    }

    /**
     * getCluster
     *
     * @return Cluster
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public Cluster getCluster() {
        return null;
    }

    /**
     * getPeerName
     *
     * @param address Address
     * @return String
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public String getPeerName(Address address) {
        return "";
    }

    /**
     * Send a ready-made Message to the Peer at the 'target' Address.
     *
     * @param target The Address of the Peer to which the Message should be sent
     * @param message The Message itself
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void send(Address target, Message message) throws
        MessageExchangeException {
    }

    /**
     * setDistributedState
     *
     * @param state Map
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void setDistributedState(Map state) throws MessageExchangeException {
    }

    /**
     * start
     *
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void start() throws MessageExchangeException {
    }

    /**
     * stop
     *
     * @throws MessageExchangeException
     * @todo Implement this org.codehaus.wadi.group.Dispatcher method
     */
    public void stop() throws MessageExchangeException {
    }
}