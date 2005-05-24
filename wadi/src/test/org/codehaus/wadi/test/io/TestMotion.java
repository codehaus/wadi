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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.io.PeerConfig;
import org.codehaus.wadi.io.Server;
import org.codehaus.wadi.io.impl.NIOServer;
import org.codehaus.wadi.io.impl.Peer;
import org.codehaus.wadi.io.impl.SocketClientConnection;
import org.codehaus.wadi.io.impl.ThreadFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import junit.framework.TestCase;

public class TestMotion extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final ThreadFactory _threadFactory=new ThreadFactory();
    
    public static class Node {
        
        protected final PooledExecutor _executor;
        protected final Server _server; // started immediately...
        protected final Map _clients; // started lazily...
        
        public Node(InetSocketAddress localAddress, InetSocketAddress remoteAddress, ThreadFactory factory) {
            _executor=new PooledExecutor(new BoundedBuffer(10), 100);
            _executor.setThreadFactory(factory);
            _executor.setMinimumPoolSize(3);
            _server=new NIOServer(_executor, 5*1000, localAddress, 1*1000, 256, 4096, 4096);
            //_server=new BIOServer(_executor, localAddress, 32, 1*1000);
            // Cluster cluster=Utils.getConnectionFactory().createConnection();
            //_server=new ClusterServer(_executor, cluster,true;);
            _clients=new HashMap();
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
        
        public SocketClientConnection getClient(InetSocketAddress address) throws IOException {
            synchronized (_clients) {
                SocketClientConnection client=(SocketClientConnection)_clients.get(address);
                if (client==null) {
                    client=new SocketClientConnection(address, 5*1000);
                    _clients.put(address, client);
                }
                return client;
            }
            
        }
    }

    protected final InetSocketAddress _local;
    protected final InetSocketAddress _remote;
    
    public TestMotion(String name) throws Exception {
        super(name);
        _local=new InetSocketAddress(InetAddress.getLocalHost(), 8888);
        _remote=new InetSocketAddress(InetAddress.getLocalHost(), 8889);
    }

    protected Node _us;
    protected Node _them;

    protected void setUp() throws Exception {
        super.setUp();
        (_us=new Node(_local, _remote, _threadFactory)).start();
        (_them=new Node(_remote, _local, _threadFactory)).start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        _them.stop();
        _us.stop();
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
        SocketClientConnection us2them=_us.getClient(_remote);
        _log.info("us -> them (1st trip)");
        us2them.run(new SingleRoundTripClientPeer());
        _log.info("us -> them (2nd trip)");
        us2them.run(new SingleRoundTripClientPeer());
        us2them.close();
        
        SocketClientConnection them2us=_them.getClient(_local);
        _log.info("them -> us (1st trip)");
        them2us.run(new SingleRoundTripClientPeer());
        _log.info("them -> us (2nd trip)");
        them2us.run(new SingleRoundTripClientPeer());
        them2us.close();
    }
}
