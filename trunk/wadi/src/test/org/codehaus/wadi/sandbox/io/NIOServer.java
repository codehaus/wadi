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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import EDU.oswego.cs.dl.util.concurrent.FIFOReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

// NOTES - reuse server BBS
// Stream should implement bulk transfers

// Do not put Connections onto Queue until the have input
// When a Connection's Peer finishes, it loses its Thread, but is not clos()-ed

public class NIOServer extends AbstractSocketServer {
    
    protected final SynchronizedBoolean _accepting=new SynchronizedBoolean(false);
    protected final ReadWriteLock _lock=new FIFOReadWriteLock();
    protected final EDU.oswego.cs.dl.util.concurrent.Channel _queue; // we get our ByteBuffers from here...
    protected final int _outputBufferSize;
    
    public NIOServer(PooledExecutor executor, InetSocketAddress address, int numInputBuffers, int inputBufferSize, int outputBufferSize) {
        super(executor, address);
        _queue=new LinkedQueue(); // parameterise ?
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
        _selector.wakeup(); // we should go round the
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
        NIOConnection connection=new NIOConnection(this, channel, readKey, new LinkedQueue(), _queue, _outputBufferSize); // reuse the queue
        readKey.attach(connection);
        doConnection(connection);
    }
    
    public void read(SelectionKey key) throws IOException {
        // _log.info("key: "+key);
        NIOConnection connection=(NIOConnection)key.attachment();
        
//      if (connection._idle && isOutOfResources())
//      // Don't handle idle connections if out of resources.
//      return;
        ByteBuffer buffer=(ByteBuffer)Utils.safeTake(_queue);
        int count=((SocketChannel)key.channel()).read(buffer); // read off network into buffer
        if (count<0) {
            connection.commit();
            buffer.clear();
            Utils.safePut(buffer, _queue); // could be cleverer
            // what about tidying up associated objects - or does consumer thread do that ?
        } else {
            buffer.flip();
            Utils.safePut(buffer, connection);
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
                        
                        boolean used=false;
                        //_log.info("picked up key: "+key);
                        
                        if (key.isAcceptable() && _accepting.get()) {
                            accept(key);
                            used=true;
                            //_log.info("accepted key: "+key);
                        }
                        
                        if (key.isReadable()) {
                            read(key);
                            used=true;
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
                        
                        
                        if (!used)
                            _log.warn("unused key: "+key);
                            
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
    
    public Connection makeClientConnection(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        SelectionKey key=channel.register(_selector, /*SelectionKey.OP_WRITE|*/SelectionKey.OP_READ);
        NIOConnection connection=new NIOConnection(this, channel, key, new LinkedQueue(), _queue, _outputBufferSize); // reuse the queue
        key.attach(connection);
        return connection;
    }
    
    public Sync getReadLock() {
        return _lock.readLock();
    }
}
