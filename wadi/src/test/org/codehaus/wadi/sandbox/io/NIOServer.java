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

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

// NOTES - reuse server BBS
// Stream should implement bulk transfers

// JMS Server should be implemented....

public class NIOServer extends AbstractSocketServer {
    
    protected final EDU.oswego.cs.dl.util.concurrent.Channel _queue; // we get our ByteBuffers from here...
    
    public NIOServer(PooledExecutor executor, InetSocketAddress address) {
        super(executor, address);
        _queue=new LinkedQueue(); // parameterise ?
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
        (_thread=new Thread(new Producer())).start();
        _log.info("Producer thread started");
        _log.info("started: "+_channel);
    }
    
    public synchronized void stop() throws Exception {
        _log.info("stopping: "+_channel);
        _running=false;
        _selector.wakeup();
        _selector.close();
        _channel.socket().close();
        _channel.close();
        _thread.join();
        _thread=null;
        _log.info("Producer thread stopped");
        // wait for all connections to finish...
        // how ?
        _log.info("Connections all consumed");
        // stop all consumers...
        // how - TODO
        _log.info("Consumer threads stopped");
        _log.info("stopped: "+_address);
    }
    
    public void accept(SelectionKey key) throws ClosedChannelException, IOException {
        ServerSocketChannel server=(ServerSocketChannel)key.channel();
        SocketChannel channel=server.accept();
        channel.configureBlocking(false);
        SelectionKey readKey=channel.register(_selector, SelectionKey.OP_READ);
        
        NIOConnection connection=new NIOConnection(this, channel, readKey, new LinkedQueue(), _queue); // reuse the queue
        readKey.attach(connection);
        doConnection(connection);
    }
    
    public void read(SelectionKey key) throws IOException {
        // _log.info("key: "+key);
        NIOConnection connection=(NIOConnection)key.attachment();
        
//      if (connection._idle && isOutOfResources())
//      // Don't handle idle connections if out of resources.
//      return;
        
        ByteBuffer buffer=ByteBuffer.allocateDirect(4096); // tmp solution...
        int count=((SocketChannel)key.channel()).read(buffer); // read off network into buffer
        if (count<0) {
            connection.commit();
            // what about tidying up associated objects - or does consumer thread do that ?
        } else {
            buffer.flip();
            Utils.safePut(buffer, connection);
        }
    }
    
    
    public class Producer implements Runnable {
        
        public void run() {
            while (_running) {
                try {
                    _selector.select();
                    
                    for (Iterator i=_selector.selectedKeys().iterator(); i.hasNext(); ) {
                        SelectionKey key=(SelectionKey)i.next();
                        
                        if (key.isAcceptable())
                            accept(key);
                        
                        if (key.isReadable())
                            read(key);
                        
                        i.remove();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }        
}
