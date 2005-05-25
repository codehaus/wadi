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
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.io.Pipe;
import org.codehaus.wadi.io.PeerConfig;
import org.codehaus.wadi.io.Server;
import org.codehaus.wadi.io.ServerConfig;
import org.codehaus.wadi.io.impl.ClusterServer;
import org.codehaus.wadi.io.impl.NIOServer;
import org.codehaus.wadi.io.impl.Peer;
import org.codehaus.wadi.io.impl.SocketClientPipe;
import org.codehaus.wadi.io.impl.ThreadFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import junit.framework.TestCase;

public class TestMotion extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final ThreadFactory _threadFactory=new ThreadFactory();
    
    interface Location {/* empty */};
    
    public static class InetSocketAddressLocation implements Location {
        
        protected final InetSocketAddress _address;
        
        public InetSocketAddressLocation(InetSocketAddress address) {
            _address=address;    
        }
        
        public InetSocketAddress getAddress() {return _address;}
    }
    
    public static class DestinationLocation implements Location {
        
        protected final Destination _destination;
        
        public DestinationLocation(Destination destination) {
            _destination=destination;
        }
        
        public Destination getDestination() {return _destination;}
    }
    
    
    public static class MyServerConfig implements ServerConfig {
        
        protected final Log _log=LogFactory.getLog(getClass());
        protected ExtendedCluster _cluster;
        protected static ConnectionFactory _cfactory=Utils.getConnectionFactory();
        protected static CustomClusterFactory _factory=new CustomClusterFactory(_cfactory);
        
        public MyServerConfig() {
            try {
                _cluster=(ExtendedCluster)_factory.createCluster("ORG.CODEHAUS.WADI.TEST.CLUSTER");
            } catch (Exception e) {
                _log.error("unexpected problem", e);
            }
        }
        
        public ExtendedCluster getCluster() {
            return _cluster;
        }
        
    }
    
    public static class Node {
        
        protected final Map _clients=new HashMap();
        protected final PooledExecutor _executor;
        protected final Server _server;
        
        protected int _counter=0;
        
        public Node(Location location, ThreadFactory factory, ServerConfig config) {
            _executor=new PooledExecutor(new BoundedBuffer(10), 100);
            _executor.setThreadFactory(factory);
            _executor.setMinimumPoolSize(3);
            //_server=new NIOServer(_executor, 5*1000, ((InetSocketAddressLocation)location).getAddress(), 1*1000, 256, 4096, 4096);
            //_server=new BIOServer(_executor, ((InetSocketAddressLocation)location).getAddress(), 32, 1*1000);
            _server=new ClusterServer(_executor, 5000, false);
            _server.init(config);
        }
        
        public void start() throws Exception {
            _server.start();
        }
        
        public void stop() throws Exception {
//            synchronized (_clients) {
//                for (Iterator i=_clients.entrySet().iterator(); i.hasNext(); ) {
//                    Map.Entry e=(Map.Entry)i.next();
//                    //InetSocketAddress key=(InetSocketAddress)e.getKey();
//                    SocketClientConnection val=(SocketClientConnection)e.getValue();
//                    val.close();
//                    i.remove();
//                }
//            }
            _server.stop();
        }
        
        public Pipe getClient(Location location) throws IOException {
            synchronized (_clients) {
                Pipe client=(Pipe)_clients.get(location);
                if (client==null) {
                    client=((ClusterServer)_server).makeClientPipe("foo-"+_counter++, ((DestinationLocation)location).getDestination());
                    //client=new SocketClientConnection(((InetSocketAddressLocation)location).getAddress(), 5*1000);
                    _clients.put(location, client);
                }
                return client;
            }
            
        }
    }

    protected final Location _local;
    protected final ServerConfig _localConfig=new MyServerConfig();
    protected final Location _remote;
    protected final ServerConfig _remoteConfig=new MyServerConfig();
    
    public TestMotion(String name) throws Exception {
        super(name);
        _local=new DestinationLocation(_localConfig.getCluster().getLocalNode().getDestination());
        _remote=new DestinationLocation(_remoteConfig.getCluster().getLocalNode().getDestination());
        //_local=new InetSocketAddressLocation(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
        //_remote=new InetSocketAddressLocation(new InetSocketAddress(InetAddress.getLocalHost(), 8889));
    }

    protected Node _us;
    protected Node _them;

    protected void setUp() throws Exception {
        super.setUp();
        (_us=new Node(_local, _threadFactory, _localConfig)).start();
        (_them=new Node(_remote, _threadFactory, _remoteConfig)).start();
        _localConfig.getCluster().start();
        _remoteConfig.getCluster().start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        _them.stop();
        _us.stop();
        _remoteConfig.getCluster().stop();
        _localConfig.getCluster().stop();
    }

    public static class SingleRoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripServerPeer.class);
        
        public void run(PeerConfig config) {
            try {
                _log.info("server - starting");
                _log.info("server - creating output stream");
                ObjectOutputStream oos=new ObjectOutputStream(config.getOutputStream());
                _log.info("server - writing response");
                oos.writeBoolean(true); // ack
                _log.info("server - flushing response");
                oos.flush();
                _log.info("server - finished");
                //config.close();
            } catch (IOException e) {
                _log.error(e);
            }
        }
    }
    
    public static class SingleRoundTripClientPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripClientPeer.class);
        
        public void run(PeerConfig config) {
            try {
                _log.info("client - starting");
                _log.info("client - creating output stream");
                ObjectOutputStream oos=new ObjectOutputStream(config.getOutputStream());
                _log.info("client - writing server");
                oos.writeObject(new SingleRoundTripServerPeer());
                _log.info("client - flushing server");
                oos.flush();
                _log.info("client - creating input stream");
                ObjectInputStream ois=new ObjectInputStream(config.getInputStream());
                _log.info("client - reading response");
                boolean result=ois.readBoolean();
                _log.info("client - finished: "+result);
                assertTrue(result);
                //config.close();
            } catch (IOException e) {
                _log.error("unexpected problem", e);
            }
        }
    }
    
    
    public void testMotion() throws Exception {
        Pipe us2them=_us.getClient(_remote);
        _log.info("us -> them (1st trip)");
        us2them.run(new SingleRoundTripClientPeer());
        _log.info("us -> them (2nd trip)");
        us2them.run(new SingleRoundTripClientPeer());
        us2them.close();
        
        Pipe them2us=_them.getClient(_local);
        _log.info("them -> us (1st trip)");
        them2us.run(new SingleRoundTripClientPeer());
        _log.info("them -> us (2nd trip)");
        them2us.run(new SingleRoundTripClientPeer());
        them2us.close();
    }
}
