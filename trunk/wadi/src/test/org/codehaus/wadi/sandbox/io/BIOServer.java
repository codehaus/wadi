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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

interface Listener {void notifyCompleted();}

/**
 * A Socket Server - you send it instances of Peer, which are then fed the Socket that they arrived on to consume...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class BIOServer implements Listener {
    
    protected Log _log=LogFactory.getLog(getClass());
    
    protected final PooledExecutor _executor;
    protected final int _backlog; // 16?
    protected final int _timeout; // secs
    protected volatile int _connections=0;

    public BIOServer(InetSocketAddress address, int backlog, int timeout) {
        _executor=new PooledExecutor(new BoundedBuffer(10), 100); // parameterise
        _executor.setMinimumPoolSize(3);
        _address=address;
        _backlog=backlog;
        _timeout=timeout;
        }
    
    protected ServerSocket _socket;
    protected InetSocketAddress _address;
    protected Thread _thread;
    protected volatile boolean _running;

    public void start() throws IOException {
        _running=true;
        int port=_address.getPort();
        InetAddress host=_address.getAddress();
        _socket=new ServerSocket(port, _backlog, host);
        _socket.setSoTimeout(_timeout*1000);
        _address=new InetSocketAddress(host, _socket. getLocalPort());
        (_thread=new Thread(new Producer())).start();
        _log.info("Producer thread started");
        if (_log.isDebugEnabled()) _log.debug("started: "+_socket);
    }
    
    public void stop() {
        if (_log.isDebugEnabled()) _log.debug("stopping: "+_socket);
        _running=false;
        
        do {
            try {
                _thread.join();
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
        _log.info("Producer thread stopped");
        _thread=null;
        
        while (_connections>0) {
            _log.info("waiting for: "+_connections+" thread[s]");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
            }
        }
        _log.info("Consumer threads stopped");
        
        try {
            _socket.close();
        } catch (IOException e) {
            _log.warn("problem closing server socket", e);
        }
        _socket=null;
        
        if (_log.isDebugEnabled()) _log.debug("stopped: "+_address);
    }
    
    public void notifyCompleted() {
        _connections--;
    }
    
    public class Producer implements Runnable {
        
        public void run() {
            try {
                while (_running) {
                    try {
                        if (_timeout==0) Thread.yield();
                        Socket socket=_socket.accept();
                        socket.setSoTimeout(30*1000);
                        _connections++;
                        BIOConnection connection=new BIOConnection(socket, BIOServer.this);
                        
                        // it would be easy to refactor this to work [optionally] in the same way
                        // as the NIOServer - i.e. a Pool of Consumer threads working on a Queue of Connections...
                        _executor.execute(connection);
                        
                    } catch (SocketTimeoutException ignore) {
                        // ignore...
                    } catch (InterruptedException e) {
                        _log.error(e);
                    }
                }
            } catch (IOException e) {
                _log.warn("unexpected io problem - stopping");
            }
        }
        
    }
    
}