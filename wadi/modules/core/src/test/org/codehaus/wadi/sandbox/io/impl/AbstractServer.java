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
package org.codehaus.wadi.sandbox.io.impl;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.sandbox.io.Pipe;
import org.codehaus.wadi.sandbox.io.PipeConfig;
import org.codehaus.wadi.sandbox.io.Server;
import org.codehaus.wadi.sandbox.io.ServerConfig;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public abstract class AbstractServer implements Server, PipeConfig {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final PooledExecutor _executor;
    protected final long _pipeTimeout;

    public AbstractServer(PooledExecutor executor, long pipeTimeout) {
        super();
        _executor=executor;
        _pipeTimeout=pipeTimeout;
    }

    protected ServerConfig _config;
    protected Thread _thread;
    protected volatile boolean _running;
    
    public void init(ServerConfig config) {
        _config=config;
    }
    
    public void start() throws Exception {
        _log.info("starting");
    }

    public void stop() throws Exception {
        _log.info("stopped");
    }

    public void run(Pipe pipe) {
        try {
            _executor.execute(pipe);
        } catch (InterruptedException e) { // TODO - do this safely...
            _log.error(e);
        }
    }

    // PipeConfig
    
    public Contextualiser getContextualiser() {
        return _config.getContextualiser();
    }
    
    public String getNodeId() {
        return _config.getNodeName();
    }
}

