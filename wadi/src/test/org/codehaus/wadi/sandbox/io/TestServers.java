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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import junit.framework.TestCase;

public class TestServers extends TestCase {
    
    protected Log _log = LogFactory.getLog(getClass());
    
    public TestServers(String name) {
        super(name);
    }
    
    protected InetSocketAddress _bioAddress;
    protected Server _bioServer;
    protected InetSocketAddress _nioAddress;
    protected Server _nioServer;

    protected final int _count=10000;
    
    protected void setUp() throws Exception {
        super.setUp();
        // an unbounded queue, serviced by 5 threads
        PooledExecutor pool;
        pool=new PooledExecutor(new LinkedQueue());
        pool.setKeepAliveTime(-1); // live forever
        pool.createThreads(5);
        
        _bioAddress=new InetSocketAddress(8888);
        PooledExecutor executor;
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setMinimumPoolSize(3);
        _bioServer=new BIOServer(executor, _bioAddress, 16, 1); // backlog, timeout
        _bioServer.start();
        _nioAddress=new InetSocketAddress(8889);
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setMinimumPoolSize(3);
        _nioServer=new NIOServer(executor, _nioAddress); // bufferSize, numConsumers
        _nioServer.start();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        _nioServer.stop();
        _bioServer.stop();
    }

    public static class SingleRoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripServerPeer.class);
        
        public void process(Channel channel, InputStream is, OutputStream os) {
            try {
                ObjectOutputStream oos=new ObjectOutputStream(os);
                oos.writeBoolean(true); // ack
                oos.flush();
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public static class SingleRoundTripClientPeer extends Peer {
        
        public SingleRoundTripClientPeer(InetSocketAddress address) throws IOException {
            super(address);
        }
        
        public void process(Channel channel, InputStream is, OutputStream os) {
            try {
                ObjectOutputStream oos=new ObjectOutputStream(os);
                oos.writeObject(new SingleRoundTripServerPeer());
                oos.flush();
                ObjectInputStream ois=new ObjectInputStream(is);
                assertTrue(ois.readBoolean());
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public void testSingleRoundTrip() throws Exception {
        testSingleRoundTrip("BIO", _bioAddress);
        testSingleRoundTrip("NIO", _nioAddress);
    }
    
    public void testSingleRoundTrip(String info, InetSocketAddress address) throws Exception {
        long start=System.currentTimeMillis();
        for (int i=0; i<_count; i++) {
            Peer peer=new SingleRoundTripClientPeer(address);
            peer.run();
        }
        long elapsed=System.currentTimeMillis()-start;
        _log.info(info+" rate="+(_count*1000/elapsed)+" round-trips/second");
    }
    
    public static class MultipleRoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripServerPeer.class);
        
        public void process(Channel channel, InputStream is, OutputStream os) {
            try {
                ObjectInputStream ois=new ObjectInputStream(is);
                ObjectOutputStream oos=new ObjectOutputStream(os);
                while (ois.readBoolean()) {
                    oos.writeBoolean(true);
                    oos.flush();
                }
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public static class MultipleRoundTripClientPeer extends Peer {
        
        protected final int _numTrips;
        
        public MultipleRoundTripClientPeer(InetSocketAddress address, int numTrips) throws IOException {
            super(address);
            _numTrips=numTrips;
        }
        
        public void process(Channel channel, InputStream is, OutputStream os) {
            try {
                ObjectOutputStream oos=new ObjectOutputStream(os);
                oos.writeObject(new MultipleRoundTripServerPeer());
                ObjectInputStream ois=new ObjectInputStream(is);
                for (int i=0; i<_numTrips; i++) {
                    oos.writeBoolean(true);
                    oos.flush();
                    ois.readBoolean();
                }
                oos.writeBoolean(false);
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }

    
    public void testMultipleRoundTrip() throws Exception {
        testMultipleRoundTrip("NIO", _nioAddress);
        testMultipleRoundTrip("BIO", _bioAddress);
    }
    
    public void testMultipleRoundTrip(String info, InetSocketAddress address) throws Exception {
        long start=System.currentTimeMillis();
        Peer peer=new SingleRoundTripClientPeer(address);
        peer.run();
        long elapsed=System.currentTimeMillis()-start;
        _log.info(info+" rate="+(_count*1000/elapsed)+" round-trips/second");
    }
    
//    public static class MixedContentServerPeer extends Peer {
//        
//        protected static final Log _log=LogFactory.getLog(MixedContentServerPeer.class);
//        
//        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
//            try {
//                int capacity=ois.readInt();
//                ByteBuffer buffer=ByteBuffer.allocateDirect(capacity);
//                socket.getChannel().read(buffer);
//                oos.writeBoolean(true); // ack
//            } catch (IOException e) {
//                _log.error(e);
//            }
//        }
//    }
    
//    public static class MixedContentClientPeer extends Peer {
//        
//        protected static final Log _log=LogFactory.getLog(MixedContentClientPeer.class);
//        protected final ByteBuffer _buffer;
//        
//        public MixedContentClientPeer(InetSocketAddress address, ByteBuffer buffer, boolean inputThenOutput) throws IOException {
//            super(address, inputThenOutput);
//            _buffer=buffer;
//        }
//        
//        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
//            try {
//                oos.writeObject(new MixedContentServerPeer());
//                oos.writeInt(_buffer.capacity());
//                oos.flush();
//                SocketChannel channel=socket.getChannel();
//                
//                // AHA ! - you can't get the Channel for a preexisting Socket :-(
//                // back to the drawing board...
//                
//                channel.write(_buffer);
//                oos.flush();
//                assertTrue(ois.readBoolean());
//            } catch (IOException e) {
//                _log.error(e);
//            }
//        }
//    }
    
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
    
//    public static class PeerMoter extends Peer implements Moter {
//        
//        public PeerMoter(Socket socket) throws IOException {
//            super(socket);
//        }
//        
//        public boolean prepare(String name, Motable emotable, Motable immotable) {
//            // lock e
//            return true;
//        }
//
//        public void commit(String name, Motable motable) {
//            
//        }
//        
//        public void rollback(String name, Motable motable) {
//            
//        }
//
//        public String getInfo() {
//            return "peer";
//        }
//
//    }
//    
//    public static class PeerEmoter extends PeerMoter implements Emoter{
//    }
//    
//    public static class PeerImmoter extends PeerMoter implements Immoter {
//        
//        Motable nextMotable(String id, Motable emotable);
//
//        boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable, Sync motionLock) throws IOException, ServletException;
//
//    }
    
//    public void testMigration() throws Exception {
//        
//        Emoter emoter=null;
//        Immoter immoter=null;
//        
//        Motable emotable=new SimpleMotable();
//        String name="foo";
//        long time=System.currentTimeMillis();
//        emotable.init(time, time, 30*60, name);
//        
//        Utils.mote(emoter, immoter, emotable, name);
//    }

}
