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

import javax.jms.ObjectMessage;

import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.StateManager;
import org.codehaus.wadi.dindex.StateManagerConfig;
import org.codehaus.wadi.gridstate.Dispatcher;

public class SimpleStateManager implements StateManager {

	protected final Dispatcher _dispatcher;
	protected final long _inactiveTime;
	
	protected StateManagerConfig _config;
	
	public SimpleStateManager(Dispatcher dispatcher, long inactiveTime) {
		super();
		_dispatcher=dispatcher;
		_inactiveTime=inactiveTime;
	}

	public void init(StateManagerConfig config) {
		_config=config;
        _dispatcher.register(this, "onDIndexInsertionRequest", DIndexInsertionRequest.class);
        _dispatcher.register(DIndexInsertionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexDeletionRequest", DIndexDeletionRequest.class);
        _dispatcher.register(DIndexDeletionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexRelocationRequest", DIndexRelocationRequest.class);
        _dispatcher.register(DIndexRelocationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexForwardRequest", DIndexForwardRequest.class);
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
        onDIndexRequest(om, request);
    }

    public void onDIndexDeletionRequest(ObjectMessage om, DIndexDeletionRequest request) {
        onDIndexRequest(om, request);
    }

    public void onDIndexForwardRequest(ObjectMessage om, DIndexForwardRequest request) {
        onDIndexRequest(om, request);
    }

    public void onDIndexRelocationRequest(ObjectMessage om, DIndexRelocationRequest request) {
        onDIndexRequest(om, request);
    }

    protected void onDIndexRequest(ObjectMessage om, DIndexRequest request) {
        int partitionKey=request.getPartitionKey(_config.getNumPartitions());
        _config.getPartition(partitionKey).dispatch(om, request);
    }

}
