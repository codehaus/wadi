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
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
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
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RelocaterConfig;
import org.codehaus.wadi.SessionRelocater;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;


// TODO
// It should be possible to merge :
//   ImmigrationRequest-ImmigrationResponse-ImmigrationAcknowledgement
// and
//   EmigrationRequest-EmigrationAcknowledgement
// into e.g.
//   ImmigrationRequest-EmigrationRequest-EmigrationAcknowledgement
//
// This would save a fair amount of code...

/**
 * Relocate the state, bringing it underneath the incoming request - immigration
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MigratingRelocater extends AbstractRelocater implements SessionRelocater {

    protected final Log _log=LogFactory.getLog(getClass());
	protected final long _resTimeout;
	protected final long _ackTimeout;
	protected final Map _resRvMap=new HashMap();
	protected final Map _ackRvMap=new HashMap();

	public MigratingRelocater(long resTimeout, long ackTimeout) {
		_resTimeout=resTimeout;
		_ackTimeout=ackTimeout;
	}

    public void init(RelocaterConfig config) {
        super.init(config);
        Dispatcher dispatcher=_config.getDispatcher();
        dispatcher.register(this, "onMessage", ImmigrationRequest.class);
        dispatcher.register(ImmigrationResponse.class, _resRvMap, _resTimeout);
        dispatcher.register(ImmigrationAcknowledgement.class, _ackRvMap, _ackTimeout);
    }

    protected int _counter;
    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock) throws IOException, ServletException {

        Location location=null;
        Destination destination=_config.getDispatcher().getCluster().getDestination();

        Dispatcher.Settings settingsInOut=new Dispatcher.Settings();
        settingsInOut.from=_config.getLocation().getDestination();
        settingsInOut.to=destination;
        settingsInOut.correlationId=name+"-"+(_counter++)+"-"+_config.getCluster().getLocalNode().getDestination().toString(); // TODO - better correlationId
        if (_log.isTraceEnabled()) _log.trace("sending immigration request: "+settingsInOut.correlationId);
        ImmigrationRequest request=new ImmigrationRequest(name, _resTimeout);
        ImmigrationResponse response=(ImmigrationResponse)_config.getDispatcher().exchangeMessages(request, _resRvMap, settingsInOut, _resTimeout);
        if (_log.isTraceEnabled()) _log.trace("received immigration response: "+settingsInOut.correlationId);
        // take out session, prepare to promote it...

        if (response==null)
            return false;

        Motable emotable=response.getMotable();

        if (!emotable.checkTimeframe(System.currentTimeMillis()))
            if (_log.isWarnEnabled()) _log.warn("immigrating session has come from the future!: "+emotable.getName());

        Emoter emoter=new ImmigrationEmoter(settingsInOut);
        Motable immotable=Utils.mote(emoter, immoter, emotable, name);
        if (null==immotable)
            return false;
        else {
            boolean answer=immoter.contextualise(hreq, hres, chain, name, immotable, motionLock);
            return answer;
        }
    }

	class ImmigrationEmoter extends AbstractChainedEmoter {
		protected final Log _log=LogFactory.getLog(getClass());

		protected Destination _from;
        protected Destination _to;
        protected String _correlationId;
        
		public ImmigrationEmoter(Dispatcher.Settings settingsInOut) {
            _from=settingsInOut.from;
            _to=settingsInOut.to;
            _correlationId=settingsInOut.correlationId;
		}

		public boolean prepare(String name, Motable emotable) {
			return true;
		}

		public void commit(String name, Motable emotable) {
			emotable.destroy(); // remove copy in store

			// TODO - move some of this to prepare()...
			if (_log.isTraceEnabled()) _log.trace("sending immigration ack: "+_correlationId);
			ImmigrationAcknowledgement ack=new ImmigrationAcknowledgement(name, _config.getLocation());
			if (_config.getDispatcher().send(_from, _to, _correlationId, ack)) {
                // needs refactoring...
			} else {
			    if (_log.isErrorEnabled()) _log.error("could not send immigration acknowledgement: "+name);
			}
		}

		public void rollback(String name, Motable motable) {
		    throw new RuntimeException("NYI");
		}

		public String getInfo() {
			return "immigration";
		}
	}

    public void onMessage(ObjectMessage om, ImmigrationRequest request) {
        String id=request.getId();
        if (_log.isTraceEnabled()) _log.trace("receiving immigration request: "+id);
        Contextualiser top=_config.getContextualiser();
        if (top==null) {
            _log.warn("no Contextualiser set - cannot respond to ImmigrationRequests");
        } else {
            Sync motionLock=_config.getCollapser().getLock(id);
            boolean acquired=false;
            try {
                try {
                    Utils.acquireUninterrupted(motionLock);
                    acquired=true;
                } catch (TimeoutException e) {
                    if (_log.isErrorEnabled()) _log.error("exclusive access could not be guaranteed within timeframe: "+id, e);
                    return;
                }

                Dispatcher.Settings settingsInOut=new Dispatcher.Settings();
                // reverse direction...
                settingsInOut.to=om.getJMSReplyTo();
                settingsInOut.from=_config.getLocation().getDestination();
                settingsInOut.correlationId=om.getJMSCorrelationID();
                if (_log.isTraceEnabled()) _log.trace("receiving immigration request: "+settingsInOut.correlationId);
                //				long handShakePeriod=request.getHandOverPeriod();
                // TODO - the peekTimeout should be specified by the remote node...
                Immoter promoter=new ImmigrationImmoter(settingsInOut);

                RankedRWLock.setPriority(RankedRWLock.EMIGRATION_PRIORITY);
		boolean found=top.contextualise(null,null,null,id, promoter, motionLock, true);
		if (found)
		  acquired=false; // someone else has released the promotion lock...
            } catch (Exception e) {
                if (_log.isWarnEnabled()) _log.warn("problem handling immigration request: "+id, e);
            } finally {
                RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
                if (acquired) motionLock.release();
            }
            // TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
        }
    }

	/**
	 * Manage the immotion of a session into the Cluster tier and thence its Emigration
	 * (in response to an ImmigrationRequest) thence to another node.
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class ImmigrationImmoter implements Immoter {
		protected final Log _log=LogFactory.getLog(getClass());

		protected final Dispatcher.Settings _settingsInOut;

		public ImmigrationImmoter(Dispatcher.Settings settingsInOut) {
			_settingsInOut=settingsInOut;
		}

		public Motable nextMotable(String id, Motable emotable) {
			return new SimpleMotable();
		}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			// send the message
			if (_log.isTraceEnabled()) _log.trace("sending immigration response: "+_settingsInOut.correlationId);
			ImmigrationResponse mr=new ImmigrationResponse();
			mr.setId(name);
			try {
			immotable.copy(emotable);
			} catch (Exception e) {
			    _log.warn("unexpected problem", e);
			    return false;
			}
			mr.setMotable(immotable);
			ImmigrationAcknowledgement ack=(ImmigrationAcknowledgement)_config.getDispatcher().exchangeMessages(mr, _ackRvMap, _settingsInOut, _ackTimeout);
			if (ack==null) {
			  if (_log.isWarnEnabled()) _log.warn("no ack received for session immigration: "+_settingsInOut.correlationId); // TODO - increment a couter somewhere...
				// TODO - who owns the session now - consider a syn link to old owner to negotiate this..
				return false;
			}
			if (_log.isTraceEnabled()) _log.trace("received immigration ack: "+_settingsInOut.correlationId);
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
			return "emigration";
		}
	}
}
