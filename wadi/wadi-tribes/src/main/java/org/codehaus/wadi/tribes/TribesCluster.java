package org.codehaus.wadi.tribes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.DomainFilterInterceptor;
import org.apache.catalina.tribes.group.interceptors.MessageDispatch15Interceptor;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.McastService;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.transport.ReceiverBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;

public class TribesCluster implements Cluster {
    private static final Log LOG = LogFactory.getLog(TribesCluster.class);
    private static final String SYS_PROP_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
    private static final String SYS_PROP_INHIBIT_AUTO_PREFER_IPV4_STACK = 
        "org.codehaus.wadi.tribes.inhibit.auto.preferIPv4Stack";

    private final byte[] clusterDomain;
    protected GroupChannel channel;
    protected List<ClusterListener> listeners = new CopyOnWriteArrayList<ClusterListener>();
    protected boolean initialized;
    private final TribesDispatcher dispatcher;
    private final boolean disableMulticasting;

    public TribesCluster(byte[] clusterDomain,
            TribesDispatcher dispatcher,
            String localPeerName,
            PeerInfo localPeerinfo) {
        this(clusterDomain, dispatcher, localPeerName, localPeerinfo, false, null, 4000);
    }
    
    public TribesCluster(byte[] clusterDomain,
            TribesDispatcher dispatcher,
            String localPeerName,
            PeerInfo localPeerinfo,
            boolean disableMulticasting,
            Properties mcastServiceProperties,
            int receiverPort) {
        if (null == clusterDomain) {
            throw new IllegalArgumentException("clusterDomain is required");
        } else if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (receiverPort < 1) {
            throw new IllegalArgumentException("receiverPort must be greater than zero");
        }
        this.clusterDomain = clusterDomain;
        this.dispatcher = dispatcher;
        this.disableMulticasting = disableMulticasting;
        
        channel = new GroupChannel();

        ReceiverBase receiver = (ReceiverBase) channel.getChannelReceiver();
        receiver.setPort(receiverPort);
        
        channel.addInterceptor(new WadiMemberInterceptor());
        channel.addInterceptor(new MessageDispatch15Interceptor());
        channel.addMembershipListener(new WadiListener(this));
        
        McastService mcastService = (McastService) channel.getMembershipService();
        if (null == mcastServiceProperties) {
            mcastService.setAddress("224.0.0.4");
        } else {
            mcastServiceProperties.setProperty("tcpListenPort", "123456789");
            mcastServiceProperties.setProperty("tcpListenHost", "NOT_USED");
            mcastService.setProperties(mcastServiceProperties);
        }
        mcastService.setDomain(clusterDomain);
        
        byte[] payload = TribesPeer.writePayload(localPeerName, localPeerinfo);
        channel.getMembershipService().setPayload(payload);
        
        DomainFilterInterceptor filter = new DomainFilterInterceptor();
        filter.setDomain(clusterDomain);
        channel.addInterceptor(filter);

        channel.addInterceptor(new TcpPingInterceptor());
        channel.addInterceptor(new TcpFailureDetector());

        addStaticMembers(dispatcher);
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
        listeners.add(listener);
        
        Set existing = new HashSet(getRemotePeers().values());
        listener.onListenerRegistration(this, existing);
    }
    
    /**
     * getAddress
     *
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public Address getAddress() {
        Member[] mbrs = channel.getMembers();
        TribesPeer[] peers = new TribesPeer[mbrs.length + 1];
        for (int i = 0; i < mbrs.length; i++) {
            peers[i] = (TribesPeer) mbrs[i];
        }
        peers[peers.length - 1] = (TribesPeer) channel.getLocalMember(true);
        return new TribesClusterAddress(peers);
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
    public Map<Address, Peer> getRemotePeers() {
        Member[] mbrs = channel.getMembers();
        LinkedHashMap<Address, Peer> result = new LinkedHashMap<Address, Peer>();
        for (int i=0; i<mbrs.length; i++) result.put((TribesPeer) mbrs[i], (TribesPeer) mbrs[i]);
        return result;
    }

    /**
     * removeClusterListener
     *
     * @param listener ClusterListener
     * @todo Implement this org.codehaus.wadi.group.Cluster method
     */
    public void removeClusterListener(ClusterListener listener) {
        listeners.remove(listener);
    }
    
    public void init() throws ClusterException {
        ensurePreferIPv4StackPropertyIsSet();
        
        try {
            channel.start(Channel.SND_RX_SEQ);
            initialized = true;
        }catch ( ChannelException x ) {
            throw new ClusterException(x);
        }
    }

    protected void ensurePreferIPv4StackPropertyIsSet() {
        String preferIPv4Stack = System.getProperty(SYS_PROP_PREFER_IPV4_STACK);
        if (null == preferIPv4Stack) {
            if (null != System.getProperty(SYS_PROP_INHIBIT_AUTO_PREFER_IPV4_STACK)) {
                LOG.warn("System property java.net.preferIPv4Stack is not set and auto-set if inhibited. Tribes " +
                		"multicasting will not properly work. You need to have a static cluster configuration.");
            } else {
                LOG.info("System property java.net.preferIPv4Stack has been set automatically to true so that " +
                		"Tribes multicasting properly works.");
                System.setProperty(SYS_PROP_PREFER_IPV4_STACK, "true");
            }
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
            if (disableMulticasting) {
                channel.start(Channel.SND_TX_SEQ);
            } else {
                channel.start(Channel.MBR_RX_SEQ | Channel.MBR_TX_SEQ | Channel.SND_TX_SEQ);
            }
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
    
    protected void addStaticMembers(TribesDispatcher dispatcher) {
        Collection<StaticMember> staticMembers = dispatcher.getStaticMembers();
        if (!staticMembers.isEmpty()) {
            StaticMembershipInterceptor smi = new StaticMembershipInterceptor();
            for (Member member : staticMembers) {
                smi.addStaticMember(member);
            }
            channel.addInterceptor(smi);
        }
    }

    protected class WadiListener implements MembershipListener {
        TribesCluster cluster;
        public WadiListener(TribesCluster cluster) {
            this.cluster = cluster;
        }
        
        public synchronized void memberAdded(Member member) {
            HashSet added = new HashSet();
            HashSet removed = new HashSet();
            if ( !member.equals(cluster.channel.getLocalMember(false)) ) added.add(member);
            for (Iterator<ClusterListener> iter = cluster.listeners.iterator(); iter.hasNext();) {
                ClusterListener listener = iter.next();
                listener.onMembershipChanged(cluster,added,removed);
            }
        }
        
        public synchronized void memberDisappeared(Member member) {
            HashSet added = new HashSet();
            HashSet removed = new HashSet();
            removed.add(member);
            for (Iterator<ClusterListener> iter = cluster.listeners.iterator(); iter.hasNext();) {
                ClusterListener listener = iter.next();
                listener.onMembershipChanged(cluster, added, removed);
            }
        }
    }

}
