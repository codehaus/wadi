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
package org.codehaus.wadi.sandbox.io;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public abstract class AbstractServer implements Server {

    protected final Log _log = LogFactory.getLog(getClass());
    protected final PooledExecutor _executor;

    public AbstractServer(PooledExecutor executor) {
        super();
        _executor=executor;
    }

    protected Thread _thread;
    protected volatile boolean _running;
    public void start() throws Exception {
        // TODO Auto-generated method stub
    }

    public void stop() throws Exception {
        // TODO Auto-generated method stub
    }

    // Notifiable
    
    public void doConnection(Connection connection) {
        try {
            _executor.execute(connection);
        } catch (InterruptedException e) {
            _log.error(e);
        }
    }
}

