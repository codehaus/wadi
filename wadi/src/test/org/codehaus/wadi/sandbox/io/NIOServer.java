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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.nio.ByteBufferInputStream;
import org.mortbay.http.nio.SocketChannelOutputStream;

public class NIOServer implements Runnable {
        
        class Connection implements Runnable {
            
            protected final SocketChannel _channel;
            protected final SelectionKey _key;
            protected final ByteBufferInputStream _in;
            protected final SocketChannelOutputStream _out;

            public Connection(SocketChannel channel, SelectionKey key) {
                _channel=channel;
                _key=key;
                _in=new ByteBufferInputStream(_bufferSize);
                _out=new SocketChannelOutputStream(channel, _bufferSize);
                }
            
            public void run() {
                //...
            }

            public synchronized void close()
            throws IOException
            {
                _out.close();
                _in.close();
                if (!_channel.isOpen())
                    return;
                _key.cancel();
                _channel.socket().shutdownOutput();
                _channel.close();
                _channel.socket().close();
                _channel.close();
            }
        }
        
        protected final Log _log=LogFactory.getLog(getClass());
        protected final int _bufferSize;
        
        public NIOServer(InetSocketAddress address, int bufferSize) {
            _address=address;
            _bufferSize=bufferSize;
        }
        
        protected InetSocketAddress _address;
        protected ServerSocketChannel _channel;
        protected Selector _selector;
        protected SelectionKey _key;
        protected Thread _thread;
        protected boolean _running;
        
        public synchronized void start() throws Exception {
            _channel=ServerSocketChannel.open();
            _channel.configureBlocking(false);
            _channel.socket().bind(_address);
            _log.info(_channel);
            _address=(InetSocketAddress)_channel.socket().getLocalSocketAddress(); // in case address was not fully specified
            _selector= Selector.open();
            _key=_channel.register(_selector, SelectionKey.OP_ACCEPT);
            _running=true;
            (_thread=new Thread(this)).start();
        }
        
        public synchronized void stop() throws Exception {
            _running=false;
            _selector.wakeup();
            _thread.join();
            _thread=null;
        }
        
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

        public void accept(SelectionKey key) throws ClosedChannelException, IOException {
            ServerSocketChannel server=(ServerSocketChannel)key.channel();
            SocketChannel channel=server.accept();
            channel.configureBlocking(false);
            SelectionKey readKey=channel.register(_selector, SelectionKey.OP_READ);
            
            Connection connection=new Connection(channel, readKey);
            readKey.attach(connection);
        }
        
        public void read(SelectionKey key) throws IOException {
            // _log.info("key: "+key);
            Connection connection=(Connection)key.attachment();
            
//          if (connection._idle && isOutOfResources())
//          // Don't handle idle connections if out of resources.
//          return;
            
            ByteBuffer buffer=connection._in.getBuffer();
            int count=((SocketChannel)key.channel()).read(buffer);
            if (count<0) {
                connection.close();
            } else {
                buffer.flip();
                connection._in.write(buffer);
            }
        }
    }