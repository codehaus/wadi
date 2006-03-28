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
package org.codehaus.wadi;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.ProxiedLocation;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.impl.DummyPartitionManager;
import org.codehaus.wadi.http.HTTPProxiedLocation;
import org.codehaus.wadi.impl.AbstractExclusiveContextualiser;
import org.codehaus.wadi.impl.AlwaysEvicter;
import org.codehaus.wadi.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.impl.ClusterContextualiser;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DatabaseStore;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyManagerConfig;
import org.codehaus.wadi.impl.DummyRelocater;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.ExclusiveStoreContextualiser;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.MemoryReplicaterFactory;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SerialContextualiser;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SharedStoreContextualiser;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardHttpProxy;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;

public class MyStack {

    protected ClusteredManager _manager;
    protected AbstractExclusiveContextualiser _memory;

    public MyStack(String url, DataSource dataSource, Dispatcher dispatcher) throws Exception {
      int sweepInterval=1000*60*60*24; // 1 eviction/day
      boolean strictOrdering=true;
      Streamer streamer=new SimpleStreamer();
      Collapser collapser=new HashingCollapser(100, 1000);

      // Terminator
      Contextualiser terminator=new DummyContextualiser();
      Streamer sessionStreamer=new SimpleStreamer();

      // DB
      String storeTable="SESSIONS";
      DatabaseStore store=new DatabaseStore(url, dataSource, storeTable, false, true, true);
      boolean clean=true;
      Contextualiser db=new SharedStoreContextualiser(terminator, collapser, clean, store);

      // Cluster
      Contextualiser cluster=new ClusterContextualiser(db, collapser, new DummyRelocater());

      // Disc
      Evicter devicter=new NeverEvicter(sweepInterval, strictOrdering);
      Map dmap=new HashMap();
      File dir=Utils.createTempDirectory("wadi", ".test", new File("/tmp"));
      Contextualiser spool=new ExclusiveStoreContextualiser(cluster, collapser, false, devicter, dmap, sessionStreamer, dir);

      Map mmap=new HashMap();

      Contextualiser serial=new SerialContextualiser(spool, collapser, mmap);

      SessionPool sessionPool=new SimpleSessionPool(new AtomicallyReplicableSessionFactory());

      // Memory
      Evicter mevicter=new AlwaysEvicter(sweepInterval, strictOrdering);
      ContextPool contextPool=new SessionToContextPoolAdapter(sessionPool);
      PoolableInvocationWrapperPool requestPool=new DummyStatefulHttpServletRequestWrapperPool();
      _memory=new MemoryContextualiser(serial, mevicter, mmap, streamer, contextPool, requestPool);

      // Manager
      int numPartitions=72;
      AttributesFactory attributesFactory=new DistributableAttributesFactory();
      ValuePool valuePool=new SimpleValuePool(new DistributableValueFactory());
      SessionWrapperFactory wrapperFactory=new StandardSessionWrapperFactory();
      SessionIdFactory idFactory=new TomcatSessionIdFactory();
      ReplicaterFactory replicaterfactory=new MemoryReplicaterFactory(numPartitions);
      ProxiedLocation location = new HTTPProxiedLocation(new InetSocketAddress("localhost", 8080));
      InvocationProxy proxy=new StandardHttpProxy("jsessionid");
      //String clusterUri="peer://wadi";
      //String clusterUri="tcp://localhost:61616";
      //String clusterUri="vm://localhost";
      PartitionManager partitionManager=new DummyPartitionManager(numPartitions);
      //Dispatcher dispatcher=new ActiveClusterDispatcher(nodeName, clusterName, clusterUri, 5000L);
      _manager=new ClusteredManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, _memory, _memory.getMap(), new DummyRouter(), true, streamer, true, replicaterfactory, location, proxy, dispatcher, partitionManager, collapser);
//    manager.setSessionListeners(new HttpSessionListener[]{});
      //manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
      _manager.init(new DummyManagerConfig());
    }

    public void start() throws Exception {
      _manager.start();
    }

    public void stop() throws Exception {
      _manager.stop();
    }

    public AbstractExclusiveContextualiser getTop() {
      return _memory;
    }

    public ClusteredManager getManager() {
      return _manager;
    }

  }
