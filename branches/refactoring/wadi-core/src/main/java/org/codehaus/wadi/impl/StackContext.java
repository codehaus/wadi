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

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.OswegoConcurrentSessionMap;
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
import org.codehaus.wadi.replication.contextualizer.ReplicaAwareContextualiser;
import org.codehaus.wadi.replication.manager.ReplicaterAdapterFactory;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.basic.BasicReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.SessionReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.basic.MemoryReplicaStorage;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;
import org.codehaus.wadi.web.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.web.impl.DistributableAttributesFactory;
import org.codehaus.wadi.web.impl.DistributableValueFactory;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.web.impl.StandardHttpProxy;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.web.impl.WebSessionToSessionPoolAdapter;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * 
 * @version $Revision: 1538 $
 */
public class StackContext {
    private final ServiceSpaceName serviceSpaceName;
    private final Dispatcher underlyingDispatcher;
    private final int sessionTimeout;
    
    protected ServiceSpace serviceSpace;
    protected PartitionMapper partitionMapper;
    protected PartitionManager partitionManager;
    protected StateManager stateManager;
    protected Timer timer;
    private SessionPool contextPool;
    private ReplicationManager replicationManager;
    private SimpleSessionPool sessionPool;
    private ClusteredManager manager;
    private ConcurrentMotableMap memoryMap;

    public static PartitionBalancerSingletonServiceHolder newPartitionBalancerSingletonServiceHolder(ServiceSpace serviceSpace, int nbPartitions) throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        SingletonServiceHolder holder = serviceRegistry.registerSingleton(PartitionBalancerSingletonService.NAME,
                new BasicPartitionBalancerSingletonService(
                        new BasicPartitionBalancer(serviceSpace.getDispatcher(), nbPartitions)));
        BasicPartitionBalancerSingletonServiceHolder balancerHolder = new BasicPartitionBalancerSingletonServiceHolder(holder);
        return balancerHolder;
    }
    
    public StackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher) {
        this(serviceSpaceName, underlyingDispatcher, 30);
    }

    public StackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher, int sessionTimeout) {
        if (null == serviceSpaceName) {
            throw new IllegalArgumentException("serviceSpaceName is required");
        } else if (null == underlyingDispatcher) {
            throw new IllegalArgumentException("underlyingDispatcher is required");            
        } else if (1 > sessionTimeout) {
            throw new IllegalArgumentException("sessionTimeout must be > 0");            
        }
        this.serviceSpaceName = serviceSpaceName;
        this.underlyingDispatcher = underlyingDispatcher;
        this.sessionTimeout = sessionTimeout;
    }

    public void build() throws ServiceAlreadyRegisteredException {
        timer = new Timer();
        
        serviceSpace = new BasicServiceSpace(serviceSpaceName, underlyingDispatcher);

        partitionMapper = newPartitionMapper();
        partitionManager = newPartitionManager();
        stateManager = newStateManager();
        
        sessionPool = newWebSessionPool();
        contextPool = new WebSessionToSessionPoolAdapter(sessionPool);

        replicationManager = newReplicationManager();
        Contextualiser contextualiser = newReplicaAwareContextualiser(new DummyContextualiser());
        
        contextualiser = newClusteredContextualiser(contextualiser);
        
        memoryMap = new OswegoConcurrentSessionMap();
        contextualiser = newMemoryContextualiser(contextualiser, memoryMap);

        contextualiser = newCollapserContextualiser(contextualiser);

        manager = new ClusteredManager(stateManager,
                        partitionManager,
                        sessionPool, 
                        new DistributableAttributesFactory(),
                        new SimpleValuePool(new DistributableValueFactory()),
                        new StandardSessionWrapperFactory(),
                        new TomcatSessionIdFactory(),
                        contextualiser,
                        memoryMap,
                        new DummyRouter(),
                        true,
                        new SimpleStreamer(),
                        true,
                        new ReplicaterAdapterFactory(replicationManager, sessionPool),
                        new StandardHttpProxy("jsessionid"));
        manager.init(new DummyManagerConfig());

        registerRepopulationEndPoint(contextualiser);
        registerReleaseEntryRequestEndPoint(contextualiser);
        registerMovePMToSMEndPoint(contextualiser);
        registerReplicaStorage();
        registerReplicationManager();
        registerClusteredManager(manager);
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
        ReplicaStorage replicaStorage = new MemoryReplicaStorage();
        
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

    protected SimpleSessionPool newWebSessionPool() {
        return new SimpleSessionPool(new AtomicallyReplicableSessionFactory());
    }
    
    protected StateManager newStateManager() {
        return new SimpleStateManager(serviceSpace, partitionManager, 2000);
    }

    protected int getNbPartitions() {
        return 24;
    }

    protected PartitionMapper newPartitionMapper() {
        return new SimplePartitionMapper(getNbPartitions());
    }
    
    protected PartitionManager newPartitionManager() throws ServiceAlreadyRegisteredException {
        PartitionBalancerSingletonServiceHolder balancerHolder = newPartitionBalancerSingletonServiceHolder(serviceSpace,
                getNbPartitions());

        return new SimplePartitionManager(serviceSpace, 
                getNbPartitions(),
                partitionMapper, 
                balancerHolder, 
                new SimplePartitionManagerTiming());
    }

    protected ReplicationManager newReplicationManager() {
        BasicReplicationManagerFactory managerFactory = new BasicReplicationManagerFactory();
        ReplicationManager replicationManager = managerFactory.factory(serviceSpace,
                new RoundRobinBackingStrategyFactory(1));
        return replicationManager;
    }
    
    protected Contextualiser newCollapserContextualiser(Contextualiser contextualiser) {
        return new SerialContextualiserFrontingMemory(contextualiser, new HashingCollapser(1024, 2000));
    }
    
    protected Contextualiser newMemoryContextualiser(Contextualiser next, ConcurrentMotableMap mmap) {
        int sweepInterval = 1000 * 60 * 60 * 24;
        Evicter mevicter = new AbsoluteEvicter(sweepInterval, true, sessionTimeout);
        PoolableInvocationWrapperPool requestPool = new DummyStatefulHttpServletRequestWrapperPool();
        return new MemoryContextualiser(next, mevicter, mmap, contextPool, requestPool);
    }

    protected Contextualiser newClusteredContextualiser(Contextualiser contextualiser) {
        return new ClusterContextualiser(contextualiser, 
                new HybridRelocater(serviceSpace, partitionManager, 1000), 
                partitionManager, 
                stateManager, 
                new SynchronizedBoolean(false));
    }

    protected Contextualiser newReplicaAwareContextualiser(Contextualiser next) {
        ReplicationManager sessionRepManager = new SessionReplicationManager(replicationManager, sessionPool);
        return new ReplicaAwareContextualiser(next, sessionRepManager);
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

    public void setManager(ClusteredManager manager) {
        this.manager = manager;
    }

    public ConcurrentMotableMap getMemoryMap() {
        return memoryMap;
    }

}
