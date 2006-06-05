package org.codehaus.wadi.tribes;

import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;

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
public class TribesCluster implements Cluster {
    public TribesCluster() {
    }

    /**
     * addClusterListener
     *
     * @param listener ClusterListener
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void addClusterListener(ClusterListener listener) {
    }

    /**
     * getAddress
     *
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Address getAddress() {
        return null;
    }

    /**
     * @return - the number of millis that a Peer may remain silent before being declared suspect/dead..
     *
     * @return - the number of millis that a Peer may remain silent before being declared suspect/dead..
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public long getInactiveTime() {
        return 0L;
    }

    /**
     * getLocalPeer
     *
     * @return LocalPeer
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public LocalPeer getLocalPeer() {
        return null;
    }

    /**
     * getPeerCount
     *
     * @return int
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public int getPeerCount() {
        return 0;
    }

    /**
     * getPeerFromAddress
     *
     * @param address Address
     * @return Peer
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Peer getPeerFromAddress(Address address) {
        return null;
    }

    /**
     * getRemotePeers
     *
     * @return Map
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Map getRemotePeers() {
        return null;
    }

    /**
     * removeClusterListener
     *
     * @param listener ClusterListener
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void removeClusterListener(ClusterListener listener) {
    }

    /**
     * setElectionStrategy
     *
     * @param strategy ElectionStrategy
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void setElectionStrategy(ElectionStrategy strategy) {
    }

    /**
     * start
     *
     * @throws ClusterException
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void start() throws ClusterException {
    }

    /**
     * stop
     *
     * @throws ClusterException
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void stop() throws ClusterException {
    }

    /**
     *
     * @param membershipCount - when membership reaches this number or we timeout this method will return
     * @param timeout - the number of milliseconds to wait for membership to hit membershipCount
     * @return whether or not expected membershipCount was hit within given time
     * @throws InterruptedException
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws
        InterruptedException {
        return false;
    }
}