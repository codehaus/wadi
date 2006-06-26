package org.codehaus.wadi.tribes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.membership.McastService;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import java.util.LinkedHashMap;

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
    protected GroupChannel channel = null;
    protected ArrayList listeners = new ArrayList();
    private ElectionStrategy strategy;

    public TribesCluster() {
        channel = new GroupChannel();
        channel.addInterceptor(new WadiMemberInterceptor());
        channel.addInterceptor(new TcpFailureDetector());
        //uncomment for java1.5
        //channel.addInterceptor(new MessageDispatch15Interceptor());
        //comment out for java 1.5
        channel.addInterceptor(new MessageDispatchInterceptor());
        channel.addMembershipListener(new WadiListener(this));
    }

    /**
     * addClusterListener
     *
     * @param listener ClusterListener
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void addClusterListener(ClusterListener listener) {
        listeners.add(listener);
    }
    
    public ArrayList getClusterListeners() {
        return listeners;
    }

    /**
     * getAddress
     *
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Address getAddress() {
        return (Address)channel.getLocalMember(true);
    }

    /**
     * @return - the number of millis that a Peer may remain silent before being declared suspect/dead..
     *
     * @return - the number of millis that a Peer may remain silent before being declared suspect/dead..
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public long getInactiveTime() {
        return ((McastService)channel.getMembershipService()).getMcastDropTime();
    }

    /**
     * getLocalPeer
     *
     * @return LocalPeer
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public LocalPeer getLocalPeer() {
        return (LocalPeer)channel.getLocalMember(true);
    }

    /**
     * getPeerCount
     *
     * @return int
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public int getPeerCount() {
        return channel.getMembers().length+1;
    }

    /**
     * getPeerFromAddress
     *
     * @param address Address
     * @return Peer
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Peer getPeerFromAddress(Address address) {
        if ( address instanceof TribesPeer ) return (Peer)address;
        return null;
    }

    /**
     * getRemotePeers
     *
     * @return Map
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Map getRemotePeers() {
        Member[] mbrs = channel.getMembers();
        LinkedHashMap result = new LinkedHashMap();
        for (int i=0; i<mbrs.length; i++) result.put(mbrs[i],mbrs[i]);
        return result;
    }

    /**
     * removeClusterListener
     *
     * @param listener ClusterListener
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void removeClusterListener(ClusterListener listener) {
        this.listeners.remove(listener);
    }
    
    protected boolean initialized = false;
    public void init() throws ClusterException {
        try {
            channel.start(Channel.SND_RX_SEQ);
            initialized = true;
        }catch ( ChannelException x ) {
            throw new ClusterException(x);
        }
    }

    /**
     * start
     *
     * @throws ClusterException
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void start() throws ClusterException {
        try {
            if (!initialized) init();
            channel.start(Channel.MBR_RX_SEQ | Channel.MBR_TX_SEQ | Channel.SND_TX_SEQ);
        }catch ( ChannelException x ) {
            throw new ClusterException(x);
        }
    }

    /**
     * stop
     *
     * @throws ClusterException
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void stop() throws ClusterException {
        try {
            channel.stop(Channel.DEFAULT);
            initialized = false;
        }catch ( ChannelException x ) {
            throw new ClusterException(x);
        }

    }

    /**
     *
     * @param membershipCount - when membership reaches this number or we timeout this method will return
     * @param timeout - the number of milliseconds to wait for membership to hit membershipCount
     * @return whether or not expected membershipCount was hit within given time
     * @throws InterruptedException
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        long delta = System.currentTimeMillis()-start;
        while ( delta < timeout ) {
            if ( (channel.getMembers().length+1) == membershipCount ) return true;
            else {
                try { Thread.sleep(100); } catch (InterruptedException x) {Thread.interrupted();}
            }
            delta = System.currentTimeMillis()-start;
        }//while
        return false;
    }
    
    protected static class WadiListener implements MembershipListener {
        TribesCluster cluster;
        public WadiListener(TribesCluster cluster) {
            this.cluster = cluster;
        }
        
        public void memberAdded(Member member) {
            Member[] mbrs = cluster.channel.getMembers();
            Member local = cluster.channel.getLocalMember(true);
            Member coordinator = mbrs.length>0?mbrs[0]:local;
            if ( local.getMemberAliveTime() >= coordinator.getMemberAliveTime() ) coordinator = local;
            HashSet added = new HashSet();
            HashSet removed = new HashSet();
            if ( !member.equals(cluster.channel.getLocalMember(false)) ) added.add(member);
            for (int i=0; i<cluster.listeners.size(); i++ ) {
                ClusterListener listener = (ClusterListener)cluster.listeners.get(i);
                listener.onMembershipChanged(cluster,added,removed,(Peer)coordinator);
                //listener.onPeerUpdated(event); //do we need this
            }
        }
        
        public void memberDisappeared(Member member) {
            Member[] mbrs = cluster.channel.getMembers();
            Member local = cluster.channel.getLocalMember(true);
            Member coordinator = mbrs.length>0?mbrs[0]:local;
            if ( local.getMemberAliveTime() >= coordinator.getMemberAliveTime() ) coordinator = local;
            HashSet added = new HashSet();
            HashSet removed = new HashSet();
            removed.add(member);
            for (int i = 0; i < cluster.listeners.size(); i++) {
                ClusterListener listener = (ClusterListener) cluster.listeners.get(i);
                listener.onMembershipChanged(cluster, added, removed,(Peer) coordinator);
                //listener.onPeerUpdated(event); //do we need this
            }
        }
    }


    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        this.strategy = electionStrategy;
    }

    public ElectionStrategy getElectionStrategy() {
        return strategy;
    }
}