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
package org.codehaus.wadi.dindex.impl;

import java.nio.ByteBuffer;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.dindex.StateManager;
import org.codehaus.wadi.dindex.StateManagerConfig;
import org.codehaus.wadi.dindex.newmessages.DeleteIMToPM;
import org.codehaus.wadi.dindex.newmessages.DeletePMToIM;
import org.codehaus.wadi.dindex.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.dindex.newmessages.EvacuatePMToIM;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertPMToIM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToSM;
import org.codehaus.wadi.dindex.newmessages.MovePMToIM;
import org.codehaus.wadi.dindex.newmessages.MovePMToSM;
import org.codehaus.wadi.dindex.newmessages.MoveSMToIM;
import org.codehaus.wadi.dindex.newmessages.MoveSMToPM;
import org.codehaus.wadi.dindex.newmessages.PutSMToIM;
import org.codehaus.wadi.dindex.newmessages.ReleaseEntryRequest;
import org.codehaus.wadi.dindex.newmessages.ReleaseEntryResponse;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.AbstractMotable;
import org.codehaus.wadi.impl.RankedRWLock;
import org.codehaus.wadi.impl.SimpleMotable;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public class SimpleStateManager implements StateManager {

	protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");
	protected final Dispatcher _dispatcher;
	protected final long _inactiveTime;
	protected final int _resTimeout=500; // TODO - parameterise

	protected StateManagerConfig _config;
	protected Log _log=LogFactory.getLog(getClass());

	public SimpleStateManager(Dispatcher dispatcher, long inactiveTime) {
		super();
		_dispatcher=dispatcher;
		_inactiveTime=inactiveTime;
	}

	public void init(StateManagerConfig config) {
		_config=config;
		_log=LogFactory.getLog(getClass().getName()+"#"+_config.getLocalNodeName());
		_dispatcher.register(this, "onDIndexInsertionRequest", InsertIMToPM.class);
		_dispatcher.register(InsertPMToIM.class, _inactiveTime);
		_dispatcher.register(this, "onDIndexDeletionRequest", DeleteIMToPM.class);
		_dispatcher.register(DeletePMToIM.class, _inactiveTime);
		_dispatcher.register(this, "onDIndexRelocationRequest", EvacuateIMToPM.class);
		_dispatcher.register(EvacuatePMToIM.class, _inactiveTime);
    _dispatcher.register(this, "onPutSMToIM", PutSMToIM.class);

		// GridState - Relocate - 5 messages - IM->PM->SM->IM->SM->PM
		_dispatcher.register(this, "onMessage", MoveIMToPM.class);
		_dispatcher.register(this, "onMessage", MovePMToSM.class);
		_dispatcher.register(MoveSMToIM.class, _inactiveTime);
		_dispatcher.register(MoveIMToSM.class, _inactiveTime);
		_dispatcher.register(MoveSMToPM.class, _inactiveTime);
		// or possibly - IM->PM->IM (failure)
		_dispatcher.register(MovePMToIM.class, _inactiveTime);
	}

	public void start() throws Exception {
		// TODO Auto-generated method stub

	}

	public void stop() throws Exception {
		_dispatcher.deregister("onDIndexInsertionRequest", InsertIMToPM.class, 5000);
		_dispatcher.deregister("onDIndexDeletionRequest", DeleteIMToPM.class, 5000);
		_dispatcher.deregister("onDIndexRelocationRequest", EvacuateIMToPM.class, 5000);
	}


	public void onDIndexInsertionRequest(ObjectMessage om, InsertIMToPM request) {
		_config.getPartition(request.getKey()).onMessage(om, request);
	}

	public void onDIndexDeletionRequest(ObjectMessage om, DeleteIMToPM request) {
		_config.getPartition(request.getKey()).onMessage(om, request);
	}

	public void onDIndexRelocationRequest(ObjectMessage om, EvacuateIMToPM request) {
		_config.getPartition(request.getKey()).onMessage(om, request);
	}

	public void onMessage(ObjectMessage message, MoveIMToPM request) {
		_config.getPartition(request.getKey()).onMessage(message, request);
	}

	//----------------------------------------------------------------------------------------------------

	class PMToIMEmotable extends AbstractMotable {

		protected final String _name;
		protected final String _tgtNodeName;
		protected ObjectMessage _message1;
		protected final MovePMToSM _get;

		public PMToIMEmotable(String name, String nodeName, ObjectMessage message1, MovePMToSM get) {
			_name=name;
			_tgtNodeName=nodeName;
			_message1=message1;
			_get=get;
		}
		public byte[] getBodyAsByteArray() throws Exception {
			throw new UnsupportedOperationException();
		}

		public void setBodyAsByteArray(byte[] bytes) throws Exception {
			Motable immotable=new SimpleMotable();
			immotable.init(_creationTime, _lastAccessedTime, _maxInactiveInterval, _name);
			immotable.setBodyAsByteArray(bytes);

			Dispatcher dispatcher=_config.getDispatcher();
			long timeout=_config.getInactiveTime();
			Destination sm=dispatcher.getLocalDestination();
			Destination im=(Destination)_get.getIM();
			MoveSMToIM request=new MoveSMToIM(immotable);
			// send on state from StateMaster to InvocationMaster...
			if (_log.isTraceEnabled()) _log.trace("exchanging MoveSMToIM between: "+_config.getNodeName(sm)+"->"+_config.getNodeName(im));
			ObjectMessage message2=(ObjectMessage)dispatcher.exchangeSend(sm, im, request, timeout, _get.getIMCorrelationId());
			// should receive response from IM confirming safe receipt...
			if (message2==null) {
                // TODO throw exception
				_log.error("NO REPLY RECEIVED FOR MESSAGE IN TIMEFRAME - PANIC!");
			} else {
				MoveIMToSM response=null;
				try {
					response=(MoveIMToSM)message2.getObject();
					assert(response!=null && response.getSuccess()); // FIXME - we should keep trying til we get an answer or give up...
					// acknowledge transfer completed to PartitionMaster, so it may unlock resource...
					dispatcher.reply(_message1,new MoveSMToPM(true));
				} catch (JMSException e) {
					_log.error("unexpected problem", e);
				}
			}
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
		protected final MovePMToSM _request;

		protected boolean _found=false;
		protected Sync _invocationLock;

		public RelocationImmoter(String nodeName, ObjectMessage message, MovePMToSM request) {
			_tgtNodeName=nodeName;
			_message=message;
			_request=request;
		}

		public Motable nextMotable(String name, Motable emotable) {
			return new PMToIMEmotable(name, _tgtNodeName, _message, _request);
		}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			// work is done in ClusterEmotable...
			// take invocation lock
			//boolean needsRelease=false;
      
      // used to take an invocation lock whlist running, but, if we were already promoting a session from e.g. disc, this would cause a deadlock...
      // do we need to take a lock to prevet anyone else trying to load a session as we emmmigrate it - I don't think so...
      
			_invocationLock=_config.getInvocationLock(name);
//			try {
//				Utils.acquireUninterrupted("Invocation", name, _invocationLock);
//				//needsRelease=true;
//			} catch (TimeoutException e) {
//				_log.error("unexpected timeout - proceding without lock", e);
//			}
			return true;
		}

		public void commit(String name, Motable immotable) {
			// do nothing
			// release invocation lock
			_found=true;
//			Utils.release("Invocation", name, _invocationLock);
		}

		public void rollback(String name, Motable immotable) {
			// this probably has to by NYI... - nasty...
		}

		public boolean contextualise(InvocationContext invocationContext, String id, Motable immotable, Sync motionLock) throws InvocationException {
			return false;
		}

		public String getInfo() {
			return "emigration:"+_tgtNodeName;
		}

		public boolean getFound() {
			return _found;
		}

	}

	//--------------------------------------------------------------------------------------

	// called on State Master...
	public void onMessage(ObjectMessage message1, MovePMToSM request) {
		// DO NOT Dispatch onto Partition - deal with it here...
		Object key=request.getKey();
		//String nodeName=_config.getLocalNodeName();
		try {
			RankedRWLock.setPriority(RankedRWLock.EMIGRATION_PRIORITY);

			// Tricky - we need to call a Moter at this point and start removal of State to other node...

			try {
				Destination im=(Destination)request.getIM();
				String imName=_config.getNodeName(im);
				RelocationImmoter promoter=new RelocationImmoter(imName, message1, request);
				//boolean found=

				// acquire invocation lock here... - we need it - not sure why...
				Sync invocationLock=_config.getInvocationLock((String)key);
				try {
					Utils.acquireUninterrupted("Invocation", (String)key, invocationLock);
				} catch (TimeoutException e) {
					_log.error("unexpected timeout - proceding without lock", e);
				}

				_config.contextualise(null, (String)key, promoter, invocationLock, true); // if we own session, this will send the correct response...
				if (!promoter.getFound()) {
					_log.warn("state not found - perhaps it has just been destroyed: "+key);
					MoveSMToIM req=new MoveSMToIM(null);
					// send on null state from StateMaster to InvocationMaster...
					Destination sm=_dispatcher.getLocalDestination();
					long timeout=_config.getInactiveTime();
					_log.info("sending 0 bytes to : "+imName);
					ObjectMessage ignore=(ObjectMessage)_dispatcher.exchangeSend(sm, im, req, timeout, request.getIMCorrelationId());
					_log.info("received: "+ignore);
					// StateMaster replies to PartitionMaster indicating failure...
					_log.info("reporting failure to PM");
					_dispatcher.reply(message1,new MoveSMToPM(false));
				}
			} catch (Exception e) {
				if (_log.isWarnEnabled()) _log.warn("problem handling relocation request: "+key, e);
			} finally {
				RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
			}
		} finally {
		}
	}

	// evacuation protocol

	public boolean offerEmigrant(String key, Motable emotable, long timeout) {
		Destination to=((RemotePartition)_config.getPartition(key).getContent()).getDestination(); // TODO - HACK - temporary
		Destination from=_dispatcher.getLocalDestination();
    
    // this code on the way in...
//    {
//      PutSMToIM put=new PutSMToIM(key);
//      _dispatcher.send(from, to, null, put);
//      try{Thread.sleep(1000L);} catch (InterruptedException e){};
//      
////      ReleaseEntryResponse ack=null;
////      try {
////        ack=message==null?null:(ReleaseEntryResponse)message.getObject();
////      } catch (JMSException e) {
////        _log.error("could not unpack response", e);
////      }
////
////      if (ack==null) {
////        if (_log.isWarnEnabled()) _log.warn("no acknowledgement within timeframe ("+timeout+" millis): "+key);
////        return false;
////      } else {
////        if (_log.isTraceEnabled()) _log.trace("received acknowledgement within timeframe ("+timeout+" millis): "+key);
////        return true;
////      }
//      return true;
//    }
    // this code on the way out...
		{
		  ReleaseEntryRequest request=new ReleaseEntryRequest(emotable);
		  ObjectMessage message=_dispatcher.exchangeSend(from, to, request, timeout);
		  ReleaseEntryResponse ack=null;
		  try {
		    ack=message==null?null:(ReleaseEntryResponse)message.getObject();
		  } catch (JMSException e) {
		    _log.error("could not unpack response", e);
		  }

		  if (ack==null) {
		    if (_log.isWarnEnabled()) _log.warn("no acknowledgement within timeframe ("+timeout+" millis): "+key);
		    return false;
		  } else {
		    if (_log.isTraceEnabled()) _log.trace("received acknowledgement within timeframe ("+timeout+" millis): "+key);
		    return true;
		  }
		}
    
	}

	public void acceptImmigrant(ObjectMessage message, Location location, String name, Motable motable) {
		if (!_dispatcher.reply(message, new ReleaseEntryResponse(name, location))) {
			if (_log.isErrorEnabled()) _log.error("could not acknowledge safe receipt: "+name);
		}
	}

	protected ImmigrationListener _listener;

	public void setImmigrationListener(ImmigrationListener listener) {
    _dispatcher.register(this, "onEmigrationRequest", ReleaseEntryRequest.class);
		_dispatcher.register(ReleaseEntryResponse.class, _resTimeout);
		_listener=listener;
	}

	public void unsetImmigrationListener(ImmigrationListener listener) {
		if (_listener==listener) {
			_listener=null;
			// TODO ...
			//_dispatcher.deregister("onEmigrationRequest", EmigrationRequest.class, _resTimeout);
			//_dispatcher.deregister("onEmigrationResponse", EmigrationResponse.class, _resTimeout);
		}
	}

	public void onEmigrationRequest(ObjectMessage message, ReleaseEntryRequest request) {
		_listener.onImmigration(message, request.getMotable());
	}

  public void onPutSMToIM(ObjectMessage message, PutSMToIM request) {
	  throw new UnsupportedOperationException("NYI");
    //_config.fetchSession(request.getKey());
  }
  
}
