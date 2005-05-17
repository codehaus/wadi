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

import java.net.InetSocketAddress;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public abstract class AbstractSocketServer extends AbstractServer implements SocketConnectionConfig {

    protected InetSocketAddress _address;
    protected volatile int _numConnections;

    public AbstractSocketServer(PooledExecutor executor, InetSocketAddress address) {
        super(executor);
        _address=address;
    }

    public void doConnection(Connection connection) {
        _numConnections++;
        super.doConnection(connection);
    }

    // Notifiable
    
    public void notifyCompleted() {
        _numConnections--;
    }

    // Notifiable
    
    public void waitForExistingConnections() {
        while (_numConnections>0) {
            _log.info("waiting for: "+_numConnections+" Connection[s]");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
            }
        }
        _log.info("existing Connections have finished running");
    }
    
}
