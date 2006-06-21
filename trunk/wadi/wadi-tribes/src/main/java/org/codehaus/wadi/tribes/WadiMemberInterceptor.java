package org.codehaus.wadi.tribes;

import java.util.HashMap;

import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.ChannelInterceptorBase;

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
    
    public HashMap map = new HashMap();
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
            Member[] result = new Member[mbrs.length];
            for (int i=0; i<result.length;i++) result[i] = wrap(mbrs[i]);
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
        Member result = super.getLocalMember(incAlive);
        if ( result != null ) return wrap(result);
        else return result;

    }

    public TribesPeer wrap(Member mbr) { 
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
        startLevel = startLevel | svc;
        super.start(svc);
    }
    public void stop(int svc) throws ChannelException {
        startLevel = (startLevel & (~svc));
        super.stop(svc);
        if ( startLevel == 0 ) map.clear();
    }

}