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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.codehaus.wadi.io.Connection;
import org.codehaus.wadi.io.NIOConnectionConfig;

import EDU.oswego.cs.dl.util.concurrent.FIFOReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

// NOTES - reuse server BBS
// Stream should implement bulk transfers

// Do not put Connections onto Queue until the have input
// When a Connection's Peer finishes, it loses its Thread, but is not clos()-ed

public class NIOServer extends AbstractSocketServer implements NIOConnectionConfig {
    
    protected final SynchronizedBoolean _accepting=new SynchronizedBoolean(false);
    protected final ReadWriteLock _lock=new FIFOReadWriteLock();
    protected final EDU.oswego.cs.dl.util.concurrent.Channel _queue=new LinkedQueue(); // parameterise ?; // we get our ByteBuffers from here...
    protected final long _serverTimeout;
    protected final int _outputBufferSize;
    
    public NIOServer(PooledExecutor executor, long connectionTimeout, InetSocketAddress address, long serverTimeout, int numInputBuffers, int inputBufferSize, int outputBufferSize) {
        super(executor, connectionTimeout, address);
        _serverTimeout=serverTimeout;
        _outputBufferSize=outputBufferSize;

        for (int i=0; i<numInputBuffers; i++)
            Utils.safePut(ByteBuffer.allocateDirect(inputBufferSize), _queue);
    }
    
    protected ServerSocketChannel _channel;
    protected Selector _selector;
    protected SelectionKey _key;
    
    public synchronized void start() throws Exception {
        _channel=ServerSocketChannel.open();
        _channel.configureBlocking(false);
        _channel.socket().bind(_address);
        _channel.socket().setSoTimeout((int)_serverTimeout);
        //_channel.socket().setReuseAddress(true);
        _log.info(_channel);
        _address=(InetSocketAddress)_channel.socket().getLocalSocketAddress(); // in case address was not fully specified
        _selector= Selector.open();
        _key=_channel.register(_selector, SelectionKey.OP_ACCEPT);
        _running=true;
        _accepting.set(true);
        (_thread=new Thread(new Producer(), "WADI NIO Server")).start();
        _log.info("Producer thread started");
        _log.info("started: "+_channel);
    }
    
    public synchronized void stop() throws Exception {
        _log.info("stopping: "+_channel);
        stopAcceptingConnections();
        waitForExistingConnections();
        // stop Producer loop
        _running=false;
        _selector.wakeup();
        _thread.join();
        _selector.close();
        _thread=null;
        _log.info("Producer thread stopped");
        // tidy up
        _channel.socket().close();
        _channel.close();
        _log.info("stopped: "+_address);
    }
    
    public void stopAcceptingConnections() {
        _accepting.set(false);
    }
    
    public void accept(SelectionKey key) throws ClosedChannelException, IOException {
        ServerSocketChannel server=(ServerSocketChannel)key.channel();
        SocketChannel channel=server.accept();
        channel.configureBlocking(false);
        SelectionKey readKey=channel.register(_selector, SelectionKey.OP_READ/*|SelectionKey.OP_WRITE*/);
        NIOServerConnection connection=new NIOServerConnection(this, _connectionTimeout, channel, readKey, new LinkedQueue(), _queue, _outputBufferSize); // reuse the queue
        readKey.attach(connection);
        add(connection);
    }
    
    public void read(SelectionKey key) throws IOException {
        NIOServerConnection connection=(NIOServerConnection)key.attachment();
        ByteBuffer buffer=(ByteBuffer)Utils.safeTake(_queue);

        int count=((SocketChannel)key.channel()).read(buffer); // read off network into buffer
        if (count<0) {
            if (_log.isTraceEnabled()) _log.trace("committing server Connection: "+connection);
            connection.commit();
            buffer.clear();
            Utils.safePut(buffer, _queue); // could be cleverer
        } else {
            if (_log.isTraceEnabled()) _log.trace("servicing server Connection: "+connection+" ("+count+" bytes)");
            buffer.flip();
            Utils.safePut(buffer, connection);
        }
        
        if (!connection.getRunning()) {
            connection.setRunning(true);
            try {
                if (_log.isTraceEnabled()) _log.trace("running server Connection: "+connection);
                _executor.execute(connection);
            } catch (InterruptedException e) { // TODO - do this safely...
                _log.error("problem running connection", e);
            }
        }
    }
    
    
    public class Producer implements Runnable {
        
        public void run() {
            SelectionKey key=null;
            
             while (_running) {
                 
                 Sync lock=_lock.writeLock();
                 do {
                     try {
                         lock.acquire();
                     } catch (InterruptedException e) {
                         // ignore
                     }
                 } while (Thread.interrupted());
            
                try {
                    _selector.select();
                    
                    for (Iterator i=_selector.selectedKeys().iterator(); i.hasNext(); ) {
                        key=(SelectionKey)i.next();
                        
                        //boolean used=false;
                        //_log.info("picked up key: "+key);
                        
                        if (key.isAcceptable() && _accepting.get()) {
                            accept(key);
                            //used=true;
                            //_log.info("accepted key: "+key);
                        }
                        
                        if (key.isReadable()) {
                            read(key);
                            //used=true;
                            //_log.info("read key: "+key);
                        }
                        
//                        if (key.isWritable()) {
//                            used=true;
//                           // _log.info("wrote key: "+key);
//                        }
//                        if (key.isConnectable()) {
//                            used=true;
//                            //_log.info("connected key: "+key);
//                        }
                        
//                        if (!used)
//                            _log.warn("unused key: "+key);
                            
                        i.remove();
                        
                    }
                } catch (Throwable t) {
                    _log.error("unexpected problem", t);
                } finally {
                    lock.release();
                    // now threads who want to close selectors, channels etc can run on a read lock...
                }
            }
        }
    }        
    
    // ConnectionConfig

    public void notifyIdle(Connection connection) {
        if (_log.isTraceEnabled()) _log.trace("idling server Connection: "+connection);
        ((NIOServerConnection)connection).setRunning(false);
    }
    
    // NIOConnectionConfig
    
    protected final Sync _dummy=new NullSync();
    public Sync getLock() {
        //return _dummy;
        return _lock.readLock();
    }

}
