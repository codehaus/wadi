/**
*
* Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.SessionRelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class MigrateRelocationStrategy implements SessionRelocationStrategy {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final MessageDispatcher _dispatcher;
	protected final long _timeout;
	protected final Location _location;
	protected final StreamingStrategy _streamer;
	protected final Map _locationMap;
	
	protected final Map _resRvMap=new HashMap();
	protected final Map _ackRvMap=new HashMap();
	
	public MigrateRelocationStrategy(MessageDispatcher dispatcher, Location location, long timeout, StreamingStrategy ss, Map locationMap) {
		_dispatcher=dispatcher;		
		_timeout=timeout;
		_location=location;
		_streamer=ss;
		_locationMap=locationMap;
		
		_dispatcher.register(this, "onMessage");
		_dispatcher.register(MigrationResponse.class, _resRvMap, _timeout);
		_dispatcher.register(MigrationAcknowledgement.class, _ackRvMap, _timeout);
	}
	
	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
		
		Location location=(Location)locationMap.get(id);
		Destination destination;
		
		if (location==null) {
			_log.info("no cached location - 1->n : "+id);
			destination=_dispatcher.getCluster().getDestination();
		} else {
			_log.info("cached location - 1->1 : "+id);
			destination=location.getDestination();
		}
		
		MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
		settingsInOut.from=_location.getDestination();
		settingsInOut.to=destination;
		settingsInOut.correlationId=id; // TODO - better correlationId
		_log.info("sending migration request: "+id+" : "+settingsInOut);
		MigrationRequest request=new MigrationRequest(id, 3000); // TODO - timeout value
		MigrationResponse response=(MigrationResponse)_dispatcher.exchangeMessages(id, _resRvMap, request, settingsInOut, _timeout);
		_log.info("received migration response: "+id+" - "+response);
		// take out session, prepare to promote it...
		Motable p=promoter.nextMotable();
		try {
			// TODO - this sort of stuff should be encapsulated by an AbstractStreamingStrategy...
			// TODO - packet-sniff James stuff and see if we can shrink the number of packets - is it significant?
			
			// consider how/when to send migration notifications... could the ack do it ?
			p.setBytes(response.getBytes());
		} catch (Exception e) {
			_log.warn("problem promoting session", e);
			return false;
		}		
		_log.info("sending migration ack: "+id);
		MigrationAcknowledgement ack=new MigrationAcknowledgement(id, _location);
		try {
			_dispatcher.sendMessage(ack, settingsInOut);
		} catch (JMSException e) {
			_log.error("could not send migration acknowledgement: "+id, e);
			return false;
		}

		// get session out of response and promote...
		_log.info("promoting (from cluster): "+id);			
		if (promoter.prepare(id, p)) {
			_locationMap.remove(id); // evict old location from cache
			promoter.commit(id, p);
			promotionLock.release();
			promoter.contextualise(hreq, hres, chain, id, p);
		} else {
			promoter.rollback(id, p);
			return false;
		}
				
		return true;
	}
	
	protected Contextualiser _top;
	public void setTop(Contextualiser top){_top=top;}
	public Contextualiser getTop(){return _top;}
	
	public void onMessage(ObjectMessage om, MigrationRequest request) throws JMSException {
		String id=request.getId();
		_log.info("receiving migration request: "+id);
		if (_top==null) {
			_log.warn("no Contextualiser set - cannot respond to MigrationRequests");
		} else {
			try {
				MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
				// reverse direction...
				settingsInOut.to=om.getJMSReplyTo();
				settingsInOut.from=_location.getDestination();
				settingsInOut.correlationId=om.getJMSCorrelationID();
				_log.info("receiving migration request: "+id+" : "+settingsInOut);
//				long handShakePeriod=request.getHandOverPeriod();
				// TODO - the peekTimeout should be specified by the remote node...
				//FilterChain fc=new MigrationResponseFilterChain(id, settingsInOut, handShakePeriod);
				Promoter promoter=new MigrationPromoter(settingsInOut);
		//		boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException;
				//_top.contextualise(null,null,fc,id, null, null, true);
				Sync promotionLock=new Mutex(); // TODO - we need a solution...
				_top.contextualise(null,null,null,id, promoter, promotionLock, false);
				} catch (Exception e) {
				_log.warn("problem handling migration request: "+id, e);
			}
			// TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
		}
	}
	
	class MigrationPromoter implements Promoter {
		
		protected final MessageDispatcher.Settings _settingsInOut;
		
		public MigrationPromoter(MessageDispatcher.Settings settingsInOut) {
			_settingsInOut=settingsInOut;
		}
		
		public Motable nextMotable() {
			// return a message into which the session can be written
			return new MigrationResponse();
		}

		public boolean prepare(String id, Motable motable) {
			
			_log.info("promoting (to cluster): "+id);
			// send the message
			_log.info("sending migration response: "+id+" : "+_settingsInOut);
			MigrationResponse mr=(MigrationResponse)motable;
			mr.setId(id);
			MigrationAcknowledgement ack=(MigrationAcknowledgement)_dispatcher.exchangeMessages(id, _ackRvMap, mr, _settingsInOut, _timeout);
			if (ack==null) {
				_log.warn("no ack received for session migration: "+id);
				// TODO - who owns the session now - consider a syn link to old owner to negotiate this..
				return false;
			}
			_log.info("received migration ack: "+id+" : "+_settingsInOut);
			// update location cache...
			Location tmp=ack.getLocation();
			synchronized (_locationMap) {
				_locationMap.put(id, tmp);
			}
			
			return true;
		}

		public void commit(String id, Motable motable) {
			// do nothing
			}

		public void rollback(String id, Motable motable) {
			// this probably has to by NYI... - nasty...
		}

		public void contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Motable motable) throws IOException, ServletException {
			// does nothing - contextualisation will happen when the session arrives...
		}
	}
}
