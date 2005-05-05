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
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
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
public class ImmigrateRelocater implements SessionRelocater {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final MessageDispatcher _dispatcher;
	protected final long _resTimeout;
	protected final long _ackTimeout;
	protected final Location _location;
	protected final Map _locationMap;

	protected final Map _resRvMap=new HashMap();
	protected final Map _ackRvMap=new HashMap();

    protected final Collapser _collapser;

	public ImmigrateRelocater(MessageDispatcher dispatcher, Location location, long timeout, Map locationMap, Collapser collapser) {
		_dispatcher=dispatcher;
		_resTimeout=timeout;
		_ackTimeout=500;
		_location=location;
		_locationMap=locationMap;
        _collapser=collapser;

		_dispatcher.register(this, "onMessage");
		_dispatcher.register(ImmigrationResponse.class, _resRvMap, _resTimeout);
		_dispatcher.register(ImmigrationAcknowledgement.class, _ackRvMap, _ackTimeout);
	}

    protected int _counter;
    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, Map locationMap) throws IOException, ServletException {

        Location location=(Location)locationMap.get(id);
        location=null;
        Destination destination;

        if (location==null) {
            if (_log.isTraceEnabled()) _log.trace("immigration: no cached location - 1->n : "+id);
            destination=_dispatcher.getCluster().getDestination();
        } else {
            if (_log.isTraceEnabled()) _log.trace("immigration: cached location - 1->1 : "+id);
            destination=location.getDestination();
        }

        MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
        settingsInOut.from=_location.getDestination();
        settingsInOut.to=destination;
        settingsInOut.correlationId=id+"-"+(_counter++)+"-"+_dispatcher._cluster.getLocalNode().getDestination().toString(); // TODO - better correlationId
        if (_log.isTraceEnabled()) _log.trace("sending immigration request: "+settingsInOut.correlationId);
        ImmigrationRequest request=new ImmigrationRequest(id, _resTimeout);
        ImmigrationResponse response=(ImmigrationResponse)_dispatcher.exchangeMessages(id, _resRvMap, request, settingsInOut, _resTimeout);
        if (_log.isTraceEnabled()) _log.trace("received immigration response: "+settingsInOut.correlationId);
        // take out session, prepare to promote it...

        if (response==null)
            return false;

        Motable emotable=response.getMotable();

        if (!emotable.checkTimeframe(System.currentTimeMillis()))
            if (_log.isWarnEnabled()) _log.warn("immigrating session has come from the future!: "+emotable.getId());

        Emoter emoter=new ImmigrationEmoter(_locationMap, settingsInOut);
        Motable immotable=Utils.mote(emoter, immoter, emotable, id);
        if (null==immotable)
            return false;
        else {
            boolean answer=immoter.contextualise(hreq, hres, chain, id, immotable, motionLock);
            return answer;
        }
    }

	protected Contextualiser _top;
	public void setTop(Contextualiser top){_top=top;}
	public Contextualiser getTop(){return _top;}

	class ImmigrationEmoter extends AbstractChainedEmoter {
		protected final Log _log=LogFactory.getLog(getClass());

		protected final Map _locationMap;
		protected MessageDispatcher.Settings _settingsInOut;

		public ImmigrationEmoter(Map locationMap, MessageDispatcher.Settings settingsInOut) {
			_locationMap=locationMap;
			_settingsInOut=settingsInOut;
		}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
			return true;
		}

		public void commit(String id, Motable emotable) {
			emotable.tidy(); // remove copy in store

			// TODO - move some of this to prepare()...
			if (_log.isTraceEnabled()) _log.trace("sending immigration ack: "+_settingsInOut.correlationId);
			ImmigrationAcknowledgement ack=new ImmigrationAcknowledgement(id, _location);
			try {
				_dispatcher.sendMessage(ack, _settingsInOut);
				_locationMap.remove(id);
			} catch (JMSException e) {
				if (_log.isErrorEnabled()) _log.error("could not send immigration acknowledgement: "+id, e);
			}
		}

		public void rollback(String id, Motable motable) {
		    throw new RuntimeException("NYI");
		}

		public String getInfo() {
			return "immigration";
		}
	}

    public void onMessage(ObjectMessage om, ImmigrationRequest request) {
        String id=request.getId();
        if (_log.isTraceEnabled()) _log.trace("receiving immigration request: "+id);
        if (_top==null) {
            _log.warn("no Contextualiser set - cannot respond to ImmigrationRequests");
        } else {
            Sync motionLock=_collapser.getLock(id);
            boolean acquired=false;
            try {
                try {
                    Utils.acquireUninterrupted(motionLock);
                    acquired=true;
                } catch (TimeoutException e) {
                    if (_log.isErrorEnabled()) _log.error("exclusive access could not be guaranteed within timeframe: "+id, e);
                    return;
                }

                MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
                // reverse direction...
                settingsInOut.to=om.getJMSReplyTo();
                settingsInOut.from=_location.getDestination();
                settingsInOut.correlationId=om.getJMSCorrelationID();
                if (_log.isTraceEnabled()) _log.trace("receiving immigration request: "+settingsInOut.correlationId);
                //				long handShakePeriod=request.getHandOverPeriod();
                // TODO - the peekTimeout should be specified by the remote node...
                Immoter promoter=new ImmigrationImmoter(settingsInOut);

                RankedRWLock.setPriority(RankedRWLock.EMMIGRATION_PRIORITY);
		boolean found=_top.contextualise(null,null,null,id, promoter, motionLock, true);
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

		protected final MessageDispatcher.Settings _settingsInOut;

		public ImmigrationImmoter(MessageDispatcher.Settings settingsInOut) {
			_settingsInOut=settingsInOut;
		}

		public Motable nextMotable(String id, Motable emotable) {
			return new SimpleMotable();
		}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
			// send the message
			if (_log.isTraceEnabled()) _log.trace("sending immigration response: "+_settingsInOut.correlationId);
			ImmigrationResponse mr=new ImmigrationResponse();
			mr.setId(id);
			try {
			immotable.copy(emotable);
			} catch (Exception e) {
			    _log.warn("unexpected problem", e);
			    return false;
			}
			mr.setMotable(immotable);
			ImmigrationAcknowledgement ack=(ImmigrationAcknowledgement)_dispatcher.exchangeMessages(id, _ackRvMap, mr, _settingsInOut, _ackTimeout);
			if (ack==null) {
			  if (_log.isWarnEnabled()) _log.warn("no ack received for session immigration: "+_settingsInOut.correlationId); // TODO - increment a couter somewhere...
				// TODO - who owns the session now - consider a syn link to old owner to negotiate this..
				return false;
			}
			if (_log.isTraceEnabled()) _log.trace("received immigration ack: "+_settingsInOut.correlationId);
			// update location cache...
			Location tmp=ack.getLocation();
			synchronized (_locationMap) {
				_locationMap.put(id, tmp);
			}

			return true;
		}

		public void commit(String id, Motable immotable) {
			// do nothing
			}

		public void rollback(String id, Motable immotable) {
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
