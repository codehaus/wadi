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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.codehaus.wadi.sandbox.io.Pipe;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;


/**
 * A Socket Server - you send it instances of Peer, which are then fed the Socket that they arrived on to consume...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class BIOServer extends AbstractSocketServer {
    
    protected final int _backlog; // 16?
    protected final long _serverTimeout; // secs
    
    public BIOServer(PooledExecutor executor, long pipeTimeout, InetSocketAddress address, long serverTimeout, int backlog) {
        super(executor, pipeTimeout, address);
        _backlog=backlog;
        _serverTimeout=serverTimeout;
        }
    
    protected ServerSocket _socket;
    
    public void start() throws IOException {
        _running=true;
        int port=_address.getPort();
        InetAddress host=_address.getAddress();
        _socket=new ServerSocket(port, _backlog, host);
        _socket.setSoTimeout((int)_serverTimeout);
        //_socket.setReuseAddress(true);
        _address=new InetSocketAddress(host, _socket. getLocalPort());
        (_thread=new Thread(new Producer(), "WADI BIO Server")).start();
        if ( _log.isInfoEnabled() ) {

            _log.info("Producer thread started");
        }
        if (_log.isDebugEnabled()) _log.debug("started: "+_socket);
    }
    
    public void stop() {
        if (_log.isDebugEnabled()) _log.debug("stopping: "+_socket);
        
        stopAcceptingPipes();
        waitForExistingPipes();
        
        try {
            _socket.close();
        } catch (IOException e) {
            _log.warn("problem closing server socket", e);
        }
        _socket=null;
        
        if (_log.isDebugEnabled()) _log.debug("stopped: "+_address);
    }
    
    public void stopAcceptingPipes() {
        _running=false;
        do {
            try {
                _thread.join();
            } catch (InterruptedException e) {
                if ( _log.isTraceEnabled() ) {

                    _log.trace("unexpected interruption - ignoring", e);
                }
            }
        } while (Thread.interrupted());
        if ( _log.isInfoEnabled() ) {

            _log.info("Producer thread stopped");
        }
        _thread=null;
    }
    
    public class Producer implements Runnable {
        
        public void run() {
            try {
                while (_running) {
                    try {
                        if (_serverTimeout==0) Thread.yield();
                        Socket socket=_socket.accept();
                        BIOPipe pipe=new BIOPipe(BIOServer.this, _pipeTimeout, socket);
                        add(pipe);
                        BIOServer.this.run(pipe);
                        
                    } catch (SocketTimeoutException ignore) {
                        // ignore...
                    }
                }
            } catch (IOException e) {
                _log.warn("unexpected io problem - stopping");
            }
        }
        
    }
    
    // PipeConfig

    public void notifyIdle(Pipe pipe) {
        // BIOServer does not support idling Pipes :-(        
    }
    
}