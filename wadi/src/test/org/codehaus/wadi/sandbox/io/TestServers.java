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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.activecluster.ClusterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Cluster;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.RestartableClusterFactory;
import org.codehaus.wadi.impl.Utils;

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
    protected ConnectionFactory _bio2BioConnectionFactory=new ConnectionFactory(){ public Connection create() throws IOException {return _bioServer.makeClientConnection(new Socket(_bioAddress.getAddress(), _bioAddress.getPort()));}};
    protected ConnectionFactory _bio2NioConnectionFactory=new ConnectionFactory(){ public Connection create() throws IOException {return _bioServer.makeClientConnection(new Socket(_nioAddress.getAddress(), _nioAddress.getPort()));}};
    protected InetSocketAddress _nioAddress;
    protected NIOServer _nioServer;
    protected ConnectionFactory _nio2BioConnectionFactory=new ConnectionFactory() {public Connection create() throws IOException {return _nioServer.makeClientConnection(SocketChannel.open(_bioAddress));}};
    protected ConnectionFactory _nio2NioConnectionFactory=new ConnectionFactory() {public Connection create() throws IOException {return _nioServer.makeClientConnection(SocketChannel.open(_nioAddress));}};
    protected javax.jms.ConnectionFactory _connectionFactory=Utils.getConnectionFactory();
    protected ClusterFactory _clusterFactory=new RestartableClusterFactory(new CustomClusterFactory(_connectionFactory));
    protected String _clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
    protected Cluster _cluster0;
    protected ClusterServer _clusterServer0;
    protected Cluster _cluster1;
    protected ClusterServer _clusterServer1;
    protected ConnectionFactory _clusterConnectionFactory;
    

    protected final int _count=10000;
    
    
    protected void setUp() throws Exception {
        super.setUp();
        // an unbounded queue, serviced by 5 threads
        PooledExecutor pool;
        pool=new PooledExecutor(new LinkedQueue());
        pool.setKeepAliveTime(-1); // live forever
        pool.createThreads(5);
        
        _bioAddress=new InetSocketAddress(InetAddress.getLocalHost(), 8888);
        PooledExecutor executor;
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setMinimumPoolSize(3);
        _bioServer=new BIOServer(executor, _bioAddress, 16, 1); // backlog, timeout
        _bioServer.start();
        
        _nioAddress=new InetSocketAddress(InetAddress.getLocalHost(), 8889);
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setMinimumPoolSize(3);
        _nioServer=new NIOServer(executor, _nioAddress, 1024, 256); // bufSize should be 4096*2
        _nioServer.start();
        
        _cluster0=(Cluster)_clusterFactory.createCluster(_clusterName);
        _cluster1=(Cluster)_clusterFactory.createCluster(_clusterName);
        executor=new PooledExecutor(new BoundedBuffer(10), 100);
        executor.setMinimumPoolSize(3);
        _clusterServer0=new ClusterServer(executor, _cluster0, true);
        _clusterServer0.start();
        _cluster0.start();
        
        _clusterServer1=new ClusterServer(executor, _cluster1, true);
        _clusterServer1.start();
        _cluster1.start();
        
        _clusterConnectionFactory=new ConnectionFactory()  {
            protected int _count=0;
            public Connection create() throws IOException {
                return _clusterServer1.makeClientConnection("foo-"+(_count++), _cluster0.getLocalNode().getDestination());
            }
        };
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        _nioServer.stop();
        _bioServer.stop();
        _clusterServer0.stop();
        _cluster0.stop();
        _clusterServer1.stop();
        _cluster1.stop();
    }

    public static class SingleRoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripServerPeer.class);
        
        public void process(InputStream is, OutputStream os) {
            try {
                //_log.info("server - starting");
                //_log.info("server - creating output stream");
                ObjectOutputStream oos=new ObjectOutputStream(os);
                //_log.info("server - writing response");
                oos.writeBoolean(true); // ack
                //_log.info("server - flushing response");
                oos.flush();
                //_log.info("server - finished");
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public static class SingleRoundTripClientPeer extends Peer {
        
        public void process(InputStream is, OutputStream os) {
            try {
                //_log.info("client - starting");
                //_log.info("client - creating output stream");
                ObjectOutputStream oos=new ObjectOutputStream(os);
                //_log.info("client - writing object");
                oos.writeObject(new SingleRoundTripServerPeer());
                //_log.info("client - flushing object");
                oos.flush();
                //_log.info("client - creating input stream");
                ObjectInputStream ois=new ObjectInputStream(is);
                //_log.info("client - reading response");
                boolean result=ois.readBoolean();
                //_log.info("client - finished: "+result);
                assertTrue(result);
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    // NEED CONCURRENT TEST
    
    public void testSingleRoundTrip() throws Exception {
        testSingleRoundTrip("BIO2BIO", _bio2BioConnectionFactory);
        testSingleRoundTrip("BIO2NIO", _bio2NioConnectionFactory);
        //testSingleRoundTrip("NIO2BIO", _nio2BioConnectionFactory);
        //testSingleRoundTrip("NIO2NIO", _nio2NioConnectionFactory);
        testSingleRoundTrip("Cluster", _clusterConnectionFactory);
    }
    
    public void testSingleRoundTrip(String info, ConnectionFactory factory) throws Exception {
        long start=System.currentTimeMillis();
        for (int i=0; i<_count; i++) {
            Connection connection=factory.create();
            Peer peer=new SingleRoundTripClientPeer();
            connection.process(peer);
            connection.close();
            //_log.info("count: "+i);
        }
        long elapsed=System.currentTimeMillis()-start;
        _log.info(info+" rate="+(_count*1000/elapsed)+" round-trips/second");
    }
    
    public static class MultipleRoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripServerPeer.class);
        
        public void process(InputStream is, OutputStream os) {
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
        
        public MultipleRoundTripClientPeer(int numTrips) throws IOException {
            super();
            _numTrips=numTrips;
        }
        
        public void process(InputStream is, OutputStream os) {
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
        testMultipleRoundTrip("BIO2BIO", _bio2BioConnectionFactory);
        testMultipleRoundTrip("BIO2NIO", _bio2NioConnectionFactory);
        //testMultipleRoundTrip("NIO", _nioConnectionFactory);
        testMultipleRoundTrip("Cluster", _clusterConnectionFactory);
    }
    
    public void testMultipleRoundTrip(String info, ConnectionFactory factory) throws Exception {
        long start=System.currentTimeMillis();
        Peer peer=new SingleRoundTripClientPeer();
        Connection connection=factory.create();
        connection.process(peer);
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
