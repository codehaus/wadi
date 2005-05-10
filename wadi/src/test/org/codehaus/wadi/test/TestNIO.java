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
package org.codehaus.wadi.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class TestNIO extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass());

    public TestNIO(String name) {
        super(name);
    }

    public static class Connection {
        public Connection(SocketChannel channel, SelectionKey key, Server server) {
        }
    }
    
    public static class Server implements Runnable {
        
        protected final Log _log=LogFactory.getLog(getClass());

        protected final ServerSocketChannel _channel;
        protected final Selector _selector;

        protected InetSocketAddress _address;
        protected Thread _thread;
        protected boolean _running;
        
        public Server(InetSocketAddress address) throws Exception {
            _address=address;
            _channel=ServerSocketChannel.open();
            _channel.configureBlocking(false);
            _channel.socket().bind(_address);
            _address=(InetSocketAddress)_channel.socket().getLocalSocketAddress(); // in case address was not fully specified
            _selector= Selector.open();
        }
        
        public synchronized void start() throws Exception {
           _running=true;
           (_thread=new Thread(this)).start();
        }
        
        public synchronized void stop() throws Exception {
            _running=false;
            _thread.join();
            _thread=null;
        }
        
        public void run() {
            while (_running) {
                try {
                    _log.info("selecting...");
                    _selector.select();
                    _log.info("...selected");
                    
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
 
            Connection connection=new Connection(channel,readKey, Server.this);
            readKey.attach(connection);
            }
        
        public void read(SelectionKey key) {
            _log.info("key: "+key);
            Connection connection = (Connection)key.attachment();
//            if (connection._idle && isOutOfResources())
//                // Don't handle idle connections if out of resources.
//                return;
//            ByteBuffer buf=connection._in.getBuffer();
//            int count=((SocketChannel)key.channel()).read(buf);
//            if (count<0) {
//                connection.close();
//            }
//            else {
//                buf.flip();
//                connection.write(buf);
//            }
        }
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWrite() throws Exception {
        assertTrue(true);
        
        int size=1024;
        byte[] bytes=new byte[size];
        for (int i=0; i<size; i++)
            bytes[i]=(byte)i;
        File file=File.createTempFile("wadi-", ".tst");
        FileOutputStream fos=new FileOutputStream(file);
        fos.write(bytes);
        fos.close();

        _log.info("File: "+file);
        
        assertTrue(file.length()==size);
        FileInputStream fis= new FileInputStream(file);
        FileChannel fc=fis.getChannel();
        MappedByteBuffer mbb=fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        
//        Server server=new Server(new InetSocketAddress(InetAddress.getLocalHost(), 8080));
//        server.start();
//        Thread.sleep(10000);
//        server.stop();
//        _log.info("finished");
    }
}
