/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.demo;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheTransaction;
import org.codehaus.wadi.cache.basic.core.BasicCache;
import org.codehaus.wadi.cache.basic.core.BasicGlobalObjectStore;
import org.codehaus.wadi.cache.basic.core.BasicInTxCacheFactory;
import org.codehaus.wadi.cache.basic.entry.AccessListener;
import org.codehaus.wadi.cache.basic.entry.BasicAccessListener;
import org.codehaus.wadi.cache.policy.OptimisticAcquisitionPolicy;
import org.codehaus.wadi.cache.policy.PessimisticAcquisitionPolicy;
import org.codehaus.wadi.cache.policy.ReadOnlyAcquisitionPolicy;
import org.codehaus.wadi.cache.store.ObjectLoaderContextualiser;
import org.codehaus.wadi.cache.store.ObjectWriterContextualiser;
import org.codehaus.wadi.cache.util.TxDecoratorCache;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.web.impl.URIEndPoint;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class Main {

    private VMBroker broker;
    private CountDownLatch waitForObjectWriteLatch = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        new Main().doMain(args);    
    }
    
    public void doMain(String[] args) throws Exception {
        broker = new VMBroker("broker");
        broker.start();
        
        Cache cacheOnNode1 = newCache("node1");
        Cache cacheOnNode2 = newCache("node2");

        readAndWriteThroughCacheAccess(cacheOnNode1);
        
        if (true) {
            return;
        }
            
        String key1 = "key1";

        CacheTransaction cacheTxOnNode1 = cacheOnNode1.getCacheTransaction();
        cacheTxOnNode1.begin();
        POJO pojo = new POJO();
        pojo.field = 1;
        cacheOnNode1.insert(key1, pojo, null);
        cacheTxOnNode1.commit();
        
        pojo = (POJO) cacheOnNode1.get(key1, ReadOnlyAcquisitionPolicy.DEFAULT);
        
        CacheTransaction cacheTxOnNode2 = cacheOnNode2.getCacheTransaction();
        cacheTxOnNode2.begin();
        pojo = (POJO) cacheOnNode2.get(key1, OptimisticAcquisitionPolicy.DEFAULT);
        pojo = (POJO) cacheOnNode2.get(key1, PessimisticAcquisitionPolicy.DEFAULT);
        pojo.field = 2;
        cacheOnNode2.update(key1);
        cacheTxOnNode2.commit();

        cacheTxOnNode1.begin();
        pojo = (POJO) cacheOnNode1.get(key1, OptimisticAcquisitionPolicy.DEFAULT);
        pojo.field = 3;
        cacheOnNode1.update(key1);
        cacheTxOnNode1.rollback();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        int nbThreads = 50;
		CountDownLatch countDownLatch = new CountDownLatch(nbThreads);
        for (int i = 0; i < nbThreads; i++) {
            new UpdatePessimistic(i % 2 == 0 ? cacheOnNode1 : cacheOnNode2, key1, startLatch, countDownLatch).start();
        }
        startLatch.countDown();
        countDownLatch.await();
        
        cacheTxOnNode1.begin();
        pojo = (POJO) cacheOnNode1.get(key1, OptimisticAcquisitionPolicy.DEFAULT);
        System.out.println(pojo.field);
        cacheTxOnNode1.commit();
        
        cacheTxOnNode1.begin();
        cacheOnNode1.delete(key1, OptimisticAcquisitionPolicy.DEFAULT);
        cacheTxOnNode1.commit();
        
        System.out.println("END");
    }

    protected void readAndWriteThroughCacheAccess(Cache cacheOnNode1) throws Exception {
        CacheTransaction cacheTxOnNode1 = cacheOnNode1.getCacheTransaction();
        cacheTxOnNode1.begin();
        POJO actualObject = (POJO) cacheOnNode1.get(StubbedObjectLoaderWriter.OBJECT_STORE_OBJECT_KEY, ReadOnlyAcquisitionPolicy.DEFAULT);
        cacheTxOnNode1.commit();
        if (actualObject.field != StubbedObjectLoaderWriter.OBJECT_STORE_OBJECT.field) {
            throw new AssertionError();
        }
        boolean success = waitForObjectWriteLatch.await(20, TimeUnit.SECONDS);
        if (!success) {
            throw new AssertionError();
        }
    }

    protected Cache newCache(String nodeName) throws Exception {
        VMDispatcher dispatcher = new VMDispatcher(broker, nodeName, new URIEndPoint(new URI("uri")));
        dispatcher.start();
        
        StackContext stackContext = new CacheStackContext(dispatcher, waitForObjectWriteLatch);
        stackContext.setDisableReplication(true);
        stackContext.build();

        ServiceSpace serviceSpace = stackContext.getServiceSpace();
        serviceSpace.start();
        
        Manager manager = stackContext.getManager();
        
        LocalPeer localPeer = serviceSpace.getLocalPeer();
        AccessListener accessListener = new BasicAccessListener(localPeer);
        Streamer streamer = new SimpleStreamer();
        BasicGlobalObjectStore globalObjectStore = new BasicGlobalObjectStore(accessListener, manager, streamer);
        
        BasicInTxCacheFactory inTxCacheFactory = new BasicInTxCacheFactory(globalObjectStore);
        Cache cache = new BasicCache(globalObjectStore, inTxCacheFactory);
        cache = new TxDecoratorCache(cache);
        return cache;
    }

    protected static final class CacheStackContext extends StackContext {
        private final StubbedObjectLoaderWriter objectLoaderWriter;

        protected CacheStackContext(Dispatcher underlyingDispatcher, CountDownLatch waitForObjectWriteLatch)
                throws Exception {
            super(Thread.currentThread().getContextClassLoader(),
                    new ServiceSpaceName(new URI("/name")),
                    underlyingDispatcher,
                    0,
                    24,
                    10,
                    new RoundRobinBackingStrategyFactory(1));
            objectLoaderWriter = new StubbedObjectLoaderWriter(waitForObjectWriteLatch);
        }

        @Override
        protected Contextualiser newSharedStoreContextualiser(Contextualiser next) {
            next = new ObjectWriterContextualiser(next,
                    objectLoaderWriter,
                    sessionFactory,
                    stateManager,
                    replicationManager);
            return new ObjectLoaderContextualiser(next,
                    objectLoaderWriter,
                    sessionFactory,
                    sessionMonitor,
                    stateManager);
        }
    }

    public static class UpdatePessimistic extends Thread {
        private final Cache cache;
        private final String key;
        private final CountDownLatch startLatch;
        private final CountDownLatch countDownLatch;
        
        public UpdatePessimistic(Cache cache, String key, CountDownLatch startLatch, CountDownLatch countDownLatch) {
            this.cache = cache;
            this.key = key;
            this.startLatch = startLatch;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            
            CacheTransaction cacheTransaction = cache.getCacheTransaction();
            cacheTransaction.begin();
            POJO pojo = (POJO) cache.get(key, PessimisticAcquisitionPolicy.DEFAULT);
            pojo.field++;
            cache.update(key);
            cacheTransaction.commit();
            
            countDownLatch.countDown();
        }
    }
    
}
