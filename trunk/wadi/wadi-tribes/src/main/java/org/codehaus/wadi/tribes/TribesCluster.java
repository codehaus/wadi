package org.codehaus.wadi.tribes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.DomainFilterInterceptor;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.membership.McastService;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
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
    
    private final byte[] clusterDomain;
    protected GroupChannel channel = null;
    protected ArrayList listeners = new ArrayList();
    private ElectionStrategy strategy;
    protected boolean initialized = false;
    private Member coordinator;
    private final TribesDispatcher dispatcher;

    public TribesCluster(byte[] clusterDomain, TribesDispatcher dispatcher) {
        if (null == clusterDomain) {
            throw new IllegalArgumentException("clusterDomain is required");
        } else if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        }
        this.clusterDomain = clusterDomain;
        this.dispatcher = dispatcher;
        
        channel = new GroupChannel();
        channel.addInterceptor(new WadiMemberInterceptor());
        //uncomment for java1.5
        //channel.addInterceptor(new MessageDispatch15Interceptor());
        //comment out for java 1.5
        channel.addInterceptor(new MessageDispatchInterceptor());
        channel.addMembershipListener(new WadiListener(this));
        ((McastService)channel.getMembershipService()).setMcastAddr("224.0.0.4");
        ((McastService)channel.getMembershipService()).setDomain(clusterDomain);
        DomainFilterInterceptor filter = new DomainFilterInterceptor();
        filter.setDomain(clusterDomain);
        channel.addInterceptor(filter);
        //channel.addInterceptor(new MessageTrackInterceptor());//for debug only
        channel.addInterceptor(new TcpFailureDetector());//this one should always be at the bottom
    }
    
    public String getClusterName() {
        return new String(clusterDomain);
    }
    
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * addClusterListener
     *
     * @param listener ClusterListener
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void addClusterListener(ClusterListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        Set existing = new HashSet(getRemotePeers().values());
        listener.onListenerRegistration(this, existing, (Peer) coordinator);
    }
    
    public List getClusterListeners() {
        return Collections.unmodifiableList(listeners);
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
        //System.out.println("\n\n\n\n\n\n\nMEGA DEBUG Local peer="+channel.getLocalMember(true)+"\n\n\n\n");
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
    
    protected class WadiListener implements MembershipListener {
        TribesCluster cluster;
        public WadiListener(TribesCluster cluster) {
            this.cluster = cluster;
        }
        
        public synchronized void memberAdded(Member member) {
            coordinator = electCoordinator();
            HashSet added = new HashSet();
            HashSet removed = new HashSet();
            if ( !member.equals(cluster.channel.getLocalMember(false)) ) added.add(member);
            for (int i=0; i<cluster.listeners.size(); i++ ) {
                ClusterListener listener = (ClusterListener)cluster.listeners.get(i);
                listener.onMembershipChanged(cluster,added,removed,(Peer)coordinator);
                //listener.onPeerUpdated(event); //do we need this
            }
        }

        
        public synchronized void memberDisappeared(Member member) {
            coordinator = electCoordinator();
            HashSet added = new HashSet();
            HashSet removed = new HashSet();
            removed.add(member);
            for (int i = 0; i < cluster.listeners.size(); i++) {
                ClusterListener listener = (ClusterListener) cluster.listeners.get(i);
                listener.onMembershipChanged(cluster, added, removed,(Peer) coordinator);
                //listener.onPeerUpdated(event); //do we need this
            }
        }

        private Member electCoordinator() {
            Member[] mbrs = channel.getMembers();
            Member local = channel.getLocalMember(true);
            Member newCoordinator = mbrs.length>0?mbrs[0]:local;
            if ( local.getMemberAliveTime() >= newCoordinator.getMemberAliveTime() ) newCoordinator = local;
            return newCoordinator;
        }
    }

    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        this.strategy = electionStrategy;
    }

    public ElectionStrategy getElectionStrategy() {
        return strategy;
    }

}