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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.SessionRelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class ImmigrateRelocationStrategy implements SessionRelocationStrategy {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final MessageDispatcher _dispatcher;
	protected final long _timeout;
	protected final Location _location;
	protected final StreamingStrategy _streamer;
	protected final Map _locationMap;
	
	protected final Map _resRvMap=new HashMap();
	protected final Map _ackRvMap=new HashMap();
	
	public ImmigrateRelocationStrategy(MessageDispatcher dispatcher, Location location, long timeout, StreamingStrategy ss, Map locationMap) {
		_dispatcher=dispatcher;		
		_timeout=timeout;
		_location=location;
		_streamer=ss;
		_locationMap=locationMap;
		
		_dispatcher.register(this, "onMessage");
		_dispatcher.register(ImmigrationResponse.class, _resRvMap, _timeout);
		_dispatcher.register(ImmigrationAcknowledgement.class, _ackRvMap, _timeout);
	}
	
	
	class RelocationEmoter extends ChainedEmoter {
		
		protected final Map _locationMap;
		
		public RelocationEmoter(Map locationMap) {
			super();
			_locationMap=locationMap;
		}
		
		public boolean prepare(String id, Motable motable, Motable immotable) {
			return true;
		}
		
		public void commit(String id, Motable emotable) {
			super.commit(id, emotable); // remove copy in store
			//synchronized (_locationMap){_locationMap.put(id, );} // this location no longer valid
		}
		
		public void rollback(String id, Motable motable) {
			super.rollback(id, motable);
			// tricky...
			throw new RuntimeException("NYI");
		}

		public String getInfo() {
			return "immigration relocation";
		}
	}
	
	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
		
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
		_log.info("sending immigration request: "+id+" : "+settingsInOut);
		ImmigrationRequest request=new ImmigrationRequest(id, 3000); // TODO - timeout value
		ImmigrationResponse response=(ImmigrationResponse)_dispatcher.exchangeMessages(id, _resRvMap, request, settingsInOut, _timeout);
		_log.info("received immigration response: "+id+" - "+response);
		// take out session, prepare to promote it...

		Motable emotable=response.getMotable();
		Emoter emoter=new RelocationEmoter(_locationMap);		
		Motable immotable=Utils.mote(emoter, immoter, emotable, id);
		if (immotable!=null) {
			promotionLock.release();
			immoter.contextualise(hreq, hres, chain, id, immotable);
			return true;
		} else {
			return false;
		}
	}
	
	protected Contextualiser _top;
	public void setTop(Contextualiser top){_top=top;}
	public Contextualiser getTop(){return _top;}
	
	class ImmigrationEmoter extends ChainedEmoter {
		
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

			_log.info("sending migration ack: "+id);
			ImmigrationAcknowledgement ack=new ImmigrationAcknowledgement(id, _location);
			try {
				_dispatcher.sendMessage(ack, _settingsInOut);
			} catch (JMSException e) {
				_log.error("could not send migration acknowledgement: "+id, e);
			}
		}
		
		public void rollback(String id, Motable motable) {
		}
		
		public String getInfo() {
			return "immigration";
		}
	}
	
	public void onMessage(ObjectMessage om, ImmigrationRequest request) throws JMSException {
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
				Immoter promoter=new MigrationImmoter(settingsInOut);
		//		boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException;
				//_top.contextualise(null,null,fc,id, null, null, true);
				Sync promotionLock=new NullSync(); // TODO - is this right?...
				_top.contextualise(null,null,null,id, promoter, promotionLock, true);
				} catch (Exception e) {
				_log.warn("problem handling migration request: "+id, e);
			}
			// TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
		}
	}
	
	class MigrationImmoter implements Immoter {
		
		protected final MessageDispatcher.Settings _settingsInOut;
		
		public MigrationImmoter(MessageDispatcher.Settings settingsInOut) {
			_settingsInOut=settingsInOut;
		}
		
		public Motable nextMotable(String id, Motable emotable) {
			return new SimpleMotable();
		}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
			// send the message
			_log.info("sending migration response: "+id+" : "+_settingsInOut);
			ImmigrationResponse mr=new ImmigrationResponse();
			mr.setId(id);
			mr.setMotable(emotable);
			ImmigrationAcknowledgement ack=(ImmigrationAcknowledgement)_dispatcher.exchangeMessages(id, _ackRvMap, mr, _settingsInOut, _timeout);
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

		public void commit(String id, Motable immotable) {
			// do nothing
			}

		public void rollback(String id, Motable immotable) {
			// this probably has to by NYI... - nasty...
		}

		public void contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable) throws IOException, ServletException {
			// does nothing - contextualisation will happen when the session arrives...
		}
		
		public String getInfo() {
			return "immigration";
		}
	}
}
