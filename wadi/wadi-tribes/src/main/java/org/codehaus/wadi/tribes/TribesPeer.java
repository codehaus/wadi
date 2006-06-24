package org.codehaus.wadi.tribes;


import org.codehaus.wadi.group.Address;
import org.apache.catalina.tribes.Member;
import java.io.Externalizable;
import java.util.Map;
import java.io.ObjectInput;
import java.io.IOException;
import org.apache.catalina.tribes.membership.MemberImpl;
import java.io.ObjectOutput;
import org.codehaus.wadi.group.LocalPeer;
import org.apache.catalina.tribes.io.XByteBuffer;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.HashMap;

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
public class TribesPeer extends MemberImpl implements Member, LocalPeer, Address, Externalizable {
    
    protected Member member;
    protected Map state = null;
    
    public TribesPeer() {} //for serialization
    public TribesPeer(Member mbr) {
        this.member = mbr;
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
        return member.getName();
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
        return member.getUniqueId();
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
    
    public Map getState() {
        byte[] load = this.getPayload();
        if ( state == null && load!=null && load.length > 0){
            try {
                state = (Map) XByteBuffer.deserialize(load);
            }catch ( Exception x ) {
                throw new RuntimeException(x);
            }
        }
        return (state == null)?new HashMap():state;
    }
    
    public void setState(Map state) {
        this.state = state;
        try {
            ( (MemberImpl) member).setPayload(XByteBuffer.serialize( (Serializable) state));
        }catch ( Exception x ) {
            throw new RuntimeException(x);
        }
    }
    
    protected Object readResolve() throws ObjectStreamException {
        return WadiMemberInterceptor.wrap(this);
    }
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        member = new MemberImpl();
        ((MemberImpl)member).readExternal(in);
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        ((MemberImpl)member).writeExternal(out);
    }
    
    /*===============================
     */
    
    public static MemberImpl cast(Member member) {
        return (MemberImpl)member;
    }
    public MemberImpl cast() {
        return cast(member);
    }
    public byte[] getData() { return cast().getData();}

    public byte[] getData(boolean getalive) {
        return cast().getData(getalive);
    }

    public int getDataLength() {
        return cast().getDataLength();
    }

    public byte[] getData(boolean getalive, boolean reset) {
        return cast().getData(getalive,reset);
    }

    public static MemberImpl getMember(byte[] data, MemberImpl member) {
        return MemberImpl.getMember(data,member);
    }

    public static MemberImpl getMember(byte[] data) {
        return MemberImpl.getMember(data);
    }


    public String getHostname() {
        return cast().getHostname();
    }

    public long getServiceStartTime() {
        return cast().getServiceStartTime();
    }

    public void setMemberAliveTime(long time) {
        cast().setMemberAliveTime(time);
    }

    public String toString() {
        return member.toString();
    }


    public int hashCode() {
        return member.hashCode();
    }

    public boolean equals(Object o) {
        if ( o instanceof MemberImpl ) return o.equals(member);
        else if ( o instanceof TribesPeer ) return ((TribesPeer)o).member.equals(member);
        else return false;
    }

    public void setHost(byte[] host) {
        cast().setHost(host);
    }

    public void setHostname(String host) throws IOException {
        cast().setHostname(host);
    }

    public void setMsgCount(int msgCount) {
        cast().setMsgCount(msgCount);
    }

    public void setPort(int port) {
        cast().setPort(port);
    }

    public void setServiceStartTime(long serviceStartTime) {
        cast().setServiceStartTime(serviceStartTime);
    }

    public void setUniqueId(byte[] uniqueId) {
        cast().setUniqueId(uniqueId);
    }

    public void setPayload(byte[] payload) {
        cast().setPayload(payload);
    }


}