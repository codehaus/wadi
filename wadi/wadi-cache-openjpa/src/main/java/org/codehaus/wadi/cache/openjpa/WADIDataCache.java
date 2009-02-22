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

package org.codehaus.wadi.cache.openjpa;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.openjpa.datacache.AbstractDataCache;
import org.apache.openjpa.datacache.DataCachePCData;
import org.codehaus.wadi.cache.AcquisitionInfo;
import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.assembler.CacheStackContext;
import org.codehaus.wadi.cache.basic.core.BasicCache;
import org.codehaus.wadi.cache.basic.core.BasicGlobalObjectStore;
import org.codehaus.wadi.cache.basic.core.BasicInTxCacheFactory;
import org.codehaus.wadi.cache.basic.entry.AccessListener;
import org.codehaus.wadi.cache.basic.entry.BasicAccessListener;
import org.codehaus.wadi.cache.policy.OptimisticAcquisitionPolicy;
import org.codehaus.wadi.cache.policy.PessimisticAcquisitionPolicy;
import org.codehaus.wadi.cache.policy.ReadOnlyAcquisitionPolicy;
import org.codehaus.wadi.cache.util.TransactionManagerAwareCache;
import org.codehaus.wadi.cache.util.TxDecoratorCache;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.tribes.TribesDispatcher;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class WADIDataCache extends AbstractDataCache {
    private Cache cache;
    private String nodeName;

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void endConfiguration() {
        if (null == getName()) {
            throw new IllegalArgumentException("name property has not been set");
        }
        // Add persistence unit name.
        String cacheName = nodeName + "/" + getName();
        
        try {
            Dispatcher dispatcher = new TribesDispatcher("clusterName", nodeName, null);
            dispatcher.start();

            ServiceSpaceName name = new ServiceSpaceName(URI.create(cacheName));
            
            CacheStackContext stackContext = new CacheStackContext(name, dispatcher);
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
            cache = new BasicCache(globalObjectStore, inTxCacheFactory);
            cache = new TxDecoratorCache(cache);
            cache = new TransactionManagerAwareCache(cache, conf.getManagedRuntimeInstance().getTransactionManager());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    @Override
    public void commit(Collection additions, Collection newUpdates, Collection existingUpdates, Collection deletes) {
        super.commit(additions, newUpdates, Collections.EMPTY_LIST, deletes);
        
        for (Iterator iterator = existingUpdates.iterator(); iterator.hasNext();) {
            DataCachePCData pc = (DataCachePCData) iterator.next();
            cache.get(pc.getId(), PessimisticAcquisitionPolicy.DEFAULT);
            cache.update(pc.getId(), pc);
        }
    }
    
    public void writeLock() {
    }

    public void writeUnlock() {
    }

    @Override
    protected void clearInternal() {
    }

    @Override
    protected DataCachePCData getInternal(Object oid) {
        return (DataCachePCData) cache.get(oid, OptimisticAcquisitionPolicy.DEFAULT);
    }

    @Override
    protected boolean pinInternal(Object oid) {
        AcquisitionInfo pinAcquisition = new AcquisitionInfo(AcquisitionInfo.DEFAULT, true, false);
        Object object = cache.get(oid, new ReadOnlyAcquisitionPolicy(pinAcquisition));
        return null != object;
    }

    @Override
    protected DataCachePCData putInternal(Object oid, DataCachePCData pc) {
        cache.insert(oid, pc, null);
        return pc;
    }

    @Override
    protected void removeAllInternal(Class cls, boolean subclasses) {
    }

    @Override
    protected DataCachePCData removeInternal(Object oid) {
        return (DataCachePCData) cache.delete(oid, OptimisticAcquisitionPolicy.DEFAULT);
    }

    @Override
    protected boolean unpinInternal(Object oid) {
        AcquisitionInfo pinAcquisition = new AcquisitionInfo(AcquisitionInfo.DEFAULT, false, true);
        Object object = cache.get(oid, new ReadOnlyAcquisitionPolicy(pinAcquisition));
        return null != object;
    }

}
