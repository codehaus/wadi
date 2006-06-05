package org.codehaus.wadi.tribes;


import org.codehaus.wadi.group.Address;
import org.apache.catalina.tribes.Member;
import org.codehaus.wadi.group.Peer;
import java.io.Externalizable;
import java.util.Map;
import java.io.ObjectInput;
import java.io.IOException;
import org.apache.catalina.tribes.membership.MemberImpl;
import java.io.ObjectOutput;

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
public class TribesPeer implements Member, Peer, Address, Externalizable {
    
    protected Member member;
    protected Map map = null;
    
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
        return map;
    }
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        member = new MemberImpl();
        ((MemberImpl)member).readExternal(in);
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        ((MemberImpl)member).writeExternal(out);
    }
}