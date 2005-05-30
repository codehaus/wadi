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
import org.activemq.ActiveMQConnection;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.DistributableSessionConfig;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.io.Server;
import org.codehaus.wadi.io.ServerConfig;

public class DistributableManager extends StandardManager implements DistributableSessionConfig, DistributableContextualiserConfig, ServerConfig {

    protected final Map _distributedState=new HashMap();
    protected final Streamer _streamer;
    protected final String _clusterName;
    protected final String _nodeName;
    protected final HttpProxy _httpProxy;
    protected final InetSocketAddress _httpAddress;

    public DistributableManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, Streamer streamer, boolean accessOnLoad, String clusterName, String nodeName, HttpProxy httpProxy, InetSocketAddress httpAddress) {
        super(sessionPool, attributesFactory, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, sessionMap, router, accessOnLoad);
        _streamer=streamer;
        _clusterName=clusterName;
        _nodeName=nodeName;
        _httpProxy=httpProxy;
        _httpAddress=httpAddress;
    }
    
    public String getContextPath() { // TODO - integrate with Jetty/Tomcat
        return "/";
    }
    
    protected ActiveMQConnectionFactory _connectionFactory;
    protected ClusterFactory _clusterFactory;
    protected ExtendedCluster _cluster;

    public void init() {
        // must be done before super.init() so that ContextualiserConfig contains a Cluster
        try {
            _connectionFactory=new ActiveMQConnectionFactory("peer://org.codehaus.wadi");
            _connectionFactory.start();
            System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName());
            _clusterFactory=new CustomClusterFactory(_connectionFactory);
            _cluster=(ExtendedCluster)_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
            _distributedState.put("name", _nodeName);
            _cluster.getLocalNode().setState(_distributedState);
        } catch (Exception e) {
            _log.error("problem starting Cluster", e);
        }
        super.init();
    }
    
    public void start() throws Exception {
        super.start();
        _cluster.start();
    }
    
    public void stop() throws Exception {
        super.stop();
        _cluster.stop();
        // shut down activemq cleanly - what happens if we are running more than one distributable webapp ?
        // there must be an easier way - :-(
        ((ActiveMQConnection)_cluster.getConnection()).getTransportChannel().getEmbeddedBrokerConnector().getBrokerContainer().stop();
        _connectionFactory.stop();
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
        String id=session.getName();
        _map.remove(id);
        session.destroy();
        _sessionPool.put(session);
        if (_log.isDebugEnabled()) _log.debug("destroyed: "+id);
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
    
    public Object putDistributedState(Object key, Object newValue) throws JMSException {
        synchronized (_distributedState) {
            Object oldValue=_distributedState.put(key, newValue);
            _cluster.getLocalNode().setState(_distributedState);
            return oldValue;
        }
    }
    
    public Object removeDistributedState(Object key) throws JMSException {
        synchronized (_distributedState) {
            Object value=_distributedState.remove(key);
            _cluster.getLocalNode().setState(_distributedState);
            return value;
        }
    }
}
