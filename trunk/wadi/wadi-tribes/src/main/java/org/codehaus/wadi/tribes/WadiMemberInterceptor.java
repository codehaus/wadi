package org.codehaus.wadi.tribes;

import java.util.HashMap;

import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.ChannelInterceptorBase;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.group.AbsoluteOrder;
import org.apache.catalina.tribes.membership.MemberImpl;

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
public class WadiMemberInterceptor extends ChannelInterceptorBase {
    
    protected static HashMap map = new HashMap();
    protected int startLevel = 0;

    public void memberAdded(Member member) { 
        TribesPeer peer = wrap(member);
        super.memberAdded(peer);
    }
    
    public void messageReceived(ChannelMessage msg) {
        TribesPeer peer = wrap(msg.getAddress());
        msg.setAddress(peer);
        super.messageReceived(msg);
    }
    
    public void memberDisappeared(Member member) { 
        TribesPeer peer = wrap(member);
        super.memberDisappeared(peer);
    }
    public Member[] getMembers() { 
        Member[] mbrs = super.getMembers();
        if ( mbrs != null ) {
            Member[] peers = new Member[mbrs.length];
            for (int i=0; i<peers.length;i++) peers[i] = wrap(mbrs[i]);
            //add local member to it
            Member[] result = new Member[peers.length+1];
            result[0] = getLocalMember(false);
            System.arraycopy(peers,0,result,1,peers.length);
            return result;
        }
        return mbrs;
    }
    
    public Member getMember(Member mbr) { 
        Member result = super.getMember(mbr);
        if ( result != null ) return wrap(result);
        else return result;
    }
    public Member getLocalMember(boolean incAlive) {
        Member result = wrap(super.getLocalMember(incAlive));
        return result;

    }

    public static TribesPeer wrap(Member mbr) { 
        if ( mbr == null ) return null;
        TribesPeer peer = (TribesPeer)map.get(mbr);
        if ( peer == null ) {
            synchronized (map) {
                peer = (TribesPeer)map.get(mbr);
                if ( peer == null ) {
                    peer = new TribesPeer(mbr);
                    map.put(mbr,peer);
                }
            }
        }
        return peer;
    }
    
    public void start(int svc) throws ChannelException {
        super.start(svc);
        if ( (svc&Channel.SND_RX_SEQ)==Channel.SND_RX_SEQ ) memberAdded(getLocalMember(true));
        startLevel = startLevel | svc;
    }
    public void stop(int svc) throws ChannelException {
        startLevel = (startLevel & (~svc));
        super.stop(svc);
        if ( startLevel == 0 ) map.clear();
    }

}