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
package org.codehaus.wadi.core.assembler;

import java.io.File;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.JDK5ConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.ClusterContextualiser;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.DummyContextualiser;
import org.codehaus.wadi.core.contextualiser.ExclusiveStoreContextualiser;
import org.codehaus.wadi.core.contextualiser.HashingCollapser;
import org.codehaus.wadi.core.contextualiser.HybridRelocater;
import org.codehaus.wadi.core.contextualiser.MemoryContextualiser;
import org.codehaus.wadi.core.contextualiser.SerialContextualiser;
import org.codehaus.wadi.core.contextualiser.SharedStoreContextualiser;
import org.codehaus.wadi.core.eviction.AbsoluteEvicter;
import org.codehaus.wadi.core.eviction.Evicter;
import org.codehaus.wadi.core.manager.BasicSessionMonitor;
import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.manager.DummyManagerConfig;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.manager.TomcatSessionIdFactory;
import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.session.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.core.session.BasicValueHelperRegistry;
import org.codehaus.wadi.core.session.DistributableAttributesFactory;
import org.codehaus.wadi.core.session.DistributableValueFactory;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.core.session.ValueHelperRegistry;
import org.codehaus.wadi.core.store.DiscStore;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.location.balancing.BasicPartitionBalancer;
import org.codehaus.wadi.location.balancing.BasicPartitionBalancerSingletonService;
import org.codehaus.wadi.location.balancing.BasicPartitionBalancerSingletonServiceHolder;
import org.codehaus.wadi.location.balancing.PartitionBalancerSingletonService;
import org.codehaus.wadi.location.balancing.PartitionBalancerSingletonServiceHolder;
import org.codehaus.wadi.location.endpoint.MovePMToSMEndPoint;
import org.codehaus.wadi.location.endpoint.PartitionRepopulationEndPoint;
import org.codehaus.wadi.location.endpoint.ReleaseEntryRequestEndPoint;
import org.codehaus.wadi.location.partitionmanager.PartitionManager;
import org.codehaus.wadi.location.partitionmanager.PartitionMapper;
import org.codehaus.wadi.location.partitionmanager.SimplePartitionManager;
import org.codehaus.wadi.location.partitionmanager.SimplePartitionManagerTiming;
import org.codehaus.wadi.location.partitionmanager.SimplePartitionMapper;
import org.codehaus.wadi.location.statemanager.SimpleStateManager;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.contextualiser.ReplicaAwareContextualiser;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.LoggingReplicationManager;
import org.codehaus.wadi.replication.manager.basic.NoOpReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.ObjectStateHandler;
import org.codehaus.wadi.replication.manager.basic.SessionStateHandler;
import org.codehaus.wadi.replication.manager.basic.SyncReplicationManagerFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageFactory;
import org.codehaus.wadi.replication.storage.memory.SyncMemoryReplicaStorageFactory;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;
import org.codehaus.wadi.servicespace.admin.commands.ContextualiserStackExplorer;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;
import org.codehaus.wadi.web.impl.JkRouter;
import org.codehaus.wadi.web.impl.StandardHttpProxy;

/**
 * 
 * @version $Revision: 1538 $
 */
public class StackContext {
    private static final int THIRTY_MINUTES_IN_SECONDS = 30 * 60;
    private static final int ONE_MINUTE_IN_SECONDS = 60;
    private static final int TEN_SECONDS_IN_MILLISECONDS = 10 * 1000;
    
    private final ClassLoader cl;
    private final ServiceSpaceName serviceSpaceName;
    protected final Dispatcher underlyingDispatcher;
    private final int sessionTimeout;
    private final int sweepInterval;
    private final int numPartitions;
    private final BackingStrategyFactory backingStrategyFactory;
    private File discStoreDir;
    private int numberOfSecondsInMemoryContextualiser;
    private boolean disableReplication;
    private Store sharedStore;
    
    protected ServiceSpace serviceSpace;
    protected PartitionMapper partitionMapper;
    protected PartitionManager partitionManager;
    protected StateManager stateManager;
    protected Timer timer;
    protected ReplicationManager replicationManager;
    protected ReplicaStorage replicaStorage;
    protected SessionFactory sessionFactory;
    protected ClusteredManager manager;
    protected ConcurrentMotableMap memoryMap;
    protected Router router;
    private SimplePartitionManagerTiming simplePartitionManagerTiming;
    protected SessionMonitor sessionMonitor;
    private ReplicationManagerFactory repManagerFactory;
    private ReplicaStorageFactory repStorageFactory;
    protected ObjectStateHandler stateHandler;

    public StackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher) {
        this(serviceSpaceName, underlyingDispatcher, THIRTY_MINUTES_IN_SECONDS);
    }

    public StackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher, int sessionTimeout) {
        this(Thread.currentThread().getContextClassLoader(),
                serviceSpaceName,
                underlyingDispatcher,
                sessionTimeout,
                24,
                ONE_MINUTE_IN_SECONDS,
                new RoundRobinBackingStrategyFactory(1));
    }
    
    public StackContext(ClassLoader cl,
            ServiceSpaceName serviceSpaceName,
            Dispatcher underlyingDispatcher,
            int sessionTimeout,
            int numPartitions,
            int sweepInterval,
            BackingStrategyFactory backingStrategyFactory) {
        if (null == cl) {
            throw new IllegalArgumentException("cl is required");
        } else if (null == serviceSpaceName) {
            throw new IllegalArgumentException("serviceSpaceName is required");
        } else if (null == underlyingDispatcher) {
            throw new IllegalArgumentException("underlyingDispatcher is required");            
        } else if (1 > sessionTimeout) {
            throw new IllegalArgumentException("sessionTimeout must be > 0");            
        } else if (1 > numPartitions) {
            throw new IllegalArgumentException("numPartitions must be > 0");            
        } else if (1 > sweepInterval) {
            throw new IllegalArgumentException("sweepInterval must be > 0");            
        } else if (null == backingStrategyFactory) {
            throw new IllegalArgumentException("backingStrategyFactory is required");
        }
        this.cl = cl;
        this.serviceSpaceName = serviceSpaceName;
        this.underlyingDispatcher = underlyingDispatcher;
        this.sessionTimeout = sessionTimeout;
        this.numPartitions = numPartitions;
        this.sweepInterval = sweepInterval;
        this.backingStrategyFactory = backingStrategyFactory;
    }

    public void build() throws Exception {
        simplePartitionManagerTiming = new SimplePartitionManagerTiming();
        timer = new Timer();
        
        ClassIndexerRegistry serviceClassIndexerRegistry = newServiceClassIndexerRegistry();
        
        Streamer streamer = newStreamer();

        serviceSpace = new BasicServiceSpace(serviceSpaceName,
            underlyingDispatcher,
            serviceClassIndexerRegistry,
            streamer);

        sessionMonitor = newSessionMonitor();
        partitionMapper = newPartitionMapper();
        partitionManager = newPartitionManager();
        stateManager = newStateManager();
        router = newRouter();
        
        stateHandler = newObjectStateHandler(streamer);
        
        repStorageFactory = newReplicaStorageFactory();
        replicaStorage = repStorageFactory.factory(serviceSpace);
        
        repManagerFactory = newReplicationManagerFactory();
        replicationManager = newReplicationManager();

        sessionFactory = newSessionFactory(streamer);
        stateHandler.setObjectFactory(sessionFactory);
        
        ContextualiserStackExplorer stackExplorer = new ContextualiserStackExplorer();
        
        Contextualiser contextualiser = new DummyContextualiser(stateManager, replicationManager);
        
        contextualiser = newSharedStoreContextualiser(contextualiser);
        stackExplorer.pushContextualiser(contextualiser);

        contextualiser = newReplicaAwareContextualiser(contextualiser);
        stackExplorer.pushContextualiser(contextualiser);

        contextualiser = newClusteredContextualiser(contextualiser);
        stackExplorer.pushContextualiser(contextualiser);

        memoryMap = newConcurrentMap();
        contextualiser = newCollapserContextualiser(contextualiser, memoryMap);
        stackExplorer.pushContextualiser(contextualiser);

        contextualiser = newDiscStoreContextualiser(streamer, contextualiser);
        stackExplorer.pushContextualiser(contextualiser);
        
        contextualiser = newMemoryContextualiser(contextualiser, memoryMap);
        stackExplorer.pushContextualiser(contextualiser);

        manager = new ClusteredManager(stateManager,
                        partitionManager,
                        sessionFactory, 
                        new TomcatSessionIdFactory(),
                        contextualiser,
                        memoryMap,
                        router,
                        sessionMonitor,
                        new StandardHttpProxy("jsessionid"));
        manager.init(new DummyManagerConfig());

        registerRepopulationEndPoint(contextualiser);
        registerReleaseEntryRequestEndPoint(contextualiser);
        registerMovePMToSMEndPoint(contextualiser);
        registerReplicaStorage();
        registerReplicationManager();
        registerStackExplorer(stackExplorer);
        
        // Implementation note: must be registered in this exact order.
        registerPartitionManager();
        registerStateManager();
        registerClusteredManager(manager);
        // End of implementation note.
    }

    protected Contextualiser newDiscStoreContextualiser(Streamer streamer, Contextualiser contextualiser)
            throws Exception {
        if (sessionTimeout == numberOfSecondsInMemoryContextualiser) {
            return contextualiser;
        }
        
        DiscStore store = newDiscStore(streamer);
        return new ExclusiveStoreContextualiser(contextualiser,
            true,
            new AbsoluteEvicter(sweepInterval, true, sessionTimeout),
            new JDK5ConcurrentMotableMap(),
            store);
    }

    protected DiscStore newDiscStore(Streamer streamer) throws Exception {
        if (null == discStoreDir) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            discStoreDir = new File(tmpDir);
        }
        return new DiscStore(streamer, discStoreDir, true);
    }

    protected ClassIndexerRegistry newServiceClassIndexerRegistry() {
        return new JDKClassIndexerRegistry(new DeclaredMemberFilter());
    }

    protected JDK5ConcurrentMotableMap newConcurrentMap() {
        return new JDK5ConcurrentMotableMap();
    }

    protected ObjectStateHandler newObjectStateHandler(Streamer streamer) {
        return new SessionStateHandler(streamer);
    }

    protected ReplicaStorageFactory newReplicaStorageFactory() {
        return new SyncMemoryReplicaStorageFactory(stateHandler);
    }

    protected ReplicationManagerFactory newReplicationManagerFactory() {
        if (disableReplication) {
            return new NoOpReplicationManagerFactory();
        }
        return new SyncReplicationManagerFactory(stateHandler, replicaStorage);
    }

    protected void registerStackExplorer(ContextualiserStackExplorer stackExplorer) throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(ContextualiserStackExplorer.NAME, stackExplorer);
    }

    protected void registerStateManager() throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(StateManager.NAME, stateManager);
    }

    protected void registerPartitionManager() throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        serviceRegistry.register(PartitionManager.NAME, partitionManager);
    }

    protected SessionMonitor newSessionMonitor() {
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
        MovePMToSMEndPoint movePMToSMEndPoint = new MovePMToSMEndPoint(serviceSpace, 
            contextualiser, 
            replicationManager,
            simplePartitionManagerTiming.getSessionRelocationIMToSMAckWaitTime());

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

    protected SessionFactory newSessionFactory(Streamer streamer) {
        ValueHelperRegistry valueHelperRegistry = newValueHelperRegistry();
        ValueFactory valueFactory = newValueFactory(valueHelperRegistry);
        return new AtomicallyReplicableSessionFactory(
                new DistributableAttributesFactory(valueFactory),
                streamer,
                replicationManager);
    }

    protected Streamer newStreamer() {
        return new SimpleStreamer(cl);
    }

    protected DistributableValueFactory newValueFactory(ValueHelperRegistry valueHelperRegistry) {
        return new DistributableValueFactory(valueHelperRegistry);
    }

    protected BasicValueHelperRegistry newValueHelperRegistry() {
        return new BasicValueHelperRegistry();
    }
    
    protected StateManager newStateManager() {
        return new SimpleStateManager(serviceSpace, partitionManager, TEN_SECONDS_IN_MILLISECONDS);
    }

    protected PartitionMapper newPartitionMapper() {
        return new SimplePartitionMapper(numPartitions);
    }
    
    protected PartitionManager newPartitionManager() throws ServiceAlreadyRegisteredException {
        PartitionBalancerSingletonServiceHolder balancerHolder = newPartitionBalancerSingletonServiceHolder();

        return new SimplePartitionManager(serviceSpace, 
                numPartitions,
                partitionMapper, 
                balancerHolder, 
                simplePartitionManagerTiming);
    }

    protected PartitionBalancerSingletonServiceHolder newPartitionBalancerSingletonServiceHolder()
            throws ServiceAlreadyRegisteredException {
        ServiceRegistry serviceRegistry = serviceSpace.getServiceRegistry();
        SingletonServiceHolder holder = serviceRegistry.registerSingleton(PartitionBalancerSingletonService.NAME,
                new BasicPartitionBalancerSingletonService(serviceSpace, new BasicPartitionBalancer(serviceSpace
                        .getDispatcher(), numPartitions)));
        return new BasicPartitionBalancerSingletonServiceHolder(holder);
    }

    protected ReplicationManager newReplicationManager() {
        ReplicationManager replicationManager = repManagerFactory.factory(serviceSpace, backingStrategyFactory);
        return new LoggingReplicationManager(replicationManager);
    }

    protected Contextualiser newCollapserContextualiser(Contextualiser contextualiser, ConcurrentMotableMap mmap) {
        return new SerialContextualiser(contextualiser, new HashingCollapser(1024, TEN_SECONDS_IN_MILLISECONDS), mmap);
    }
    
    protected Contextualiser newMemoryContextualiser(Contextualiser next, ConcurrentMotableMap mmap) {
        if (0 == numberOfSecondsInMemoryContextualiser) {
            numberOfSecondsInMemoryContextualiser = sessionTimeout / 2;
        }
        Evicter mevicter = new AbsoluteEvicter(sweepInterval, true, numberOfSecondsInMemoryContextualiser);
        return newMemoryContextualiser(next, mmap, mevicter);
    }

    protected MemoryContextualiser newMemoryContextualiser(Contextualiser next,
            ConcurrentMotableMap mmap,
            Evicter mevicter) {
        return new MemoryContextualiser(next, mevicter, mmap, sessionFactory, sessionMonitor);
    }

    protected Contextualiser newClusteredContextualiser(Contextualiser contextualiser) {
        return new ClusterContextualiser(contextualiser, 
                new HybridRelocater(serviceSpace, partitionManager, replicationManager),
                partitionManager, 
                stateManager, 
                new AtomicBoolean(false));
    }

    protected Contextualiser newSharedStoreContextualiser(Contextualiser next) {
        if (null == sharedStore) {
            return next;
        } else {
            return new SharedStoreContextualiser(next, sharedStore, stateManager, replicationManager, sessionMonitor);
        }
    }

    protected Contextualiser newReplicaAwareContextualiser(Contextualiser next) {
        if (disableReplication) {
            return next;
        }
        return new ReplicaAwareContextualiser(next, replicationManager, stateManager);
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

    public Manager getManager() {
        return manager;
    }

    public ConcurrentMotableMap getMemoryMap() {
        return memoryMap;
    }

    public SessionMonitor getSessionMonitor() {
        return sessionMonitor;
    }

    public boolean isDisableReplication() {
        return disableReplication;
    }

    public void setDisableReplication(boolean disableReplication) {
        this.disableReplication = disableReplication;
    }

    public void setSharedStore(Store sharedStore) {
        this.sharedStore = sharedStore;
    }

    public void setDiscStoreDir(File discStoreDir) {
        this.discStoreDir = discStoreDir;
    }

    public void setNumberOfSecondsInMemoryContextualiser(int numberOfSecondsInMemoryContextualiser) {
        if (numberOfSecondsInMemoryContextualiser < 1) {
            throw new IllegalStateException("numberOfSecondsInMemoryContextualiser must be greater than 0");
        } else if (numberOfSecondsInMemoryContextualiser > sessionTimeout) {
            throw new IllegalStateException("numberOfSecondsInMemoryContextualiser is greater than sessionTimeout!");
        }
        this.numberOfSecondsInMemoryContextualiser = numberOfSecondsInMemoryContextualiser;
    }

}
