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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
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
import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Location;
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
	protected final Cluster _cluster;
	protected final StreamingStrategy _ss;
	
	protected final Map _resRvMap=new HashMap();
	protected final Map _ackRvMap=new HashMap();
	
	public MigrateRelocationStrategy(Cluster cluster, MessageDispatcher dispatcher, Location location, long timeout, StreamingStrategy ss) {
		_dispatcher=dispatcher;		
		_timeout=timeout;
		_location=location;
		_cluster=cluster;
		_ss=ss;
		
		_dispatcher.register(this, "onMessage");
		_dispatcher.register(MigrationResponse.class, _resRvMap, _timeout);
		_dispatcher.register(MigrationAcknowledgement.class, _ackRvMap, _timeout);
	}
	
	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
		
		Location location=(Location)locationMap.get(id);
		Destination destination;
		
		if (location==null) {
			_log.info("no cached location - 1->n : "+id);
			destination=_cluster.getDestination();
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
		Context context=promoter.nextContext();
		try {
			// TODO - this sort of stuff should be encapsulated by an AbstractStreamingStrategy...
			// TODO - packet-sniff James stuff and see if we can shrink the number of packets - is it significant?
			
			// consider how/when to send migration notifications... could the ack do it ?
			ObjectInput oi=_ss.getInputStream(new ByteArrayInputStream(response.getBytes()));
			context.readContent(oi);
			oi.close();
		} catch (ClassNotFoundException e) {
			_log.warn("perhaps all app classes are not deployed, or we need to load the class via e.g. jndi ?", e);
			return false;
		} catch (IOException e) {
			_log.warn("problem unmarshalling session", e);
		}
		
		_log.info("sending migration ack: "+id);
		MigrationAcknowledgement ack=new MigrationAcknowledgement();
		try {
			_dispatcher.sendMessage(ack, settingsInOut);
		} catch (JMSException e) {
			_log.error("could not send migration acknowledgement: "+id, e);
			return false;
		}

		// get session out of response and promote...
		_log.info("promoting (from cluster): "+id);			
		if (promoter.prepare(id, context)) {
			promoter.commit(id, context);
			promotionLock.release();
			promoter.contextualise(hreq, hres, chain, id, context);
		} else {
			promoter.rollback(id, context);
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
	
	class MigrationResponseFilterChain
	implements FilterChain
	{
		protected final String _id;
		protected final MessageDispatcher.Settings _settingsInOut;
		protected final long _handOverPeriod;
		MigrationResponseFilterChain(String id, MessageDispatcher.Settings settingsInOut, long handOverPeriod) {
			_id=id;
			_handOverPeriod=handOverPeriod;
			_settingsInOut=settingsInOut;
		}
		
		public void
		doFilter(ServletRequest request, ServletResponse response)
		throws IOException, ServletException
		{
			_log.info("acquiring session lock");
			Context context=null; // TODO - how do we get hold of this Context ? - Yikes...!
			context.getSharedLock().release();
			try {
//				context.getExclusiveLock().attempt(_timeout); // TODO - which timeout ?
//				ByteArrayOutputStream baos=new ByteArrayOutputStream();
//				ObjectOutput oos=_ss.getOutputStream(baos);
//				context.writeContent(oos);
//				oos.flush();
//				oos.close();
//				byte[] bytes=baos.toByteArray();
				byte[] bytes=null;
				_log.info("sending migration response: "+_id+" : "+_settingsInOut);
				MigrationResponse mr=new MigrationResponse(_id, bytes);
				MigrationAcknowledgement ack=(MigrationAcknowledgement)_dispatcher.exchangeMessages(_id, _ackRvMap, mr, _settingsInOut, _timeout);
				if (ack==null) {
					throw new Exception("no ack received for session migration: "+_id);
					// TODO - who owns the session now - consider a syn link to old owner to negotiate this..
				}
				_log.info("received migration ack: "+_id+" : "+_settingsInOut);
				
				// if we got to here we need to consider how to promote the session out of the container and release the exclusive lock
				
			} catch (Exception e) {
				_log.error("problem responding to migration request - session may be lost: "+_id); // TODO - break this down...
			}
		}
	}

	class MigrationContext implements Context {
		protected byte[] _bytes=null;
		
		// Context
		public Sync getSharedLock() {return null;}
		public Sync getExclusiveLock() {return null;}
		// Motable
		public long getExpiryTime() {return 0;}
		// SerializableContent
		public void readContent(java.io.ObjectInput is) throws IOException, ClassNotFoundException {}
		public void writeContent(java.io.ObjectOutput os) throws IOException, ClassNotFoundException {}
		
		// total HACKery... - TODO
		public void setBytes(byte[] bytes){_bytes=bytes;}
		public byte[] getBytes(){return _bytes;}
	}
	
	class MigrationPromoter implements Promoter {
		
		protected final MessageDispatcher.Settings _settingsInOut;
		
		public MigrationPromoter(MessageDispatcher.Settings settingsInOut) {
			_settingsInOut=settingsInOut;
		}
		
		public Context nextContext() {
			// return a message into which the session can be written
			return new MigrationContext();
		}

		public boolean prepare(String id, Context context) {
			_log.info("promoting (to cluster): "+id);
			// send the message
			byte[] bytes=((MigrationContext)context).getBytes();
			_log.info("sending migration response: "+id+" : "+_settingsInOut);
			MigrationResponse mr=new MigrationResponse(id, bytes);
			MigrationAcknowledgement ack=(MigrationAcknowledgement)_dispatcher.exchangeMessages(id, _ackRvMap, mr, _settingsInOut, _timeout);
			if (ack==null) {
				_log.warn("no ack received for session migration: "+id);
				// TODO - who owns the session now - consider a syn link to old owner to negotiate this..
				return false;
			}
			_log.info("received migration ack: "+id+" : "+_settingsInOut);
			
			// if we got to here we need to consider how to promote the session out of the container and release the exclusive lock
			
			return true;
		}

		public void commit(String id, Context context) {
			// do nothing
			}

		public void rollback(String id, Context context) {
			// this probably has to by NYI... - nasty...
		}

		public void contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Context context) throws IOException, ServletException {
			// does nothing - contextualisation will happen when the session arrives...
		}
	}

}
