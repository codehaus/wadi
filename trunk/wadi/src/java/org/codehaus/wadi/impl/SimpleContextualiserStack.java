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

// Thoughts -

// invalidation is tricky - stuff invalidated on disc needs may need to be unmarshalled so that the correct listeners may be notified...

// we need a JDBC passivation store

// can we even store an invalidated session without breaking listener model - consider...



import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.activecluster.ClusterException;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.HttpServletRequestWrapperPool;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.Streamer;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class SimpleContextualiserStack implements Contextualiser {

    protected final Streamer _streamer;
    protected final Collapser _collapser;

    protected final DummyContextualiser _dummy;

    protected final DataSource _databaseDataSource;
    protected final String _databaseTable;
    protected Evicter _databaseEvicter;
    protected final SharedJDBCContextualiser _database;

    protected final ConnectionFactory _connectionFactory;
    protected final CustomClusterFactory _clusterFactory;
    protected final String _clusterName;
    protected final CustomCluster _clusterCluster;
    protected final Evicter _clusterEvicter;
    protected final Map _clusterMap;
    protected final MessageDispatcher _clusterDispatcher;
    protected final Relocater _clusterRelocater;
    protected final Location _clusterLocation;
    protected final ClusterContextualiser _cluster;

    protected final Pattern _statelessMethods;
    protected final boolean _statelessMethodFlag;
    protected final Pattern _statelessURIs;
    protected final boolean _statelessURIFlag;
    protected final StatelessContextualiser _stateless;

    protected final File _discDirectory;
    protected final Evicter _discEvicter;
    protected final Map _discMap;
    protected final ExclusiveDiscContextualiser _disc;

    protected final SerialContextualiser _serial;

    protected final ContextPool _memoryPool;
    protected final HttpServletRequestWrapperPool _requestPool;
    protected final Evicter _memoryEvicter;
    protected final Map _memoryMap;
    protected final MemoryContextualiser _memory;

    public SimpleContextualiserStack(Map sessionMap, ContextPool pool, DataSource dataSource, int port) throws SQLException, JMSException, ClusterException, UnknownHostException {
        super();
        _streamer=new SimpleStreamer();
        //_collapser=new DebugCollapser();
        _collapser=new HashingCollapser(1000, 6000);

        _dummy=new DummyContextualiser();
        _databaseDataSource=dataSource;
        _databaseTable="WADI";
        SharedJDBCMotable.init(_databaseDataSource, _databaseTable);
        _database=new SharedJDBCContextualiser(_dummy, _collapser, true, _databaseDataSource, _databaseTable);
        _connectionFactory=Utils.getConnectionFactory();
        _clusterFactory=new CustomClusterFactory(_connectionFactory);
        _clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
        _clusterCluster=(CustomCluster)_clusterFactory.createCluster(_clusterName);
        InetAddress localhost=InetAddress.getLocalHost();
        System.out.println("LOCALHOST: "+localhost);
        InetSocketAddress isa=new InetSocketAddress(localhost, port);
        HttpProxy proxy=new StandardHttpProxy("jsessionid");
        _clusterLocation=new HttpProxyLocation(_clusterCluster.getLocalNode().getDestination(), isa, proxy);
        _clusterMap=new ConcurrentHashMap();
        _clusterEvicter=new DummyEvicter(); // TODO - consider Cluster eviction carefully...
        _clusterDispatcher=new MessageDispatcher(_clusterCluster);
//        _clusterRelocater=new ImmigrateRelocater(_clusterDispatcher, _clusterLocation, 2000, _clusterMap, _collapser);
        //(MessageDispatcher dispatcher, Location location, long timeout, long proxyHandOverPeriod) {
        _clusterRelocater=new ProxyRelocater(2000, 2000);
        _cluster=new ClusterContextualiser(_database, _collapser, _clusterEvicter, _clusterMap, _clusterCluster, _clusterDispatcher, _clusterRelocater, _clusterLocation);

        _statelessMethods=Pattern.compile("GET|POST", Pattern.CASE_INSENSITIVE);
        _statelessMethodFlag=true;
        _statelessURIs=Pattern.compile(".*\\.(JPG|JPEG|GIF|PNG|ICO|HTML|HTM)(|;jsessionid=.*)", Pattern.CASE_INSENSITIVE);
        _statelessURIFlag=false;
        _stateless=new StatelessContextualiser(_cluster, _statelessMethods, _statelessMethodFlag, _statelessURIs, _statelessURIFlag);

        File dir=new File("/tmp/wadi/"+System.getProperty("wadi.colour"));
        dir.delete();
        dir.mkdir();
        _discDirectory=dir;
        // TODO - consider eviction on disc, indexing by ttl would be efficient enough...
        _discEvicter=new NeverEvicter(20, true); // sessions never pass below this point, unless the node is shutdown
        _discMap=new ConcurrentHashMap();
        _disc=new ExclusiveDiscContextualiser(_stateless, _collapser, true, _discEvicter, _discMap, _streamer, _discDirectory);


        _memoryPool=pool;
        _memoryEvicter=new AbsoluteEvicter(10, true, 10); // if a session is inactive for 10 secs, it moves to disc
        _memoryMap=sessionMap;
        _serial=new SerialContextualiser(_disc, _collapser, _memoryMap);
        _requestPool=new DummyStatefulHttpServletRequestWrapperPool(); // TODO - use a ThreadLocal based Pool
        _memory=new MemoryContextualiser(_serial, _memoryEvicter, _memoryMap, _streamer, _memoryPool, _requestPool);

        _cluster.setTop(_memory);
        // ready to rock !
    }

    public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws IOException, ServletException {
        return _memory.contextualise(hreq, hres, chain, id, immoter, motionLock, exclusiveOnly);
    }

    public Evicter getEvicter() {
        return _memory.getEvicter();
    }

    public boolean isExclusive() {
        return _memory.isExclusive();
    }

    public Immoter getDemoter(String id, Motable motable) {
        return _memory.getDemoter(id, motable);
    }

    public Immoter getSharedDemoter() {
        return _memory.getSharedDemoter();
    }

    public void init(ContextualiserConfig config) {
        _memory.init(config);
    }

    public void start() throws Exception {
        _clusterCluster.start();
	// TODO - dispatcher probably needs a Lifecycle too... (and a thread pool)
        _memory.start();
    }

    public void stop() throws Exception {
        _memory.stop();
        _clusterCluster.stop();
    }

    public void destroy() {
        _memory.destroy();
    }

    public void promoteToExclusive(Immoter immoter){_memory.promoteToExclusive(immoter);}
    public int loadMotables(Emoter emoter, Immoter immoter) {return _memory.loadMotables(emoter, immoter);}

    public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime){_memory.setLastAccessedTime(evictable, oldTime, newTime);}
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {_memory.setMaxInactiveInterval(evictable, oldInterval, newInterval);}

    public Contextualiser getTop() {return _memory;}

}
