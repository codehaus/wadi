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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public class Server implements Listener {

    protected Log _log=LogFactory.getLog(getClass());
    
    protected final PooledExecutor _executor;
    protected final int _backlog; // 16?
    protected final int _timeout; // secs
    protected final Collection _consumers=Collections.synchronizedSet(new TreeSet());

    public Server(InetSocketAddress address, int backlog, int timeout) {
        _executor=new PooledExecutor(new BoundedBuffer(10), 100); // parameterise
        _executor.setMinimumPoolSize(3);
        _address=address;
        _backlog=backlog;
        _timeout=timeout;
        }
    
    protected ServerSocket _socket;
    protected InetSocketAddress _address;
    protected Thread _thread;
    protected boolean _running;
    protected int _token;

    public void start() throws IOException {
        int port=_address.getPort();
        InetAddress host=_address.getAddress();
        _socket=new ServerSocket(port, _backlog, host);
        _socket.setSoTimeout(_timeout*1000);
        _address=new InetSocketAddress(host, _socket. getLocalPort());
        (_thread=new Thread(new Producer())).start();
        _running=true;
        if (_log.isDebugEnabled()) _log.debug("started: "+_socket);
    }
    
    public void stop() {
        if (_log.isDebugEnabled()) _log.debug("stopping: "+_socket);
        _running=false;
        try {
            _thread.join();
            _log.info("Producer thread joined");
            _thread=null;

            while (_consumers.size()>0) {
                _log.info("waiting for: "+_consumers);
                Thread.sleep(1000);
            }
            _log.info("Consumer threads joined");

            _socket.close();
            _socket=null;

            if (_log.isDebugEnabled()) _log.debug("stopped: "+_address);
        } catch (InterruptedException e) {
            _log.warn("unexpectedly interrupted whilst stopping");
            // TODO - interrupted()?
        } catch (IOException e) {
            _log.warn("could not close server socket");
        }
    }
    
    public void notify(Object token) {
        _consumers.remove(token);
    }
    
    public class Producer implements Runnable {
        
        public void run() {
            try {
                while (_running) {
                    try {
                        if (_timeout==0) Thread.yield();
                        Socket socket=_socket.accept();
                        socket.setSoTimeout(30*1000);
                        Object token=new Integer(_token++);
                        _consumers.add(token);
                        Consumer consumer=new Consumer(socket, Server.this, token);
                        _executor.execute(consumer);
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