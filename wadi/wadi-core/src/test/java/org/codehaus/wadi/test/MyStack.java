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
package org.codehaus.wadi.test;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.impl.AbstractExclusiveContextualiser;
import org.codehaus.wadi.impl.AlwaysEvicter;
import org.codehaus.wadi.impl.ClusterContextualiser;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyManagerConfig;
import org.codehaus.wadi.impl.ExclusiveStoreContextualiser;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.MemoryReplicaterFactory;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SerialContextualiser;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.web.impl.DistributableAttributesFactory;
import org.codehaus.wadi.web.impl.DistributableValueFactory;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.web.impl.StandardHttpProxy;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.web.impl.WebEndPoint;
import org.codehaus.wadi.web.impl.WebHybridRelocater;
import org.codehaus.wadi.web.impl.WebSessionToSessionPoolAdapter;

public class MyStack {

    protected ClusteredManager _manager;
    protected AbstractExclusiveContextualiser _memory;
    private BasicServiceSpace serviceSpace;

    public MyStack(Dispatcher dispatcher) throws Exception {
        dispatcher.start();
        // Implementation note: we really need to wait some time to have a "stable" Dispatcher. For instance, in the
        // case of ActiveCluster, 
        Thread.sleep(1000);

        serviceSpace = new BasicServiceSpace(new ServiceSpaceName(new URI("Space")), dispatcher, 2000);
        dispatcher = serviceSpace.getDispatcher();

        int sweepInterval = 1000 * 60 * 60 * 24;
        boolean strictOrdering = true;
        Streamer streamer = new SimpleStreamer();
        Collapser collapser = new HashingCollapser(100, 1000);

        // Terminator
        Contextualiser terminator = new DummyContextualiser();
        Streamer sessionStreamer = new SimpleStreamer();

        // Cluster
        Contextualiser cluster = new ClusterContextualiser(terminator, collapser, new WebHybridRelocater(5000, 5000, true));

        // Disc
        Evicter devicter = new NeverEvicter(sweepInterval, strictOrdering);
        Map dmap = new HashMap();
        File dir = Utils.createTempDirectory("wadi", ".test", new File("/tmp"));
        Contextualiser spool = new ExclusiveStoreContextualiser(cluster, collapser, false, devicter, dmap,
                sessionStreamer, dir);

        Map mmap = new HashMap();
        Contextualiser serial = new SerialContextualiser(spool, collapser, mmap);
        WebSessionPool sessionPool = new SimpleSessionPool(new AtomicallyReplicableSessionFactory());

        // Memory
        Evicter mevicter = new AlwaysEvicter(sweepInterval, strictOrdering);
        SessionPool contextPool = new WebSessionToSessionPoolAdapter(sessionPool);
        PoolableInvocationWrapperPool requestPool = new DummyStatefulHttpServletRequestWrapperPool();
        _memory = new MemoryContextualiser(serial, mevicter, mmap, streamer, contextPool, requestPool);

        // Manager
        int numPartitions = 72;
        AttributesFactory attributesFactory = new DistributableAttributesFactory();
        ValuePool valuePool = new SimpleValuePool(new DistributableValueFactory());
        WebSessionWrapperFactory wrapperFactory = new StandardSessionWrapperFactory();
        SessionIdFactory idFactory = new TomcatSessionIdFactory();
        ReplicaterFactory replicaterfactory = new MemoryReplicaterFactory(numPartitions);
        EndPoint location = new WebEndPoint(new InetSocketAddress("localhost", 8080));
        InvocationProxy proxy = new StandardHttpProxy("jsessionid");
        _manager = new ClusteredManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, _memory,
                _memory.getMap(), new DummyRouter(), true, streamer, true, replicaterfactory, location, proxy,
                serviceSpace, 4, collapser);
        _manager.init(new DummyManagerConfig());

        serviceSpace.getServiceRegistry().register(new ServiceName("manager"), _manager);
    }

    public void start() throws Exception {
        serviceSpace.start();
    }

    public void stop() throws Exception {
        serviceSpace.stop();
    }

    public AbstractExclusiveContextualiser getTop() {
        return _memory;
    }

    public ClusteredManager getManager() {
        return _manager;
    }

    public BasicServiceSpace getServiceSpace() {
        return serviceSpace;
    }

}
