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
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public abstract class AbstractSocketServer extends AbstractServer {

    protected final SynchronizedInt _numConnections=new SynchronizedInt(0);

    protected InetSocketAddress _address;

    public AbstractSocketServer(PooledExecutor executor, long connectionTimeout, InetSocketAddress address) {
        super(executor, connectionTimeout);
        _address=address;
    }

    public void add(Connection connection) {
        _numConnections.increment();
        if (_log.isTraceEnabled()) _log.trace("adding server Connection: "+connection);
    }

    public void remove(Connection connection) {
        _numConnections.decrement();
        if (_log.isTraceEnabled()) _log.trace("removing server Connection: "+connection);
    }

    public void notifyClosed(Connection connection) {
        remove(connection);
    }

    public void waitForExistingConnections() {
        while (_numConnections.get()>0) {
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
