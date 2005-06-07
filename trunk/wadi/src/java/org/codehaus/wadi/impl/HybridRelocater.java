/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RelocaterConfig;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * Combine various RelocationStrategies to produce a cleverer one
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HybridRelocater extends AbstractRelocater {
    
    protected final Log _log=LogFactory.getLog(getClass());
    protected final Map _ackRvMap=Collections.synchronizedMap(new HashMap());
    protected final Map _resRvMap=Collections.synchronizedMap(new HashMap());
    protected final long _requestHandOverTimeout=2000;// TODO - parameterise
    protected final long _resTimeout;
    protected final long _ackTimeout;
    protected final boolean _sessionOrRequestPreferred;
    
    public HybridRelocater(long resTimeout, long ackTimeout, boolean sessionOrRequestPreferred) {
        _resTimeout=resTimeout;
        _ackTimeout=ackTimeout;
        _sessionOrRequestPreferred=sessionOrRequestPreferred;
    }
    
    protected SynchronizedBoolean _shuttingDown;
    protected MessageDispatcher _dispatcher;
    protected String _nodeName;
    protected Contextualiser _contextualiser;
    protected HttpProxy _httpProxy;
    
    public void init(RelocaterConfig config) {
        super.init(config);
        _shuttingDown=_config.getShuttingDown();
        _dispatcher=_config.getDispatcher();
        _nodeName=_config.getNodeName();
        _contextualiser=_config.getContextualiser();
        _httpProxy=_config.getHttpProxy();
        _dispatcher.register(this, "onMessage", RelocationRequest.class);
        _dispatcher.register(RelocationResponse.class, _resRvMap, _resTimeout);
        _dispatcher.register(RelocationAcknowledgement.class, _ackRvMap, _ackTimeout);
    }
    
    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock, Map locationMap) throws IOException, ServletException {
        if (_log.isTraceEnabled()) _log.trace("sending RelocationRequest from "+_nodeName+" for "+name);
        String sessionName=name;
        String nodeName=_config.getNodeName();
        boolean sessionOrRequestPreferred=getSessionOrRequestPreferred();
        boolean shuttingDown=_shuttingDown.get();
        long lastKnownTime=0L;
        String lastKnownPlace=null;
        MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
        settingsInOut.from=_config.getLocation().getDestination();
        settingsInOut.to=_config.getDispatcher().getCluster().getDestination();
        settingsInOut.correlationId=nodeName+"-"+sessionName+"-"+System.currentTimeMillis();
        RelocationRequest request=new RelocationRequest(sessionName, nodeName, sessionOrRequestPreferred, shuttingDown, lastKnownTime, lastKnownPlace, _requestHandOverTimeout);
        RelocationResponse response=(RelocationResponse)_config.getDispatcher().exchangeMessages(request, _resRvMap, settingsInOut, _resTimeout);
        
        if (response==null)
            return false;
        
        Motable emotable=response.getMotable();
        if (emotable==null) {
            // relocate request...
            try {
                
                //FIXME - API should not be in terms of HttpProxy but in terms of RequestRelocater...
                
                _httpProxy.proxy(response.getAddress(), hreq, hres);
                return true;
            } catch (Exception e) {
                _log.error("problem proxying request", e);
                return false;
            }
        } else {
            // relocate session...
            if (!emotable.checkTimeframe(System.currentTimeMillis()))
                if (_log.isWarnEnabled()) _log.warn("immigrating session has come from the future!: "+emotable.getName());
            
            Emoter emoter=new RelocationAcknowledgementEmoter(response.getNodeName(), _config.getMap(), settingsInOut);
            Motable immotable=Utils.mote(emoter, immoter, emotable, name);
            if (null==immotable)
                return false;
            else {
                boolean answer=immoter.contextualise(hreq, hres, chain, name, immotable, motionLock);
                return answer;
            }
        }

    }
    
    class RelocationAcknowledgementEmoter extends AbstractChainedEmoter {
        protected final Log _log=LogFactory.getLog(getClass());
        
        protected final String _nodeName;
        protected final Map _map;
        protected MessageDispatcher.Settings _settingsInOut;
        
        public RelocationAcknowledgementEmoter(String nodeName, Map map, MessageDispatcher.Settings settingsInOut) {
            _nodeName=nodeName;
            _map=map;
            _settingsInOut=settingsInOut;
        }
        
        public boolean prepare(String name, Motable emotable) {
            return true;
        }
        
        public void commit(String name, Motable emotable) {
            emotable.destroy(); // remove copy in store
            
            // TODO - move some of this to prepare()...
            if (_log.isTraceEnabled()) _log.trace("sending RelocationAcknowledgement: "+_settingsInOut.correlationId);
            RelocationAcknowledgement ack=new RelocationAcknowledgement();//(name, _config.getLocation());
            try {
                _config.getDispatcher().sendMessage(ack, _settingsInOut);
                _config.getMap().remove(name);
            } catch (JMSException e) {
                if (_log.isErrorEnabled()) _log.error("could not send RelocationAcknowledgement: "+name, e);
            }
        }
        
        public void rollback(String name, Motable motable) {
            throw new RuntimeException("NYI");
        }
        
        public String getInfo() {
            return "immigration:"+_nodeName;
        }
    }
    
    boolean getSessionOrRequestPreferred() {
        return _sessionOrRequestPreferred;
        // check out LB's capabilities during init()....
    }
    
    // the request arrives ...
    
    public void onMessage(ObjectMessage om, RelocationRequest request) {
        _log.trace("RelocationRequest received from "+request.getNodeName()+" for "+request.getSessionName()+" on "+_nodeName);
        // both of these may be out of date immediately... :-(
        boolean theyAreShuttingDown=request.getShuttingDown();
        boolean weAreShuttingDown=_shuttingDown.get();
        boolean sessionOrRequestPreferred=request.getSessionOrRequestPreferred();
        
        if (!theyAreShuttingDown &&
                (weAreShuttingDown || sessionOrRequestPreferred==RelocationRequest._RELOCATE_SESSION_PREFERRED)) {
            relocateSessionToThem(om, request.getSessionName(), request.getNodeName());
            return;
        }
        
        if (!weAreShuttingDown &&
                (theyAreShuttingDown || sessionOrRequestPreferred==RelocationRequest._RELOCATE_REQUEST_PREFERRED)) {
            relocateRequestToUs(om, request.getSessionName());
            return;
        }
        
        if (weAreShuttingDown && theyAreShuttingDown) {
            // yikes !
            // we need to relocate both session and request to a third, safe node
            // think about it....
            throw new UnsupportedOperationException("both source and target node are shutting down");
        }
    }
    
    // response is to relocate session back to sender...
    
    protected void relocateSessionToThem(ObjectMessage om, String sessionName, String nodeName) {
        if (_log.isTraceEnabled()) _log.trace("relocating "+sessionName+" from "+_nodeName+" to "+nodeName);
        
        Sync motionLock=_config.getCollapser().getLock(sessionName);
        boolean acquired=false;
        try {
            Utils.acquireUninterrupted(motionLock);
            acquired=true;
        } catch (TimeoutException e) {
            if (_log.isErrorEnabled()) _log.error("exclusive access could not be guaranteed within timeframe: "+sessionName, e);
            return;
        }
        
        try {
            MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
            // reverse direction...
            settingsInOut.to=om.getJMSReplyTo();
            settingsInOut.from=_config.getLocation().getDestination();
            settingsInOut.correlationId=om.getJMSCorrelationID();
            Immoter promoter=new RelocationResponseImmoter(nodeName, settingsInOut);
            RankedRWLock.setPriority(RankedRWLock.EMIGRATION_PRIORITY);
            boolean found=_contextualiser.contextualise(null,null,null,sessionName, promoter, motionLock, true); // if we own session, this will send the correct response...
            if (found)
                acquired=false; // someone else has released the promotion lock...
        } catch (Exception e) {
            if (_log.isWarnEnabled()) _log.warn("problem handling relocation request: "+sessionName, e);
        } finally {
            RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
            if (acquired) motionLock.release();
        }
        // N.B. - I don't think it is necessary to acquire the motionLock - consider...
        // TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
    }
    
    /**
     * Manage the immotion of a session into the Cluster tier and thence its Emigration
     * (in response to an RelocationRequest) thence to another node.
     *
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision$
     */
    class RelocationResponseImmoter implements Immoter {
        protected final Log _log=LogFactory.getLog(getClass());
        
        protected final String _tgtNodeName;
        protected final MessageDispatcher.Settings _settingsInOut;
        
        public RelocationResponseImmoter(String nodeName, MessageDispatcher.Settings settingsInOut) {
            _tgtNodeName=nodeName;
            _settingsInOut=settingsInOut;
        }
        
        public Motable nextMotable(String name, Motable emotable) {
            return new SimpleMotable();
        }
        
        public boolean prepare(String name, Motable emotable, Motable immotable) {
            // send the message
            if (_log.isTraceEnabled()) _log.trace("sending RelocationResponse: "+_settingsInOut.correlationId);
            try {
                immotable.copy(emotable);
            } catch (Exception e) {
                _log.warn("unexpected problem", e);
                return false;
            }
            RelocationResponse response=new RelocationResponse(name, _nodeName, immotable);
            RelocationAcknowledgement ack=(RelocationAcknowledgement)_dispatcher.exchangeMessages(response, _ackRvMap, _settingsInOut, _ackTimeout);
            if (ack==null) {
                if (_log.isWarnEnabled()) _log.warn("no ack received for session RelocationResponse: "+_settingsInOut.correlationId); // TODO - increment a counter somewhere...
                // TODO - who owns the session now - consider a syn link to old owner to negotiate this..
                return false;
            }
            if (_log.isTraceEnabled()) _log.trace("received relocation ack: "+_settingsInOut.correlationId);
            
            // update location cache...
//          Location tmp=ack.getLocation();
//          synchronized (_config.getMap()) {
//          _config.getMap().put(name, tmp);
//          }
            
            return true;
        }
        
        public void commit(String name, Motable immotable) {
            // do nothing
        }
        
        public void rollback(String name, Motable immotable) {
            // this probably has to by NYI... - nasty...
        }
        
        public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable, Sync motionLock) {
            return false;
        }
        
        public String getInfo() {
            return "emigration:"+_tgtNodeName;
        }
    }
    
    // response is to relocate http request from sender to us...
    
    protected void relocateRequestToUs(ObjectMessage om, String sessionName) {
        try {
            //MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
            // reverse direction...
//            settingsInOut.to=om.getJMSReplyTo();
//            settingsInOut.from=_config.getLocation().getDestination();
//            settingsInOut.correlationId=om.getJMSCorrelationID();
            RelocationResponse response=new RelocationResponse(sessionName, _nodeName, _config.getHttpAddress());
//            _config.getDispatcher().sendMessage(response, settingsInOut);
            _config.getDispatcher().replyToMessage(om, response);
//            _config.getMap().remove(name);
        } catch (JMSException e) {
            if (_log.isErrorEnabled()) _log.error("could not send RelocationResponse: "+sessionName, e);
        }

    }
}
