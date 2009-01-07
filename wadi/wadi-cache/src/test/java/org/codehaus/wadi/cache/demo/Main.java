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

import java.io.Serializable;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

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
import org.codehaus.wadi.cache.store.ObjectLoaderSupport;
import org.codehaus.wadi.cache.util.TxDecoratorCache;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.web.impl.URIEndPoint;

public class Main {

    private static final POJO OBJECT_STORE_OBJECT = new POJO();
    private static final String OBJECT_STORE_OBJECT_KEY = "key2";
    
    private VMBroker broker;

    public static void main(String[] args) throws Exception {
        new Main().doMain(args);    
    }
    
    public void doMain(String[] args) throws Exception {
        broker = new VMBroker("broker");
        broker.start();
        
        Cache cacheOnNode1 = newCache("node1");
        Cache cacheOnNode2 = newCache("node2");

        loadFromObjectStoreContextualiser(cacheOnNode1);
            
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

    protected void loadFromObjectStoreContextualiser(Cache cacheOnNode1) {
        CacheTransaction cacheTxOnNode1 = cacheOnNode1.getCacheTransaction();
        cacheTxOnNode1.begin();
        Object actualObject = cacheOnNode1.get(OBJECT_STORE_OBJECT_KEY, ReadOnlyAcquisitionPolicy.DEFAULT);
        cacheTxOnNode1.commit();
        if (actualObject != OBJECT_STORE_OBJECT) {
            throw new AssertionError();
        }
    }

    protected Cache newCache(String nodeName) throws Exception {
        VMDispatcher dispatcher = new VMDispatcher(broker, nodeName, new URIEndPoint(new URI("uri")));
        dispatcher.start();
        
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("/name")), dispatcher, 2) {
            @Override
            protected Contextualiser newSharedStoreContextualiser(Contextualiser next) {
                return new ObjectLoaderContextualiser(next, new ObjectLoaderSupport() {
                    public Object load(String key) {
                        if (key.equals(OBJECT_STORE_OBJECT_KEY)) {
                            return OBJECT_STORE_OBJECT;
                        }
                        return null;
                    }
                });
            }  
        };
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

    public static class POJO implements Serializable {
        public int field;
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
