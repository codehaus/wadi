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
import java.util.Collections;
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
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.SessionRelocationStrategy;

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

	protected final Map _resRvMap=new HashMap();
	protected final Map _ackRvMap=new HashMap();

	public MigrateRelocationStrategy(MessageDispatcher dispatcher, long timeout, Location location, Cluster cluster) {
		_dispatcher=dispatcher;		
		_timeout=timeout;
		_location=location;
		_cluster=cluster;
		
		_dispatcher.register(this, "onMessage");
		_dispatcher.register(MigrationResponse.class, _resRvMap, _timeout);
		_dispatcher.register(MigrationAcknowledgement.class, _ackRvMap, _timeout);
	}

	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {

		Location location=(Location)locationMap.get(id);
		Destination destination;
		
		if (location==null) {
			_log.info("cached location - 1->1 : "+id);
			destination=_cluster.getDestination();
		} else {
			_log.info("no cached location - 1->n : "+id);
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
		_log.info("sending migration ack: "+id);
		MigrationAcknowledgement ack=new MigrationAcknowledgement();
		try {
			_dispatcher.sendMessage(ack, settingsInOut);
			// promote session here...
		} catch (JMSException e) {
			_log.error("could not send migration acknowledgement: "+id, e);
		}
		// get session out of response and promote...
		
		return false;
	}

	protected Contextualiser _top;
	public void setTop(Contextualiser top){_top=top;}

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
				long handShakePeriod=request.getHandOverPeriod();
				// TODO - the peekTimeout should be specified by the remote node...
				FilterChain fc=new MigrationResponseFilterChain(id, settingsInOut, handShakePeriod);
				_top.contextualise(null,null,fc,id, null, null, true);
			} catch (Exception e) {
				_log.warn("problem handling migration request: "+id);
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
			_log.info("sending migration response: "+_id+" : "+_settingsInOut);
			MigrationResponse mr=new MigrationResponse(Collections.singleton(_id));
			MigrationAcknowledgement ack=(MigrationAcknowledgement)_dispatcher.exchangeMessages(_id, _ackRvMap, mr, _settingsInOut, _timeout);
			_log.info("received migration ack: "+_id+" : "+_settingsInOut);
		}
	}
}
