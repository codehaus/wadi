/**
 * Copyright 2006 The Apache Software Foundation
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

import java.util.Timer;

import org.codehaus.wadi.BasicSessionMonitor;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionFactory;
import org.codehaus.wadi.SessionMonitor;
import org.codehaus.wadi.ValueHelperRegistry;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.OswegoConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.ClusterContextualiser;
import org.codehaus.wadi.core.contextualiser.DummyContextualiser;
import org.codehaus.wadi.core.contextualiser.HashingCollapser;
import org.codehaus.wadi.core.contextualiser.MemoryContextualiser;
import org.codehaus.wadi.core.contextualiser.SerialContextualiser;
import org.codehaus.wadi.core.eviction.AbsoluteEvicter;
import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.session.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.core.session.BasicValueHelperRegistry;
import org.codehaus.wadi.core.session.DistributableAttributesFactory;
import org.codehaus.wadi.core.session.DistributableValueFactory;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.location.impl.BasicPartitionBalancerSingletonService;
import org.codehaus.wadi.location.impl.BasicPartitionBalancerSingletonServiceHolder;
import org.codehaus.wadi.location.impl.MovePMToSMEndPoint;
import org.codehaus.wadi.location.impl.PartitionBalancerSingletonService;
import org.codehaus.wadi.location.impl.PartitionBalancerSingletonServiceHolder;
import org.codehaus.wadi.location.impl.PartitionRepopulationEndPoint;
import org.codehaus.wadi.location.impl.SimplePartitionManager;
import org.codehaus.wadi.location.impl.SimplePartitionManagerTiming;
import org.codehaus.wadi.location.impl.SimpleStateManager;
import org.codehaus.wadi.partition.BasicPartitionBalancer;
import org.codehaus.wadi.replication.contextualiser.ReplicaAwareContextualiser;
import org.codehaus.wadi.replication.manager.ReplicaterAdapterFactory;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.BasicReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.MotableReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageFactory;
import org.codehaus.wadi.replication.storage.basic.BasicReplicaStorageFactory;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.web.impl.JkRouter;
import org.codehaus.wadi.web.impl.StandardHttpProxy;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * 
 * @version $Revision: 1538 $
 */
public class StackContext {
    private final ServiceSpaceName serviceSpaceName;
    private final Dispatcher underlyingDispatcher;
    private final int sessionTimeout;
    private final int sweepInterval;
    private final int numPartitions;
    private final ReplicationManagerFactory repManagerFactory;
    private final ReplicaStorageFactory repStorageFactory;
    private final BackingStrategyFactory backingStrategyFactory;
    
    protected ServiceSpace serviceSpace;
    protected PartitionMapper partitionMapper;
    protected PartitionManager partitionManager;
    protected StateManager stateManager;
    protected Timer timer;
    protected ReplicationManager replicationManager;
    protected SessionFactory sessionFactory;
    protected ClusteredManager manager;
    protected ConcurrentMotableMap memoryMap;
    protected Router router;
    private SimplePartitionManagerTiming simplePartitionManagerTiming;
    private BasicSessionMonitor sessionMonitor;

    public static PartitionBalancerSingletonServiceHolder newPartitionBalancerSingletonServiceHolder(ServiceSpace serviceSpace,
            int nbPartitions) throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        SingletonServiceHolder holder = serviceRegistry.registerSingleton(PartitionBalancerSingletonService.NAME,
                new BasicPartitionBalancerSingletonService(serviceSpace,
                        new BasicPartitionBalancer(serviceSpace.getDispatcher(), nbPartitions)));
        BasicPartitionBalancerSingletonServiceHolder balancerHolder = new BasicPartitionBalancerSingletonServiceHolder(holder);
        return balancerHolder;
    }
    
    public StackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher) {
        this(serviceSpaceName, underlyingDispatcher, 30 * 60);
    }

    public StackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher, int sessionTimeout) {
        this(serviceSpaceName,
                underlyingDispatcher,
                sessionTimeout,
                24,
                1000 * 60 * 60 * 24,
                new BasicReplicationManagerFactory(),
                new BasicReplicaStorageFactory(),
                new RoundRobinBackingStrategyFactory(1));
    }
    
    public StackContext(ServiceSpaceName serviceSpaceName,
            Dispatcher underlyingDispatcher,
            int sessionTimeout,
            int numPartitions,
            int sweepInterval,
            ReplicationManagerFactory repManagerFactory,
            ReplicaStorageFactory repStorageFactory,
            BackingStrategyFactory backingStrategyFactory) {
        if (null == serviceSpaceName) {
            throw new IllegalArgumentException("serviceSpaceName is required");
        } else if (null == underlyingDispatcher) {
            throw new IllegalArgumentException("underlyingDispatcher is required");            
        } else if (1 > sessionTimeout) {
            throw new IllegalArgumentException("sessionTimeout must be > 0");            
        } else if (1 > numPartitions) {
            throw new IllegalArgumentException("numPartitions must be > 0");            
        } else if (1 > sweepInterval) {
            throw new IllegalArgumentException("sweepInterval must be > 0");            
        } else if (null == repManagerFactory) {
            throw new IllegalArgumentException("repManagerFactory is required");
        } else if (null == repStorageFactory) {
            throw new IllegalArgumentException("repStorageFactory is required");
        } else if (null == backingStrategyFactory) {
            throw new IllegalArgumentException("backingStrategyFactory is required");
        }
        this.serviceSpaceName = serviceSpaceName;
        this.underlyingDispatcher = underlyingDispatcher;
        this.sessionTimeout = sessionTimeout;
        this.numPartitions = numPartitions;
        this.sweepInterval = sweepInterval;
        this.repManagerFactory = repManagerFactory;
        this.repStorageFactory = repStorageFactory;
        this.backingStrategyFactory = backingStrategyFactory;
    }

    public void build() throws ServiceAlreadyRegisteredException {
        simplePartitionManagerTiming = new SimplePartitionManagerTiming();
        timer = new Timer();
        serviceSpace = new BasicServiceSpace(serviceSpaceName, underlyingDispatcher);
        sessionMonitor = newSessionMonitor();
        partitionMapper = newPartitionMapper();
        partitionManager = newPartitionManager();
        stateManager = newStateManager();
        replicationManager = newReplicationManager();
        router = newRouter();
        sessionFactory = newSessionFactory();

        Contextualiser contextualiser = newReplicaAwareContextualiser(new DummyContextualiser());
        contextualiser = newClusteredContextualiser(contextualiser);

        memoryMap = new OswegoConcurrentMotableMap();
        contextualiser = newCollapserContextualiser(contextualiser, memoryMap);
        contextualiser = newMemoryContextualiser(contextualiser, memoryMap);
        
        manager = new ClusteredManager(stateManager,
                        partitionManager,
                        sessionFactory, 
                        new TomcatSessionIdFactory(),
                        contextualiser,
                        memoryMap,
                        router,
                        sessionMonitor,
                        false,
                        new StandardHttpProxy("jsessionid"));
        manager.init(new DummyManagerConfig());

        registerRepopulationEndPoint(contextualiser);
        registerReleaseEntryRequestEndPoint(contextualiser);
        registerMovePMToSMEndPoint(contextualiser);
        registerReplicaStorage();
        registerReplicationManager();
        registerClusteredManager(manager);
    }

    protected BasicSessionMonitor newSessionMonitor() {
        return new BasicSessionMonitor();
    }

    protected void registerClusteredManager(ClusteredManager manager) throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(ClusteredManager.NAME, manager);
    }

    protected void registerReplicationManager() throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(ReplicationManager.NAME, replicationManager);
    }

    protected void registerReplicaStorage() throws ServiceAlreadyRegisteredException {
        ReplicaStorage replicaStorage = repStorageFactory.factory(serviceSpace);
        
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(ReplicaStorage.NAME, replicaStorage);
    }

    protected void registerReleaseEntryRequestEndPoint(Contextualiser contextualiser)
            throws ServiceAlreadyRegisteredException {
        ReleaseEntryRequestEndPoint immigrationEndPoint = new ReleaseEntryRequestEndPoint(serviceSpace,
                contextualiser,
                stateManager);
        
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(ReleaseEntryRequestEndPoint.NAME, immigrationEndPoint);
    }

    protected void registerMovePMToSMEndPoint(Contextualiser contextualiser) throws ServiceAlreadyRegisteredException {
        MovePMToSMEndPoint movePMToSMEndPoint = new MovePMToSMEndPoint(serviceSpace, contextualiser, 2000);

        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(MovePMToSMEndPoint.NAME, movePMToSMEndPoint);
    }

    protected void registerRepopulationEndPoint(Contextualiser contextualiser) throws ServiceAlreadyRegisteredException {
        PartitionRepopulationEndPoint repopulationEndPoint = new PartitionRepopulationEndPoint(serviceSpace,
                        partitionMapper,
                        contextualiser);
        
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(PartitionRepopulationEndPoint.NAME, repopulationEndPoint);
    }

    protected Router newRouter() {
        String nodeName = underlyingDispatcher.getCluster().getLocalPeer().getName();
        return new JkRouter(nodeName);
    }

    protected SessionFactory newSessionFactory() {
        ValueHelperRegistry valueHelperRegistry = newValueHelperRegistry();
        ValueFactory valueFactory = newValueFactory(valueHelperRegistry);
        SimpleStreamer streamer = newStreamer();
        return new AtomicallyReplicableSessionFactory(
                new DistributableAttributesFactory(valueFactory),
                streamer,
                new ReplicaterAdapterFactory(replicationManager));
    }

    protected SimpleStreamer newStreamer() {
        return new SimpleStreamer();
    }

    protected DistributableValueFactory newValueFactory(ValueHelperRegistry valueHelperRegistry) {
        return new DistributableValueFactory(valueHelperRegistry);
    }

    protected BasicValueHelperRegistry newValueHelperRegistry() {
        return new BasicValueHelperRegistry();
    }
    
    protected StateManager newStateManager() {
        // TODO - revert to 2000 ms
        return new SimpleStateManager(serviceSpace, partitionManager, 1000 * 60 * 60);
    }

    protected PartitionMapper newPartitionMapper() {
        return new SimplePartitionMapper(numPartitions);
    }
    
    protected PartitionManager newPartitionManager() throws ServiceAlreadyRegisteredException {
        PartitionBalancerSingletonServiceHolder balancerHolder = newPartitionBalancerSingletonServiceHolder(serviceSpace,
                numPartitions);

        return new SimplePartitionManager(serviceSpace, 
                numPartitions,
                partitionMapper, 
                balancerHolder, 
                simplePartitionManagerTiming);
    }

    protected ReplicationManager newReplicationManager() {
        return repManagerFactory.factory(serviceSpace, backingStrategyFactory);
    }

    protected Contextualiser newCollapserContextualiser(Contextualiser contextualiser, ConcurrentMotableMap mmap) {
        return new SerialContextualiser(contextualiser, new HashingCollapser(1024, 2000), mmap);
    }
    
    protected Contextualiser newMemoryContextualiser(Contextualiser next, ConcurrentMotableMap mmap) {
        Evicter mevicter = new AbsoluteEvicter(sweepInterval, true, sessionTimeout);
        PoolableInvocationWrapperPool requestPool = new DummyStatefulHttpServletRequestWrapperPool();
        return newMemoryContextualiser(next, mmap, mevicter, requestPool);
    }

    protected MemoryContextualiser newMemoryContextualiser(Contextualiser next,
            ConcurrentMotableMap mmap,
            Evicter mevicter,
            PoolableInvocationWrapperPool requestPool) {
        return new MemoryContextualiser(next, mevicter, mmap, sessionFactory, requestPool);
    }

    protected Contextualiser newClusteredContextualiser(Contextualiser contextualiser) {
        return new ClusterContextualiser(contextualiser, 
                new HybridRelocater(serviceSpace,
                        partitionManager,
                        simplePartitionManagerTiming.getSessionRelocationWaitTimeForRelocater()),
                partitionManager, 
                stateManager, 
                new SynchronizedBoolean(false));
    }

    protected Contextualiser newReplicaAwareContextualiser(Contextualiser next) {
        ReplicationManager sessionRepManager = new MotableReplicationManager(replicationManager);
        return new ReplicaAwareContextualiser(next, sessionRepManager, stateManager);
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    public PartitionMapper getPartitionMapper() {
        return partitionMapper;
    }

    public ReplicationManager getReplicationManager() {
        return replicationManager;
    }

    public ServiceSpace getServiceSpace() {
        return serviceSpace;
    }

    public ServiceSpaceName getServiceSpaceName() {
        return serviceSpaceName;
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public ClusteredManager getManager() {
        return manager;
    }

    public ConcurrentMotableMap getMemoryMap() {
        return memoryMap;
    }

    public SessionMonitor getSessionMonitor() {
        return sessionMonitor;
    }

}
