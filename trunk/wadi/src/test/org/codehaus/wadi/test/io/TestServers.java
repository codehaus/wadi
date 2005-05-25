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

package org.codehaus.wadi.test.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.jms.Destination;

import org.activecluster.ClusterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.RestartableClusterFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.io.Connection;
import org.codehaus.wadi.io.PeerConfig;
import org.codehaus.wadi.io.ServerConfig;
import org.codehaus.wadi.io.impl.BIOServer;
import org.codehaus.wadi.io.impl.ClusterServer;
import org.codehaus.wadi.io.impl.NIOServer;
import org.codehaus.wadi.io.impl.Peer;
import org.codehaus.wadi.io.impl.SocketClientConnection;
import org.codehaus.wadi.io.impl.ThreadFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import junit.framework.TestCase;

public class TestServers extends TestCase {
    
    protected Log _log = LogFactory.getLog(getClass());
    
    public TestServers(String name) {
        super(name);
    }
    
    interface ConnectionFactory { Connection create() throws IOException; }

    protected InetSocketAddress _bioAddress;
    protected BIOServer _bioServer;
    protected ConnectionFactory _bioConnectionFactory=new ConnectionFactory(){ public Connection create() throws IOException {return new SocketClientConnection(_bioAddress, 5*1000);}};
    protected InetSocketAddress _nioAddress;
    protected NIOServer _nioServer;
    protected ConnectionFactory _nioConnectionFactory=new ConnectionFactory() {public Connection create() throws IOException {return new SocketClientConnection(_nioAddress, 5*1000);}};
    protected javax.jms.ConnectionFactory _connectionFactory=Utils.getConnectionFactory();
    protected ClusterFactory _clusterFactory=new RestartableClusterFactory(new CustomClusterFactory(_connectionFactory));
    protected String _clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
    protected ExtendedCluster _cluster;
    protected ClusterServer _clusterServer;
    protected ConnectionFactory _clusterConnectionFactory;
    

    protected final int _count=10000;
    
    
    protected void setUp() throws Exception {
        super.setUp();
        // an unbounded queue, serviced by 5 threads
        PooledExecutor pool;
        pool=new PooledExecutor(new LinkedQueue());
        pool.setKeepAliveTime(-1); // live forever
        pool.createThreads(5);

        ThreadFactory threadFactory=new ThreadFactory();
        
        _bioAddress=new InetSocketAddress(InetAddress.getLocalHost(), 8888);
        PooledExecutor executor;
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setThreadFactory(threadFactory);
        executor.setMinimumPoolSize(3);
        _bioServer=new BIOServer(executor, 5*1000, _bioAddress, 1*1000, 16);
        _bioServer.start();
        
        _nioAddress=new InetSocketAddress(InetAddress.getLocalHost(), 8889);
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setThreadFactory(threadFactory);
        executor.setMinimumPoolSize(3);
        _nioServer=new NIOServer(executor, 5*1000, _nioAddress, 1*1000, 1024, 256, 256);
        _nioServer.start();
        
        _cluster=(ExtendedCluster)_clusterFactory.createCluster(_clusterName);
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setThreadFactory(threadFactory);
        executor.setMinimumPoolSize(3);
        _clusterServer=new ClusterServer(executor, 5*1000, false);
        _clusterServer.init(new ServerConfig() { public ExtendedCluster getCluster() {return _cluster;}});
        _clusterServer.start();
        _cluster.start();
        
        _clusterConnectionFactory=new ConnectionFactory()  {
            protected int _count=0;
            public Connection create() throws IOException {
                String name="foo-"+(_count++);
                Destination target=_cluster.getLocalNode().getDestination();
                return _clusterServer.makeClientConnection(name, target);
            }
        };
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        _nioServer.stop();
        _bioServer.stop();
        _clusterServer.stop();
        _cluster.stop();
    }

    public static class SingleRoundTripServerPeer extends Peer {
        
        public void run(PeerConfig config) {
            try {
                //_log.info("server - starting");
                //_log.info("server - creating output stream");
                ObjectOutputStream oos=new ObjectOutputStream(config.getOutputStream());
                //_log.info("server - writing response");
                oos.writeBoolean(true); // ack
                //_log.info("server - flushing response");
                oos.flush();
                //_log.info("server - finished");
            } catch (IOException e) {
                //_log.error("problem", e);
            }
        }
    }
    
    public static class SingleRoundTripClientPeer extends Peer {
        
        public void run(PeerConfig config) {
            try {
                //_log.info("client - starting");
                //_log.info("client - creating output stream");
                ObjectOutputStream oos=new ObjectOutputStream(config.getOutputStream());
                //_log.info("client - writing object");
                oos.writeObject(new SingleRoundTripServerPeer());
                //_log.info("client - flushing object");
                oos.flush();
                //_log.info("client - creating input stream");
                ObjectInputStream ois=new ObjectInputStream(config.getInputStream());
                //_log.info("client - reading response");
                boolean result=ois.readBoolean();
                //_log.info("client - finished: "+result);
                assertTrue(result);
            } catch (IOException e) {
                //_log.error("problem", e);
            }
        }
    }
    
    // NEED CONCURRENT TEST
    
    public void testSingleRoundTrip() throws Exception {
        //testSingleRoundTrip("BIO", _bioConnectionFactory);
        //testSingleRoundTrip("NIO", _nioConnectionFactory);
        testSingleRoundTrip("Cluster", _clusterConnectionFactory);
    }
    
    public void testSingleRoundTrip(String info, ConnectionFactory factory) throws Exception {
        long start=System.currentTimeMillis();
        for (int i=0; i<_count; i++) {
            Connection connection=factory.create();
            Peer peer=new SingleRoundTripClientPeer();
            connection.run(peer);
            connection.close();
            //_log.info("count: "+i);
        }
        long elapsed=System.currentTimeMillis()-start;
        _log.info(info+" rate="+(_count*1000/elapsed)+" round-trips/second");
    }

    public void testMultipleRoundTrip() throws Exception {
        //testMultipleRoundTrip("BIO", _bioConnectionFactory);
        //testMultipleRoundTrip("NIO", _nioConnectionFactory);
        testMultipleRoundTrip("Cluster", _clusterConnectionFactory);
    }
    
    public void testMultipleRoundTrip(String info, ConnectionFactory factory) throws Exception {
        long start=System.currentTimeMillis();
        Connection connection=factory.create();
        Peer peer=new SingleRoundTripClientPeer();
        for (int i=0; i<_count; i++) {
            connection.run(peer);
            //_log.info("count: "+i);
        }
        connection.close();
        long elapsed=System.currentTimeMillis()-start;
        _log.info(info+" rate="+(_count*1000/(elapsed+1))+" round-trips/second");
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
