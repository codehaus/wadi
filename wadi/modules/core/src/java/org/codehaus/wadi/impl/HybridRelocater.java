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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

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
import org.codehaus.wadi.dindex.messages.RelocationAcknowledgement;
import org.codehaus.wadi.dindex.messages.RelocationRequest;
import org.codehaus.wadi.dindex.messages.RelocationResponse;
import org.codehaus.wadi.gridstate.Dispatcher;

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
    protected final long _requestHandOverTimeout=2000;// TODO - parameterise
    protected final long _resTimeout;
    protected final long _ackTimeout;
    protected final boolean _sessionOrRequestPreferred; // true if relocation of session is preferred to relocation of request
    protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

    public HybridRelocater(long resTimeout, long ackTimeout, boolean sessionOrRequestPreferred) {
        _resTimeout=resTimeout;
        _ackTimeout=ackTimeout;
        _sessionOrRequestPreferred=sessionOrRequestPreferred;
    }

    protected SynchronizedBoolean _shuttingDown;
    protected Dispatcher _dispatcher;
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
        _dispatcher.register(RelocationResponse.class, _resTimeout);
        _dispatcher.register(RelocationAcknowledgement.class, _ackTimeout);
    }

    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock) throws IOException, ServletException {
    	String sessionName=name;
    	String nodeName=_config.getNodeName();
    	boolean shuttingDown=_shuttingDown.get();
    	int concurrentRequestThreads=1;
    	RelocationResponse response=null;
    	ObjectMessage message2=null;

    	boolean useGridState=false;

    	if (useGridState) {
    		Motable immotable=null;
    		try {
    			immotable=_config.getDIndex().relocate2(sessionName, nodeName, concurrentRequestThreads, shuttingDown, _resTimeout);
    		} catch (Exception e) {
    			_log.error("unexpected error", e);
    		}

    		if (null==immotable) {
    			return false;
    		} else {
    			boolean answer=immoter.contextualise(hreq, hres, chain, name, immotable, motionLock);
    			return answer;
    		}
    	} else {

    		try {
    			message2=_config.getDIndex().relocate(sessionName, nodeName, concurrentRequestThreads, shuttingDown, _resTimeout);
    			if (message2==null || (response=(RelocationResponse)message2.getObject())==null)
    				return false;
    		} catch (Exception e) {
    			_log.warn("problem arranging relocation", e);
    		}

    		Motable emotable=response.getMotable();
    		if (emotable!=null) {
    			// relocate session...
    			if (!emotable.checkTimeframe(System.currentTimeMillis()))
    				if (_log.isWarnEnabled()) _log.warn("immigrating session has come from the future!: "+emotable.getName());

    			Emoter emoter=new RelocationEmoter(response.getNodeName(), message2);
    			Motable immotable=Utils.mote(emoter, immoter, emotable, name);
    			if (null==immotable)
    				return false;
    			else {
    				boolean answer=immoter.contextualise(hreq, hres, chain, name, immotable, motionLock);
    				return answer;
    			}

    		}

    		InetSocketAddress address=response.getAddress();
    		if (address!=null) {
    			// relocate request...
    			try {

    				//FIXME - API should not be in terms of HttpProxy but in terms of RequestRelocater...

    				_httpProxy.proxy(address, hreq, hres);
    				_log.trace("PROXY WAS SUCCESSFUL");
    				motionLock.release();
    				return true;
    			} catch (Exception e) {
    				_log.error("problem proxying request", e);
    				return false;
    			}
    		}

    		// if we are still here - session could not be found
    		if (_log.isWarnEnabled()) _log.warn("session not found: " + sessionName);
    		return false;
    	}
    }

    /* We send a RelocationRequest out to fetch a Session. We receive a RelocationResponse containing the Session. We pass a RelocationEmoter
     * down the Contextualiser stack. It passes the incoming Session out to the relevant Contextualiser and sends a RelocationAcknowledgment
     * back to the src of the RelocationResponse. (could be done in a Motable like Immoter?)
     */

    class RelocationEmoter extends AbstractChainedEmoter {
        protected final Log _log=LogFactory.getLog(getClass());

        protected final String _nodeName;
        protected final ObjectMessage _message;

        public RelocationEmoter(String nodeName, ObjectMessage message) {
            _nodeName=nodeName;
            _message=message;
        }

        public boolean prepare(String name, Motable emotable, Motable immotable) {
        	try {
        		immotable.copy(emotable);
        	} catch (Exception e) {
		  _log.warn(e);
        		return false;
        	}
            _config.notifySessionRelocation(name);

            // TODO - move some of this to prepare()...
            if (_log.isTraceEnabled()) _log.trace("sending RelocationAcknowledgement");
            RelocationAcknowledgement ack=new RelocationAcknowledgement();//(name, _config.getLocation());
            if (!_config.getDispatcher().reply(_message, ack)) {
                if (_log.isErrorEnabled()) _log.error("could not send RelocationAcknowledgement: "+name);
                return false;
            }

            return true;
        }

        public void commit(String name, Motable emotable) {
        	try {
            emotable.destroy(); // remove copy in store
        	} catch (Exception e) {
        		throw new UnsupportedOperationException("NYI"); // NYI
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
        if (_log.isTraceEnabled()) _log.trace("RelocationRequest received from " + request.getNodeName() + " for " + request.getSessionName() + " on " + _nodeName);
        // both of these may be out of date immediately... :-(
        boolean theyAreShuttingDown=request.getShuttingDown();
        boolean weAreShuttingDown=_shuttingDown.get();
        boolean sessionOrRequestPreferred=_sessionOrRequestPreferred;

        if (!theyAreShuttingDown && (weAreShuttingDown || sessionOrRequestPreferred)) {
            relocateSessionToThem(om, request.getSessionName(), request.getNodeName());
            return;
        }

        if (!weAreShuttingDown && (theyAreShuttingDown || !sessionOrRequestPreferred)) {
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

        Sync invocationLock=_config.getCollapser().getLock(sessionName);
        boolean invocationLockAcquired=false;
        try {
	  if (_lockLog.isTraceEnabled()) _lockLog.trace("Invocation - acquiring: "+sessionName+ " ["+Thread.currentThread().getName()+"]"+" : "+invocationLock);
            Utils.acquireUninterrupted(invocationLock);
	    if (_lockLog.isTraceEnabled()) _lockLog.trace("Invocation - acquired: "+sessionName+ " ["+Thread.currentThread().getName()+"]"+" : "+invocationLock);
            invocationLockAcquired=true;
        } catch (TimeoutException e) {
	  if (_lockLog.isTraceEnabled()) _lockLog.trace("Invocation - not acquired: "+sessionName+ " ["+Thread.currentThread().getName()+"]"+" : "+invocationLock);
            if (_log.isErrorEnabled()) _log.error("exclusive access could not be guaranteed within timeframe: "+sessionName, e);
            return;
        }

        try {
            // reverse direction...
            Immoter promoter=new RelocationImmoter(nodeName, om);
            RankedRWLock.setPriority(RankedRWLock.EMIGRATION_PRIORITY);
            boolean found=_contextualiser.contextualise(null,null,null,sessionName, promoter, invocationLock, true); // if we own session, this will send the correct response...
            if (found)
                invocationLockAcquired=false; // someone else has released the promotion lock...
        } catch (Exception e) {
        	if (_log.isWarnEnabled()) _log.warn("problem handling relocation request: "+sessionName, e);
        } finally {
        	RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
        	if (invocationLockAcquired) {
        		if (_lockLog.isTraceEnabled()) _lockLog.trace("Invocation - releasing: "+sessionName+ " ["+Thread.currentThread().getName()+"]"+" : "+invocationLock);
        		invocationLock.release();
        		if (_lockLog.isTraceEnabled()) _lockLog.trace("Invocation - released: "+sessionName+ " ["+Thread.currentThread().getName()+"]"+" : "+invocationLock);
        	}
        }
        // N.B. - I don't think it is necessary to acquire the motionLock - consider...
        // TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
    }

    class ClusterEmotable extends AbstractMotable {

    	protected final String _name;
        protected final String _tgtNodeName;
        protected ObjectMessage _message;

        public ClusterEmotable(String name, String nodeName, ObjectMessage message) {
        	_name=name;
            _tgtNodeName=nodeName;
            _message=message;
        }
        public byte[] getBodyAsByteArray() throws Exception {
        	throw new UnsupportedOperationException();
        }

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            // send the message
            if (_log.isTraceEnabled()) _log.trace("sending RelocationResponse");
            Motable immotable=new SimpleMotable();
            immotable.setBodyAsByteArray(bytes);
            RelocationResponse response=new RelocationResponse(_name, _nodeName, immotable);
            ObjectMessage message=_dispatcher.exchangeReply(_message, response, _ackTimeout);
            RelocationAcknowledgement ack=null;
            ack=message==null?null:(RelocationAcknowledgement)message.getObject();

            if (ack==null) {
                if (_log.isWarnEnabled()) _log.warn("no ack received for session RelocationResponse"); // TODO - increment a counter somewhere...
                // TODO - who owns the session now - consider a syn link to old owner to negotiate this..
                throw new Exception("no ack received for session RelocationResponse");
            }
            if (_log.isTraceEnabled()) _log.trace("received relocation ack");
        }

        public ByteBuffer getBodyAsByteBuffer() throws Exception {
        	throw new UnsupportedOperationException();
        }

        public void setBodyAsByteBuffer(ByteBuffer body) throws Exception {
        	throw new UnsupportedOperationException();
        }
    }


    /**
     * We receive a RelocationRequest and pass a RelocationImmoter down the Contextualiser stack. The Session is passed to us
     * through the Immoter and we pass it back to the Request-ing node...
     *
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision$
     */
    class RelocationImmoter implements Immoter {
        protected final Log _log=LogFactory.getLog(getClass());

        protected final String _tgtNodeName;
        protected ObjectMessage _message;

        public RelocationImmoter(String nodeName, ObjectMessage message) {
            _tgtNodeName=nodeName;
            _message=message;
        }

        public Motable nextMotable(String name, Motable emotable) {
            return new ClusterEmotable(name, _tgtNodeName, _message);
        }

        public boolean prepare(String name, Motable emotable, Motable immotable) {
        	// work is done in ClusterEmotable...
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
            String src=_config.getDIndex().getNodeName(om.getJMSReplyTo());
            if (_log.isTraceEnabled()) _log.trace("arranging for request to be relocated - sending response to: " + src);
            RelocationResponse response=new RelocationResponse(sessionName, _nodeName, _config.getHttpAddress());
            _config.getDispatcher().reply(om, response);
        } catch (JMSException e) {
            if (_log.isErrorEnabled()) _log.error("could not send RelocationResponse: "+sessionName, e);
        }
    }

}
