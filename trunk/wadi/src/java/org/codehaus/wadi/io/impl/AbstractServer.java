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
package org.codehaus.wadi.io.impl;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.io.Connection;
import org.codehaus.wadi.io.ConnectionConfig;
import org.codehaus.wadi.io.Server;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public abstract class AbstractServer implements Server, ConnectionConfig {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final PooledExecutor _executor;
    protected final long _connectionTimeout;

    public AbstractServer(PooledExecutor executor, long connectionTimeout) {
        super();
        _executor=executor;
        _connectionTimeout=connectionTimeout;
    }

    protected Thread _thread;
    protected volatile boolean _running;
    
    public void start() throws Exception {
        // TODO Auto-generated method stub
    }

    public void stop() throws Exception {
        // TODO Auto-generated method stub
    }

    public void run(Connection connection) {
        try {
            _executor.execute(connection);
        } catch (InterruptedException e) { // TODO - do this safely...
            _log.error(e);
        }
    }
}

