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
import java.util.Map;

import javax.jms.JMSException;

import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnection;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.BrokerConnector;
import org.activemq.broker.BrokerContainer;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.activemq.transport.TransportChannel;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.ClusteredContextualiserConfig;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.Replicater;
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


public class ClusteredManager extends DistributableManager implements ClusteredContextualiserConfig, ServerConfig, DispatcherConfig, DIndexConfig {

    protected final Map _distributedState=new HashMap(); // TODO - make this a SynchronisedMap
    protected final Dispatcher _dispatcher=new Dispatcher();

    protected final String _clusterUri;
    protected final String _clusterName;
    protected final String _nodeName;
    protected final int _numBuckets;

    public ClusteredManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, Streamer streamer, boolean accessOnLoad, Replicater replicater, InetSocketAddress httpAddress, HttpProxy httpProxy, String clusterUri, String clusterName, String nodeName, int numBuckets) {
        super(sessionPool, attributesFactory, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, sessionMap, router, streamer, accessOnLoad, replicater);
    	_httpAddress=httpAddress;
    	_httpProxy=httpProxy;
        _clusterUri=clusterUri;
        _clusterName=clusterName;
        _nodeName=nodeName;
        _numBuckets=numBuckets;
    }

    public String getContextPath() { // TODO - integrate with Jetty/Tomcat
        return "/";
    }

    protected ActiveMQConnectionFactory _connectionFactory;
    protected CustomClusterFactory _clusterFactory;
    protected ExtendedCluster _cluster;
    protected DIndex _dindex;
	protected final HttpProxy _httpProxy;
	protected final InetSocketAddress _httpAddress;

    public void init(ManagerConfig config) {
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
        super.init(config);
    }
    
    public void start() throws Exception {
    	_cluster.getLocalNode().setState(_distributedState);
    	_log.trace("distributed state updated: "+_cluster.getLocalNode().getState());
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

    static class HelperPair {

        final Class _type;
        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
            _type=type;
            _helper=helper;
        }
    }

    public void destroy(Session session) {
        // this destroySession method must not chain the one in super - otherwise the
        // notification aspect fires twice - once around each invocation... - DOH !
        Collection names=new ArrayList((_attributeListeners.length>0)?(Collection)session.getAttributeNameSet():((DistributableSession)session).getListenerNames());
        for (Iterator i=names.iterator(); i.hasNext();) // ALLOC ?
            session.removeAttribute((String)i.next());

        // TODO - remove from Contextualiser....at end of initial request ? Think more about this
        String name=session.getName();
        notifySessionDeletion(name);
        _map.remove(name);
        try {
        	session.destroy();
        } catch (Exception e) {
        	_log.warn("unexpected problem destroying session", e);
        }
        _sessionPool.put(session);
        if (_log.isDebugEnabled()) _log.debug("destroyed: "+name);
    }

    // Lazy

    public ExtendedCluster getCluster() {return _cluster;}

    // DistributableContextualiserConfig

    public Server getServer() {throw new UnsupportedOperationException();}
    public String getNodeName() {return _nodeName;} // NYI

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
	_log.trace("distributed state updated: "+_cluster.getLocalNode().getState());
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

	public HttpProxy getHttpProxy() {
	    return _httpProxy;
	}

	public InetSocketAddress getHttpAddress() {
	    return _httpAddress;
	}

}

