package org.codehaus.wadi.tribes;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.UniqueId;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.PeerInfo;

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
public class TribesPeer implements Member, LocalPeer, Address {
    
    public static byte[] writePayload(String peerName, PeerInfo peerInfo) {
        TribesPeerInfo tribesPeerInfo = new TribesPeerInfo(peerName, peerInfo);
        try {
            ByteArrayOutputStream memOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(memOut);
            out.writeObject(tribesPeerInfo);
            return memOut.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static TribesPeerInfo getTribesPeerInfo(byte[] payload) {
        ByteArrayInputStream memIn = new ByteArrayInputStream(payload);
        try {
            ObjectInputStream in = new ObjectInputStream(memIn);
            return (TribesPeerInfo) in.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected transient MemberImpl member;
    protected transient boolean stateModified = true;
    private final String name;
    private final PeerInfo peerInfo;
    private final UniqueId uniqueId;
    
    public TribesPeer(MemberImpl mbr) {
        if (null == mbr) {
            throw new IllegalArgumentException("mbr is required");
        }
        this.member = mbr;
        this.uniqueId = new UniqueId(mbr.getUniqueId());
        
        TribesPeerInfo tribesPeerInfo = getTribesPeerInfo(mbr.getPayload());
        
        name = tribesPeerInfo.peerName;
        peerInfo = tribesPeerInfo.peerInfo;
    }

    /**
     * getHost
     *
     * @return byte[]
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public byte[] getHost() {
        return member.getHost();
    }

    /**
     * getMemberAliveTime
     *
     * @return long
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public long getMemberAliveTime() {
        return member.getMemberAliveTime();
    }

    /**
     * getName
     *
     * @return String
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public String getName() {
        return name;
    }
    
    public PeerInfo getPeerInfo() {
        return peerInfo;
    }
    
    /**
     * getPayload
     *
     * @return byte[]
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public byte[] getPayload() {
        return member.getPayload();
    }

    /**
     * getPort
     *
     * @return int
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public int getPort() {
        return member.getPort();
    }

    public int getSecurePort() {
        return member.getSecurePort();
    }
    
    /**
     * getUniqueId
     *
     * @return byte[]
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public byte[] getUniqueId() {
        return uniqueId.getBytes();
    }

    /**
     * isFailing
     *
     * @return boolean
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public boolean isFailing() {
        return member.isFailing();
    }

    /**
     * isReady
     *
     * @return boolean
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public boolean isReady() {
        return member.isReady();
    }

    /**
     * isSuspect
     *
     * @return boolean
     * @todo Implement this org.apache.catalina.tribes.Member method
     */
    public boolean isSuspect() {
        return member.isSuspect();
    }
    
    public Address getAddress() { 
        return this;
    }
    
    public byte[] getDomain() {
        return member.getDomain();
    }
    
    public byte[] getCommand() {
        return member.getCommand();
    }
    
    public int hashCode() {
        return uniqueId.hashCode();
    }
    
    public boolean equals(Object o) {
        if ( o instanceof TribesPeer ) {
            return uniqueId.equals(((TribesPeer)o).uniqueId);
        } else return false;
    }    
    
    protected Object readResolve() throws ObjectStreamException {
        Member mbr = WadiMemberInterceptor.reverseWrap(this);
        TribesPeer peer = WadiMemberInterceptor.wrap(mbr);
        return peer;
    }
    
    public String toString() {
        return "TribesPeer [" + name + "; " + member.getName() + "]";
    }

    private static class TribesPeerInfo implements Serializable {
        private final String peerName;
        private final PeerInfo peerInfo;
        
        protected TribesPeerInfo() {
            this.peerName = null;
            this.peerInfo = null;
        }
        
        protected TribesPeerInfo(String peerName, PeerInfo peerInfo) {
            this.peerName = peerName;
            this.peerInfo = peerInfo;
        }
    }
    
}