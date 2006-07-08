package org.codehaus.wadi.tribes;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.UniqueId;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
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
public class TribesPeer implements Member, LocalPeer, Address, Serializable {
    
    protected transient MemberImpl member;
    protected transient Map state = null;
    protected transient final Log log = LogFactory.getLog(TribesPeer.class);
    protected transient boolean stateModified = true;
    protected String name = null;
    protected UniqueId uniqueId = null;
    
    public TribesPeer() {} //for serialization
    public TribesPeer(MemberImpl mbr) {
        this.member = mbr;
        this.uniqueId = new UniqueId(mbr.getUniqueId());
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
        if ( name != null ) return name;
        else if ( getState().get(Peer._peerNameKey) != null ) {
            name = (String)getState().get(Peer._peerNameKey);
            return name;
        } else return member.getName();
    }
    
    public void setName(String name) {
        this.name = name;
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
    

    
    public synchronized Map getState() {
        byte[] load = this.getPayload();
        try {
            if ( load != null && load.length > 0 ) state = (Map) XByteBuffer.deserialize(load);
        }catch ( Exception x ) {
            throw new RuntimeException(x);
        }
        return (state == null)?new HashMap():state;
    }
    
    public synchronized void setState(Map state) {
        this.state = state;
        stateModified = true;
        try {
            ( (MemberImpl) member).setPayload(XByteBuffer.serialize( (Serializable) state));
        }catch ( Exception x ) {
            throw new RuntimeException(x);
        }
        if ( log.isDebugEnabled() ) log.debug("Setting state to["+getName()+"] state:"+state);
    }
    
    public void setPayload(byte[] payload) {
        cast(member).setPayload(payload);
    }    

    public static MemberImpl cast(Member member) {
        return (MemberImpl)member;
    }    
    
    /**
     * @see java.lang.Object#hashCode()
     * @return The hash code
     */
    public int hashCode() {
        return uniqueId.hashCode();
    }
    
    /**
     * Returns true if the param o is a McastMember with the same name
     * @param o
     */
    public boolean equals(Object o) {
        if ( o instanceof TribesPeer )    {
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
        if ( in.readBoolean() ) name = in.readUTF();
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(uniqueId.getBytes().length);
        out.write(uniqueId.getBytes(),0,uniqueId.getBytes().length);
        out.writeBoolean(name!=null);
        if ( name != null ) out.writeUTF(name);
    }

//    /*===============================
//     */
//    

//    public MemberImpl cast() {
//        return cast(member);
//    }
//    public byte[] getData() { return cast().getData();}
//
//    public byte[] getData(boolean getalive) {
//        return cast().getData(getalive);
//    }
//
//    public int getDataLength() {
//        return cast().getDataLength();
//    }
//
//    public byte[] getData(boolean getalive, boolean reset) {
//        return cast().getData(getalive,reset);
//    }
//
//    public static MemberImpl getMember(byte[] data, MemberImpl member) {
//        return MemberImpl.getMember(data,member);
//    }
//
//    public static MemberImpl getMember(byte[] data) {
//        return MemberImpl.getMember(data);
//    }
//
//
//    public String getHostname() {
//        return cast().getHostname();
//    }
//
//    public long getServiceStartTime() {
//        return cast().getServiceStartTime();
//    }
//
//    public void setMemberAliveTime(long time) {
//        cast().setMemberAliveTime(time);
//    }
//
//    public String toString() {
//        return member.toString();
//    }
//
//
//    public int hashCode() {
//        return member.hashCode();
//    }
//
//    public boolean equals(Object o) {
//        if ( o instanceof MemberImpl ) return o.equals(member);
//        else if ( o instanceof TribesPeer ) return ((TribesPeer)o).member.equals(member);
//        else return false;
//    }
//
//    public void setHost(byte[] host) {
//        cast().setHost(host);
//    }
//
//    public void setHostname(String host) throws IOException {
//        cast().setHostname(host);
//    }
//
//    public void setMsgCount(int msgCount) {
//        cast().setMsgCount(msgCount);
//    }
//
//    public void setPort(int port) {
//        cast().setPort(port);
//    }
//
//    public void setServiceStartTime(long serviceStartTime) {
//        cast().setServiceStartTime(serviceStartTime);
//    }
//
//    public void setUniqueId(byte[] uniqueId) {
//        cast().setUniqueId(uniqueId);
//    }
//
//    
//    public void setDomain(byte[] domain) {
//        cast().setDomain(domain);
//    }
//    
//    
//    public void setCommand(byte[] command) {
//        cast().setCommand(command);
//    }
//    
}