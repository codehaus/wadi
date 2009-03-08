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

import java.util.Collection;
import java.util.Collections;

import org.apache.openjpa.datacache.DataCachePCData;
import org.apache.openjpa.lib.log.Log;
import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheTransaction;
import org.codehaus.wadi.cache.policy.OptimisticAcquisitionPolicy;
import org.codehaus.wadi.cache.policy.PessimisticAcquisitionPolicy;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.StaticDispatcherRegistry;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class WADIDataCacheTest extends RMockTestCase {

    public void testLocateDispatcher() throws Exception {
        StaticDispatcherRegistry registry = new StaticDispatcherRegistry();
        Dispatcher dispatcher = (Dispatcher) mock(Dispatcher.class);
        dispatcher.getCluster().getClusterName();
        String clusterName = "clusterName";
        modify().returnValue(clusterName);
        registry.register(dispatcher);

        startVerification();
        
        WADIDataCache cache = new WADIDataCache();
        cache.setDispatcherRegistryClass(StaticDispatcherRegistry.class.getName());
        cache.setClusterName(clusterName);
        
        Dispatcher locatedDispatcher = cache.locateDispatcher();
        assertSame(dispatcher, locatedDispatcher);
    }
    
    public void testBuildServiceSpaceNameWithoutGlobalDiscriminator() throws Exception {
        WADIDataCache cache = new WADIDataCache();
        String clusterName = "clusterName";
        cache.setClusterName(clusterName);
        String name = "name";
        cache.setName(name);
        
        ServiceSpaceName serviceSpaceName = cache.buildServiceSpaceName();
        assertEquals(clusterName + "/" + name, serviceSpaceName.toString());
    }
    
    public void testBuildServiceSpaceNameWithGlobalDiscriminator() throws Exception {
        WADIDataCache cache = new WADIDataCache();
        String clusterName = "clusterName";
        cache.setClusterName(clusterName);
        String name = "name";
        cache.setName(name);
        String globalDiscriminatorName = "globalDiscriminator";
        cache.setGlobalDiscriminatorName(globalDiscriminatorName);
        
        ServiceSpaceName serviceSpaceName = cache.buildServiceSpaceName();
        assertEquals(clusterName + "/" + globalDiscriminatorName + "/" + name, serviceSpaceName.toString());
    }
    
    public void testPrecommitNotTransactionalStartCacheTransaction() throws Exception {
        Cache underlyingCache = (Cache) mock(Cache.class);
        
        CacheTransaction cacheTransaction = underlyingCache.getCacheTransaction();
        cacheTransaction.begin();
        
        startVerification();
        
        WADIDataCache cache = newWADIDataCache(underlyingCache);
        cache.preCommit();
    }

    public void testPostCommitNotTransactionalCommitCacheTransaction() throws Exception {
        Cache underlyingCache = (Cache) mock(Cache.class);
        
        CacheTransaction cacheTransaction = underlyingCache.getCacheTransaction();
        cacheTransaction.commit();
        
        startVerification();
        
        WADIDataCache cache = newWADIDataCache(underlyingCache);
        cache.postCommit();
    }
    
    public void testCommit() throws Exception {
        final Cache underlyingCache = (Cache) mock(Cache.class);
        final Log mockLog = (Log) mock(Log.class);
        
        DataCachePCData addition = newDataCachePCData("1");
        DataCachePCData newUpdate = newDataCachePCData("2");
        DataCachePCData existingUpdate = newDataCachePCData("3");

        underlyingCache.insert("1", addition, null);
        underlyingCache.insert("2", newUpdate, null);
        underlyingCache.get("3", PessimisticAcquisitionPolicy.DEFAULT);
        underlyingCache.update("3", existingUpdate);
        underlyingCache.delete("4", OptimisticAcquisitionPolicy.DEFAULT);
        
        mockLog.isTraceEnabled();
        
        startVerification();
        
        WADIDataCache cache = new WADIDataCache() {
            @Override
            protected Cache createAndStartCache() throws Exception {
                log = mockLog;
                return underlyingCache;
            }
        };
        cache.setTransactional(true);
        cache.setClusterName("clusterName");
        cache.setName("name");
        cache.setDispatcherRegistryClass(StaticDispatcherRegistry.class.getName());
        cache.endConfiguration();

        Collection additions = Collections.singleton(addition);
        Collection newUpdates = Collections.singleton(newUpdate);
        Collection existingUpdates = Collections.singleton(existingUpdate);
        Collection deletes = Collections.singleton("4");
        cache.commit(additions, newUpdates, existingUpdates, deletes);
    }

    private WADIDataCache newWADIDataCache(final Cache underlyingCache) throws Exception {
        WADIDataCache cache = new WADIDataCache() {
            @Override
            protected Cache createAndStartCache() throws Exception {
                return underlyingCache;
            }
        };
        cache.setClusterName("clusterName");
        cache.setName("name");
        cache.setDispatcherRegistryClass(StaticDispatcherRegistry.class.getName());
        cache.endConfiguration();
        return cache;
    }

    private DataCachePCData newDataCachePCData(String id) {
        DataCachePCData pc = (DataCachePCData) mock(DataCachePCData.class);
        pc.getId();
        modify().multiplicity(expect.from(1)).returnValue(id);
        return pc;
    }
    
}
