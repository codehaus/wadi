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

import java.net.InetSocketAddress;

import org.codehaus.wadi.sandbox.io.Pipe;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public abstract class AbstractSocketServer extends AbstractServer {

    protected final SynchronizedInt _numPipes=new SynchronizedInt(0);

    protected InetSocketAddress _address;

    public AbstractSocketServer(PooledExecutor executor, long pipeTimeout, InetSocketAddress address) {
        super(executor, pipeTimeout);
        _address=address;
    }

    public void add(Pipe pipe) {
        _numPipes.increment();
        if (_log.isTraceEnabled()) _log.trace("adding server Pipe: "+pipe);
    }

    public void remove(Pipe pipe) {
        _numPipes.decrement();
        if (_log.isTraceEnabled()) _log.trace("removing server Pipe: "+pipe);
    }

    public void notifyClosed(Pipe pipe) {
        remove(pipe);
    }

    public void waitForExistingPipes() {
        while (_numPipes.get()>0) {
            if ( _log.isInfoEnabled() ) {

                _log.info("waiting for: " + _numPipes + " Pipe[s]");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if ( _log.isTraceEnabled() ) {

                    _log.trace("unexpected interruption - ignoring", e);
                }
            }
        }
        if ( _log.isInfoEnabled() ) {

            _log.info("existing Pipes have finished running");
        }
    }

}
