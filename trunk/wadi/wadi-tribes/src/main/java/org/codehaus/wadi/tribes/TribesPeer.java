package org.codehaus.wadi.tribes;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.UniqueId;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class TribesPeer implements Member, LocalPeer, Address, Serializable {
    
    protected transient MemberImpl member;
    protected transient final Log log = LogFactory.getLog(TribesPeer.class);
    protected transient boolean stateModified = true;
    private String name;
    private final PeerInfo peerInfo;
    protected UniqueId uniqueId = null;
    
    public TribesPeer(MemberImpl mbr) {
        if (null == mbr) {
            throw new IllegalArgumentException("mbr is required");
        }
        this.member = mbr;
        this.uniqueId = new UniqueId(mbr.getUniqueId());
        
        peerInfo = new PeerInfo();
    }

    public TribesPeer(MemberImpl mbr, String name, PeerInfo peerInfo) {
        if (null == mbr) {
            throw new IllegalArgumentException("mbr is required");
        } else if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (null == peerInfo) {
            throw new IllegalArgumentException("peerInfo is required");
        }
        this.member = mbr;
        this.name = name;
        this.uniqueId = new UniqueId(mbr.getUniqueId());
        this.peerInfo = peerInfo;
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
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        byte[] uid = new byte[in.readInt()];
        in.read(uid);
        uniqueId = new UniqueId(uid);
        name = in.readUTF();
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(uniqueId.getBytes().length);
        out.write(uniqueId.getBytes(),0,uniqueId.getBytes().length);
        out.writeUTF(name);
    }
    
    public String toString() {
        return "TribesPeer[id:"+uniqueId+"; member:"+member+"]";
    }

    public void setName(String name) {
        this.name = name;
    }

}