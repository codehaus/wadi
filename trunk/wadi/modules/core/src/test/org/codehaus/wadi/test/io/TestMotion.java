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
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activecluster.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Config;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.DistributableSessionConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.HttpServletRequestWrapperPool;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RelocaterConfig;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionRelocater;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.DistributableSession;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyHttpServletRequest;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SimpleMotable;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.StreamingMigratingRelocater;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.io.Pipe;
import org.codehaus.wadi.io.PeerConfig;
import org.codehaus.wadi.io.Server;
import org.codehaus.wadi.io.ServerConfig;
import org.codehaus.wadi.io.impl.ClusterServer;
import org.codehaus.wadi.io.impl.Peer;
import org.codehaus.wadi.io.impl.ThreadFactory;
import org.codehaus.wadi.test.DummyDistributableSessionConfig;
import org.codehaus.wadi.test.EtherEmoter;
import org.codehaus.wadi.test.MyDummyHttpServletRequestWrapperPool;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class TestMotion extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass());
    
    protected final ThreadFactory _threadFactory=new ThreadFactory();
    
    interface Location2 {/* empty */}
    
    public static class InetSocketAddressLocation implements Location2 {
        
        protected final InetSocketAddress _address;
        
        public InetSocketAddressLocation(InetSocketAddress address) {
            _address=address;    
        }
        
        public InetSocketAddress getAddress() {return _address;}
    }
    
    public static class DestinationLocation implements Location2 {
        
        protected final Destination _destination;
        
        public DestinationLocation(Destination destination) {
            _destination=destination;
        }
        
        public Destination getDestination() {return _destination;}
    }
    
    
    public static class MyServerConfig implements ServerConfig {
        
        protected static ConnectionFactory _cfactory=Utils.getConnectionFactory();
        protected static CustomClusterFactory _factory=new CustomClusterFactory(_cfactory);

        protected final Log _log=LogFactory.getLog(getClass());
        protected final String _nodeName;
        protected final Contextualiser _contextualiser;
        
        protected ExtendedCluster _cluster=null;
        
        public MyServerConfig(String nodeName, Contextualiser contextualiser) {
            _nodeName=nodeName;
            _contextualiser=contextualiser;
            try {
                _cluster=(ExtendedCluster)_factory.createCluster("ORG.CODEHAUS.WADI.TEST.CLUSTER");
            } catch (Exception e) {
                _log.error("unexpected problem", e);
            }
        }
        
        public ExtendedCluster getCluster() {
            return _cluster;
        }
        
        public Contextualiser getContextualiser() {
            return _contextualiser;
        }
        
        public String getNodeName() {
            return _nodeName;
        }
        
    }
    
    public static class Node {
        
        protected final Map _clients=new HashMap();
        protected final PooledExecutor _executor;
        protected final Server _server;
        
        protected int _counter=0;
        
        public Node(Location2 location, ThreadFactory factory, ServerConfig config) {
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
        
        public Pipe getClient(Location2 location) throws IOException {
//            synchronized (_clients) {
//                Pipe client=(Pipe)_clients.get(location);
//                if (client==null) {
//                    client=((ClusterServer)_server).makeClientPipe("foo-"+_counter++, ((DestinationLocation)location).getDestination());
//                    //client=new SocketClientConnection(((InetSocketAddressLocation)location).getAddress(), 5*1000);
//                    //_clients.put(location, client); // don't cache clients..
//                }
//                return client;
//            }
            return ((ClusterServer)_server).makeClientPipe("foo-"+_counter++, ((DestinationLocation)location).getDestination());
        }
    }

    protected final DistributableSessionFactory _distributableSessionFactory=new DistributableSessionFactory();
    protected final SessionPool _distributableSessionPool=new SimpleSessionPool(_distributableSessionFactory);
    protected final HttpServletRequestWrapperPool _requestPool=new MyDummyHttpServletRequestWrapperPool();
    protected final ContextPool _distributableContextPool=new SessionToContextPoolAdapter(_distributableSessionPool);
    
    protected final Location2 _localLocation;
    protected final Map _localMap=new HashMap();
    protected final MemoryContextualiser _localContextualiser=new MemoryContextualiser(new DummyContextualiser(), new NeverEvicter(60, true), _localMap, new SimpleStreamer(), _distributableContextPool, _requestPool);
    protected final ServerConfig _localConfig=new MyServerConfig("local", _localContextualiser);
    protected final Location2 _remoteLocation;
    protected final Map _remoteMap=new HashMap();
    protected final MemoryContextualiser _remoteContextualiser=new MemoryContextualiser(new DummyContextualiser(), new NeverEvicter(60, true), _remoteMap, new SimpleStreamer(), _distributableContextPool, _requestPool);
    protected final ServerConfig _remoteConfig=new MyServerConfig("remote", _remoteContextualiser);
    
    public TestMotion(String name) throws Exception {
        super(name);
        _distributableSessionPool.init(new DummyDistributableSessionConfig());
        _localLocation=new DestinationLocation(_localConfig.getCluster().getLocalNode().getDestination());
        _remoteLocation=new DestinationLocation(_remoteConfig.getCluster().getLocalNode().getDestination());
        //_local=new InetSocketAddressLocation(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
        //_remote=new InetSocketAddressLocation(new InetSocketAddress(InetAddress.getLocalHost(), 8889));
    }

    protected Node _us;
    protected Node _them;

    protected void setUp() throws Exception {
        super.setUp();
        (_us=new Node(_localLocation, _threadFactory, _localConfig)).start();
        (_them=new Node(_remoteLocation, _threadFactory, _remoteConfig)).start();
        _localConfig.getCluster().start();
        _remoteConfig.getCluster().start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        _them.stop();
        _us.stop();
        _remoteConfig.getCluster().stop();
        _localConfig.getCluster().stop();
        _localMap.clear();
        _remoteMap.clear();
    }

    public static class SingleRoundTripServerPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripServerPeer.class);
        
        public boolean run(PeerConfig config) throws IOException {
            _log.info("server - starting");
            _log.info("server - creating output stream");
            ObjectOutputStream oos=config.getObjectOutputStream();
            _log.info("server - writing response");
            oos.writeBoolean(true); // ack
            _log.info("server - flushing response");
            oos.flush();
            _log.info("server - finished");
            //config.close();
            return true;
        }
    }
    
    public static class SingleRoundTripClientPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(SingleRoundTripClientPeer.class);
        
        public boolean run(PeerConfig config) throws IOException {
            _log.info("client - starting");
            _log.info("client - creating output stream");
            ObjectOutputStream oos=config.getObjectOutputStream();
            _log.info("client - writing server");
            oos.writeObject(new SingleRoundTripServerPeer());
            _log.info("client - flushing server");
            oos.flush();
            _log.info("client - creating input stream");
            ObjectInputStream ois=config.getObjectInputStream();
            _log.info("client - reading response");
            boolean result=ois.readBoolean();
            _log.info("client - finished: "+result);
            assertTrue(result);
            //config.close();
            return result;
        }
    }
    
    public void testRoundTripping() throws Exception {
        
        _log.info("START");
        Pipe us2them=_us.getClient(_remoteLocation);
        try {
            _log.info("us -> them (1st trip)");
            us2them.run(new SingleRoundTripClientPeer());
            _log.info("us -> them (2nd trip)");
            us2them.run(new SingleRoundTripClientPeer());
        } finally {
            us2them.close();
        }
        _log.info("FINISH");
        
        _log.info("START");
        Pipe them2us=_them.getClient(_localLocation);
        try {
            _log.info("them -> us (1st trip)");
            them2us.run(new SingleRoundTripClientPeer());
            _log.info("them -> us (2nd trip)");
            them2us.run(new SingleRoundTripClientPeer());
        } finally {
            them2us.close();
        }
        _log.info("FINISH");
    }

    public static class EmotionServerPeer extends Peer implements Serializable {
        
        protected static final Log _log=LogFactory.getLog(EmotionServerPeer.class);
        
        public boolean run(PeerConfig config) throws IOException, ClassNotFoundException {
            long startTime=System.currentTimeMillis();
            ObjectInputStream ois=config.getObjectInputStream();
            String nodeName=(String)ois.readObject();
            String name=(String)ois.readObject();
            Motable emotable=(Motable)ois.readObject();
            Contextualiser contextualiser=config.getContextualiser();
            Immoter immoter=contextualiser.getDemoter(name, emotable);
            Motable immotable=immoter.nextMotable(name, emotable);
            ObjectOutputStream oos=config.getObjectOutputStream();
            boolean ok=immoter.prepare(name, emotable, immotable);
            if (ok) immoter.commit(name, immotable);
            oos.writeObject(config.getNodeId());
            oos.writeBoolean(ok);
            oos.flush();
            long elapsedTime=System.currentTimeMillis()-startTime;
            if (_log.isDebugEnabled())_log.debug("motion"+(ok?"":" failed")+": "+name+" : cluster ["+nodeName+"] -> "+immoter.getInfo()+" ("+elapsedTime+" millis)");
            return true;
        }   
    }
    
    public static class EmotionClientPeer extends Peer {
        
        protected static final Log _log=LogFactory.getLog(EmotionClientPeer.class);
        
        protected final String _name;
        protected final Emoter _emoter;
        protected final Motable _emotable;
        
        public EmotionClientPeer(String name, Emoter emoter, Motable emotable) {
            _name=name;
            _emoter=emoter;
            _emotable=emotable;
        }
        
        public boolean run(PeerConfig config) throws Exception {
            long startTime=System.currentTimeMillis();
            Motable motable=new SimpleMotable();
            motable.copy(_emotable); // how can we avoid this copy...? write straight onto the stream...
            boolean ok=_emoter.prepare(_name, _emotable, null); // FIXME - this should not be a null
            if (!ok) return ok;
            ok=false;
            ObjectOutputStream oos=null;
            ObjectInputStream ois=null;
            String nodeName="<unknown>";
            try {
                oos=config.getObjectOutputStream();
                oos.writeObject(new EmotionServerPeer()); // could be cached...
                oos.writeObject(config.getNodeId());
                oos.writeObject(_name);
                oos.writeObject(motable);
                oos.flush();
                // server tries to prepare and commit...
                // returns success or failure
                ois=config.getObjectInputStream();
                nodeName=(String)ois.readObject();
                ok=ois.readBoolean();
            } catch (Exception e) {
                _log.error("unexpected problem", e);
            } finally {
                if (ok) {
                    _emoter.commit(_name, _emotable);
                } else {
                    _emoter.rollback(_name, _emotable);
                }
            }
            long elapsedTime=System.currentTimeMillis()-startTime;
            if (_log.isDebugEnabled())_log.debug("motion"+(ok?"":" failed")+": "+_name+" : "+_emoter.getInfo()+" -> cluster ["+nodeName+"] ("+elapsedTime+" millis)");
            return ok;
        }
    }
    
    public static class DummyRelocaterConfig implements RelocaterConfig {

        public Collapser getCollapser() {return null;}
        public Dispatcher getDispatcher() {return null;}
        public Location getLocation() {return null;}
        public Map getMap() {return null;}
        public Cluster getCluster() {return null;}
        public Contextualiser getContextualiser() {return null;}
        public Server getServer() {return null;}
        public String getNodeName() {return null;}
        public SynchronizedBoolean getShuttingDown() {return null;}
        public HttpProxy getHttpProxy() {return null;}
        public InetSocketAddress getHttpAddress() {return null;}
        public DIndex getDIndex() {return null;}
        public void notifySessionRelocation(String name) {};
        
    }
    
    public void testEmotion() throws Exception {
        
        Emoter emoter=new EtherEmoter();
        DistributableSessionConfig config=new DummyDistributableSessionConfig();
        DistributableSessionFactory factory=new DistributableSessionFactory();
        DistributableSession s0=(DistributableSession)factory.create(config);
        long time=System.currentTimeMillis();
        String name0="foo";
        s0.init(time, time, 30*60, name0);
        DistributableSession s1=(DistributableSession)factory.create(config);
        String name1="bar";
        s1.init(time, time, 30*60, name1);

        _log.info("START");
        Pipe us2them=_us.getClient(_remoteLocation);
        try {
            _log.info("us -> them (1st trip)");
            us2them.run(new EmotionClientPeer(s0.getName(), emoter, s0));
            assertTrue(_localMap.size()==0);
            assertTrue(_remoteMap.size()==1);
            assertTrue(_remoteMap.containsKey(name0));
            _log.info("us -> them (2nd trip)");
            us2them.run(new EmotionClientPeer(s1.getName(), emoter, s1));
            assertTrue(_localMap.size()==0);
            assertTrue(_remoteMap.size()==2);
            assertTrue(_remoteMap.containsKey(name1));
        } finally {
            us2them.close();
        }
        _log.info("FINISH");
        
        SessionRelocater relocater=new StreamingMigratingRelocater();
        relocater.init(new DummyRelocaterConfig());
        Sync motionLock=new NullSync();
        Map locationMap=new HashMap();
        HttpServletRequest req=null;
        HttpServletResponse res=null;
        FilterChain chain=null;
        Immoter immoter=_localContextualiser.getImmoter();

        boolean ok=false;
        assertTrue(_localMap.size()==0);
        assertTrue(_remoteMap.size()==2);
        ok=relocater.relocate(req, res, chain, name0, immoter, motionLock);
        assertTrue(ok);
        assertTrue(_localMap.size()==1);
        assertTrue(_remoteMap.size()==1);
        assertTrue(_localMap.containsKey(name0));
        assertTrue(!_remoteMap.containsKey(name0));
        ok=relocater.relocate(req, res, chain, name1, immoter, motionLock);
        assertTrue(ok);
        assertTrue(_localMap.size()==2);
        assertTrue(_remoteMap.size()==0);
        assertTrue(_localMap.containsKey(name1));
        assertTrue(!_remoteMap.containsKey(name1));
    }
    
}
