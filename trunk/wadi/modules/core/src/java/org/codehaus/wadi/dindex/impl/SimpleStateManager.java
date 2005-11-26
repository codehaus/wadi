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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.dindex.StateManager;
import org.codehaus.wadi.dindex.StateManagerConfig;
import org.codehaus.wadi.dindex.messages.DIndexDeletionRequest;
import org.codehaus.wadi.dindex.messages.DIndexDeletionResponse;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionResponse;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationResponse;
import org.codehaus.wadi.dindex.newmessages.ReleaseEntryRequest;
import org.codehaus.wadi.dindex.newmessages.ReleaseEntryResponse;
import org.codehaus.wadi.dindex.newmessages.RelocationRequestI2P;
import org.codehaus.wadi.gridstate.Dispatcher;

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
        _dispatcher.register(this, "onDIndexInsertionRequest", DIndexInsertionRequest.class);
        _dispatcher.register(DIndexInsertionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexDeletionRequest", DIndexDeletionRequest.class);
        _dispatcher.register(DIndexDeletionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexRelocationRequest", DIndexRelocationRequest.class);
        _dispatcher.register(DIndexRelocationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexForwardRequest", DIndexForwardRequest.class);

        _dispatcher.register(this, "onMessage", RelocationRequestI2P.class);
	}

	public void start() throws Exception {
		// TODO Auto-generated method stub

	}

	public void stop() throws Exception {
        _dispatcher.deregister("onDIndexInsertionRequest", DIndexInsertionRequest.class, 5000);
        _dispatcher.deregister("onDIndexDeletionRequest", DIndexDeletionRequest.class, 5000);
        _dispatcher.deregister("onDIndexRelocationRequest", DIndexRelocationRequest.class, 5000);
        _dispatcher.deregister("onDIndexForwardRequest", DIndexForwardRequest.class, 5000);
	}


    public void onDIndexInsertionRequest(ObjectMessage om, DIndexInsertionRequest request) {
        _config.getPartition(request.getPartitionKey(_config.getNumPartitions())).onMessage(om, request);
    }

    public void onDIndexDeletionRequest(ObjectMessage om, DIndexDeletionRequest request) {
        _config.getPartition(request.getPartitionKey(_config.getNumPartitions())).onMessage(om, request);
    }

    public void onDIndexForwardRequest(ObjectMessage om, DIndexForwardRequest request) {
        _config.getPartition(request.getPartitionKey(_config.getNumPartitions())).onMessage(om, request);
    }

    public void onDIndexRelocationRequest(ObjectMessage om, DIndexRelocationRequest request) {
        _config.getPartition(request.getPartitionKey(_config.getNumPartitions())).onMessage(om, request);
    }

    public void onMessage(ObjectMessage om, RelocationRequestI2P request) {
    	_log.info("MESSAGE ARRIVED: "+request);
        _config.getPartition(request.getNodeName()).onMessage(om, request);
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
