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
package org.codehaus.wadi.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.activecluster.ClusterFactory;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnection;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.BrokerConnector;
import org.activemq.broker.BrokerContainer;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.activemq.transport.TransportChannel;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.DistributableSessionConfig;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.dindex.DIndexConfig;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.io.Server;
import org.codehaus.wadi.io.ServerConfig;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

public class DistributableManager extends StandardManager implements DistributableSessionConfig, DistributableContextualiserConfig, ServerConfig, DispatcherConfig, DIndexConfig {

    protected final Map _distributedState=new HashMap(); // TODO - make this a SynchronisedMap
    protected final SynchronizedBoolean _shuttingDown=new SynchronizedBoolean(false);
    
    protected final Dispatcher _dispatcher=new Dispatcher();
    
    protected final Streamer _streamer;
    protected final String _clusterUri;
    protected final String _clusterName;
    protected final String _nodeName;
    protected final HttpProxy _httpProxy;
    protected final InetSocketAddress _httpAddress;
    protected final boolean _accessOnLoad=true; // TODO - parameterise...
    protected final int _numBuckets;

    public DistributableManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, Streamer streamer, boolean accessOnLoad, String clusterUri, String clusterName, String nodeName, HttpProxy httpProxy, InetSocketAddress httpAddress, int numBuckets) {
        super(sessionPool, attributesFactory, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, sessionMap, router);
        _streamer=streamer;
        _clusterUri=clusterUri;
        _clusterName=clusterName;
        _nodeName=nodeName;
        _httpProxy=httpProxy;
        _httpAddress=httpAddress;
        _numBuckets=numBuckets;
    }

    public String getContextPath() { // TODO - integrate with Jetty/Tomcat
        return "/";
    }

    protected ActiveMQConnectionFactory _connectionFactory;
    protected CustomClusterFactory _clusterFactory;
    protected ExtendedCluster _cluster;
    protected DIndex _dindex;
    
    public void init() {
        // must be done before super.init() so that ContextualiserConfig contains a Cluster
        try {
            _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
            _connectionFactory.start();
            System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName());
            _clusterFactory=new CustomClusterFactory(_connectionFactory);
            _cluster=(ExtendedCluster)_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
            _dispatcher.init(this);
            _distributedState.put("name", _nodeName);
            _distributedState.put("http", _httpAddress);
            _dindex=new DIndex(_nodeName, _numBuckets, _clusterFactory.getInactiveTime(), _cluster, _dispatcher, _distributedState);
            _dindex.init(this);
        } catch (Exception e) {
            _log.error("problem starting Cluster", e);
        }
        super.init();
    }

    public void start() throws Exception {
        _cluster.getLocalNode().setState(_distributedState);
        _cluster.start();
        _dindex.start();
        super.start();
    }

    public void stop() throws Exception {
        _shuttingDown.set(true);
        super.stop();
        _dindex.stop();
        _cluster.stop();
        _connectionFactory.stop();
        
        // shut down activemq cleanly - what happens if we are running more than one distributable webapp ?
        // there must be an easier way - :-(
        ActiveMQConnection connection=(ActiveMQConnection)_cluster.getConnection();
        TransportChannel channel=(connection==null?null:connection.getTransportChannel());
        BrokerConnector connector=(channel==null?null:channel.getEmbeddedBrokerConnector());
        BrokerContainer container=(connector==null?null:connector.getBrokerContainer());
        if (container!=null)
            container.stop(); // for peer://
        
        Thread.sleep(5*1000);
    }

    // Distributable
    public Streamer getStreamer() {return _streamer;}

    static class HelperPair {

        final Class _type;
        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
            _type=type;
            _helper=helper;
        }
    }

    protected final List _helpers=new ArrayList();

    /**
     * Register a ValueHelper for a particular type. During [de]serialisation
     * Objects flowing in/out of the persistance medium will be passed through this
     * Helper, which will have the opportunity to convert them between Serializable
     * and non-Serializable representations. Helpers will be returned in their registration
     * order, so this is significant (as an Object may implement more than one interface
     * or registered type).
     *
     * @param type
     * @param helper
     */

    public void registerHelper(Class type, ValueHelper helper) {
        _helpers.add(new HelperPair(type, helper));
    }

    public boolean deregisterHelper(Class type) {
        int l=_helpers.size();
        for (int i=0; i<l; i++)
            if (type.equals(((HelperPair)_helpers.get(i))._type)) {
                _helpers.remove(i);
                return true;
            }
        return false;
    }

    public ValueHelper findHelper(Class type) {
        int l=_helpers.size();
        for (int i=0; i<l; i++) {
            HelperPair p=(HelperPair)_helpers.get(i);
            if (p._type.isAssignableFrom(type))
                return p._helper;
        }
        return null;
    }

    public void destroy(Session session) {
        // this destroySession method must not chain the one in super - otherwise the
        // notification aspect fires twice - once around each invocation... - DOH !
        Collection names=new ArrayList((_attributeListeners.size()>0)?(Collection)session.getAttributeNameSet():((DistributableSession)session).getListenerNames());
        for (Iterator i=names.iterator(); i.hasNext();) // ALLOC ?
            session.removeAttribute((String)i.next());
        
        // TODO - remove from Contextualiser....at end of initial request ? Think more about this
        String name=session.getName();
        notifySessionDeletion(name);
        _map.remove(name);
        session.destroy();
        _sessionPool.put(session);
        if (_log.isDebugEnabled()) _log.debug("destroyed: "+name);
    }
    
    // Lazy
    
    public boolean getHttpSessionAttributeListenersRegistered(){return _attributeListeners.size()>0;}
    
    public boolean getDistributable(){return true;}
    
    // ServerConfig
    
    public ExtendedCluster getCluster() {return _cluster;}
    
    // DistributableContextualiserConfig
    
    public Server getServer() {throw new UnsupportedOperationException();}
    public String getNodeName() {return _nodeName;} // NYI
    
    public HttpProxy getHttpProxy() {
        return _httpProxy;
    }
    
    public InetSocketAddress getHttpAddress() {
        return _httpAddress;
    }
    
    public Object getDistributedState(Object key) {
        synchronized (_distributedState) {
            return _distributedState.get(key);
        }
    }
    
    public Object putDistributedState(Object key, Object newValue) {
        synchronized (_distributedState) {
            return _distributedState.put(key, newValue);
        }
    }
    
    public Object removeDistributedState(Object key) {
        synchronized (_distributedState) {
            return _distributedState.remove(key);
        }
    }
    
    public void distributeState() throws JMSException {
        _cluster.getLocalNode().setState(_distributedState);
    }
    
    public boolean getAccessOnLoad() {
        return _accessOnLoad;
    }
    
    public SynchronizedBoolean getShuttingDown() {
        return _shuttingDown;
    }

    public Map getDistributedState() {
        return _distributedState;
    }

    public long getInactiveTime() {
        return ((DefaultClusterFactory)_clusterFactory).getInactiveTime();
    }

    public int getNumBuckets() {
        return 72; // TODO - parameterise...
    }
    
    public Dispatcher getDispatcher() {
        return _dispatcher;
    }
    
    public DIndex getDIndex() {
        return _dindex;
    }
    
    public void notifySessionInsertion(String name) {
        super.notifySessionInsertion(name);
        _dindex.insert(name);
    }
    
    public void notifySessionDeletion(String name) {
        super.notifySessionDeletion(name);
        _dindex.remove(name);
    }

    public void notifySessionRelocation(String  name) {
        super.notifySessionRelocation(name);
        _dindex.relocate(name);
    }

    // DIndexConfig
    
    public void findRelevantSessionNames(int numBuckets, Collection[] resultSet) {
        _log.info("findRelevantSessionNames");
        _contextualiser.findRelevantSessionNames(numBuckets, resultSet);
    }
    
}

