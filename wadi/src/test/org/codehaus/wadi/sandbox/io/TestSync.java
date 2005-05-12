package org.codehaus.wadi.sandbox.io;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import junit.framework.TestCase;
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

public class TestSync extends TestCase {
    
    protected Log _log = LogFactory.getLog(getClass());
    
    public TestSync(String name) {
        super(name);
    }
    
    protected InetSocketAddress _address;
    protected Server _server;
    
    protected void setUp() throws Exception {
        super.setUp();
        _address=new InetSocketAddress(8888);
        _server=new Server(_address, 16, 1);
        _server.start();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        _server.stop();
    }

    public static class RoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(RoundTripServerPeer.class);
        
        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
            try {
                oos.writeBoolean(true); // ack
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public static class RoundTripClientPeer extends Peer {
        
        public RoundTripClientPeer(InetSocketAddress address) throws IOException {
            super(address);
        }
        
        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
            try {
                oos.writeObject(new RoundTripServerPeer());
                oos.flush();
                assertTrue(ois.readBoolean());
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    protected final int _count=1000;
    
    public void testRoundTrips() throws Exception {
        long start=System.currentTimeMillis();
        for (int i=0; i<_count; i++) {
            Peer peer=new RoundTripClientPeer(_address);
            peer.run();
        }
        long elapsed=System.currentTimeMillis()-start;
        _log.info("rate="+(_count*1000/elapsed)+" round-trips/second");
    }
    
    public static class MixedContentServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(MixedContentServerPeer.class);
        
        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
            try {
                int capacity=ois.readInt();
                ByteBuffer buffer=ByteBuffer.allocateDirect(capacity);
                socket.getChannel().read(buffer);
                oos.writeBoolean(true); // ack
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public static class MixedContentClientPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(MixedContentClientPeer.class);
        protected final ByteBuffer _buffer;
        
        public MixedContentClientPeer(InetSocketAddress address, ByteBuffer buffer) throws IOException {
            super(address);
            _buffer=buffer;
        }
        
        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
            try {
                oos.writeObject(new MixedContentServerPeer());
                oos.writeInt(_buffer.capacity());
                oos.flush();
                SocketChannel channel=socket.getChannel();
                
                // AHA ! - you can't get the Channel for a preexisting Socket :-(
                // back to the drawing board...
                
                channel.write(_buffer);
                oos.flush();
                assertTrue(ois.readBoolean());
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
//    public void testMixedContent() throws Exception {
//        long start=System.currentTimeMillis();
//        int capacity=4096;
//        ByteBuffer buffer=ByteBuffer.allocateDirect(capacity);
//        for (int i=0; i<1; i++) {
//            Peer peer=new MixedContentClientPeer(_address, buffer);
//            peer.run();
//        }
//        long elapsed=System.currentTimeMillis()-start;
//        _log.info("rate="+(_count*1000/elapsed)+" round-trips/second");
//    }

}
