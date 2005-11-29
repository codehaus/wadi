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
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.dindex.StateManager;
import org.codehaus.wadi.dindex.StateManagerConfig;
import org.codehaus.wadi.dindex.messages.DIndexDeletionRequest;
import org.codehaus.wadi.dindex.messages.DIndexDeletionResponse;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationResponse;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertPMToIM;
import org.codehaus.wadi.dindex.newmessages.ReleaseEntryRequest;
import org.codehaus.wadi.dindex.newmessages.ReleaseEntryResponse;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.messages.MoveIMToSM;
import org.codehaus.wadi.gridstate.messages.MovePMToSM;
import org.codehaus.wadi.gridstate.messages.MoveSMToIM;
import org.codehaus.wadi.gridstate.messages.MoveSMToPM;
import org.codehaus.wadi.impl.AbstractMotable;
import org.codehaus.wadi.impl.RankedRWLock;
import org.codehaus.wadi.impl.SimpleMotable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class SimpleStateManager implements StateManager {

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
        _dispatcher.register(this, "onDIndexDeletionRequest", DIndexDeletionRequest.class);
        _dispatcher.register(DIndexDeletionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexRelocationRequest", DIndexRelocationRequest.class);
        _dispatcher.register(DIndexRelocationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexForwardRequest", DIndexForwardRequest.class);

		// GridState - Relocate - 5 messages - IM->PM->SM->IM->SM->PM
		_dispatcher.register(this, "onMessage", MoveIMToPM.class);
        _dispatcher.register(this, "onMessage", MovePMToSM.class);
		_dispatcher.register(MoveSMToIM.class, _inactiveTime);
		_dispatcher.register(MoveIMToSM.class, _inactiveTime);
		_dispatcher.register(MoveSMToPM.class, _inactiveTime);
		}

	public void start() throws Exception {
		// TODO Auto-generated method stub

	}

	public void stop() throws Exception {
        _dispatcher.deregister("onDIndexInsertionRequest", InsertIMToPM.class, 5000);
        _dispatcher.deregister("onDIndexDeletionRequest", DIndexDeletionRequest.class, 5000);
        _dispatcher.deregister("onDIndexRelocationRequest", DIndexRelocationRequest.class, 5000);
        _dispatcher.deregister("onDIndexForwardRequest", DIndexForwardRequest.class, 5000);
	}


    public void onDIndexInsertionRequest(ObjectMessage om, InsertIMToPM request) {
        _config.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onDIndexDeletionRequest(ObjectMessage om, DIndexDeletionRequest request) {
        _config.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onDIndexForwardRequest(ObjectMessage om, DIndexForwardRequest request) {
        _config.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onDIndexRelocationRequest(ObjectMessage om, DIndexRelocationRequest request) {
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
        	immotable.setBodyAsByteArray(bytes);
        	
        	Object key=_get.getKey();
        	Dispatcher dispatcher=_config.getDispatcher();
        	long timeout=_config.getInactiveTime();
        	Destination sm=dispatcher.getLocalDestination();
        	Destination im=(Destination)_get.getIM();
        	MoveSMToIM request=new MoveSMToIM(key, bytes);
        	// send on state from StateMaster to InvocationMaster...
        	if (_log.isTraceEnabled()) _log.trace("exchanging MoveSMToIM between: "+_config.getNodeName(sm)+"->"+_config.getNodeName(im));
        	ObjectMessage message2=(ObjectMessage)dispatcher.exchangeSend(sm, im, request, timeout, _get.getIMCorrelationId());
        	// should receive response from IM confirming safe receipt...
        	if (message2==null) {
        		_log.error("NO REPLY RECEIVED FOR MESSAGE IN TIMEFRAME - PANIC!");
        	} else {
        		MoveIMToSM response=null;
        		try {
        			response=(MoveIMToSM)message2.getObject();
        			// ackonowledge transfer completed to PartitionMaster, so it may unlock resource...
        			dispatcher.reply(_message1,new MoveSMToPM());
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

    //--------------------------------------------------------------------------------------
    
	// called on State Master...
    public void onMessage(ObjectMessage message1, MovePMToSM request) {
        // DO NOT Dispatch onto Partition - deal with it here...
    	Object key=request.getKey();
    	String nodeName=_config.getLocalNodeName();
    	try {
    		RankedRWLock.setPriority(RankedRWLock.EMIGRATION_PRIORITY);

    		// Tricky - we need to call a Moter at this point and start removal of State to other node...
    		
    		try {
    			Immoter promoter=new RelocationImmoter(nodeName, message1, request);
    			boolean found=_config.contextualise(null, null, null, (String)key, promoter, null, true); // if we own session, this will send the correct response...
    			if (found)
    				_log.error("WHAT SHOULD WE DO HERE?");
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

}
