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

package org.codehaus.wadi.cache.openjpa.itest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import junit.framework.TestCase;

import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.jdbc.ClientDriver;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.StoreCache;
import org.codehaus.wadi.cache.openjpa.WADIDataCache;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherRegistry;

/**
 *
 * @version $Rev:$ $Date:$
 */
public abstract class AbstractSmokeTest extends TestCase {
    private static Map<String, Dispatcher> nodeNameToDispatcher = new HashMap<String, Dispatcher>();
    
    @Override
    protected void setUp() throws Exception {
        Class.forName(ClientDriver.class.getName()).newInstance();

        Thread networkServerThread = new Thread(new Runnable() {
            public void run() {
                NetworkServerControl server;
                try {
                    server = new NetworkServerControl();
                    server.start(null);                
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        networkServerThread.start();
    }
    
    public void testSmoke() throws Exception {
        CacheInfo cacheInfo1 = newCacheInfo("NODE1");
        CacheInfo cacheInfo2 = newCacheInfo("NODE2");

        insert(cacheInfo1);
        retrieveAndRemove(cacheInfo2);
        insert(cacheInfo2);
        assertContains(cacheInfo1);
        assertContains(cacheInfo2);
        retrieveAndRemove(cacheInfo1);
    }

    private void insert(CacheInfo cacheInfo) {
        BasicEntity entity = new BasicEntity();
        entity.setId(1);

        EntityTransaction tx = cacheInfo.em.getTransaction();
        tx.begin();
        cacheInfo.em.persist(entity);
        assertDoesNotContain(cacheInfo);
        tx.commit();
        assertContains(cacheInfo);
    }

    private void retrieveAndRemove(CacheInfo cacheInfo) {
        EntityTransaction tx = cacheInfo.em.getTransaction();
        tx.begin();
        BasicEntity foundEntity = cacheInfo.em.find(BasicEntity.class, 1);
        assertNotNull(foundEntity);
        assertContains(cacheInfo);
        cacheInfo.em.remove(foundEntity);
        tx.commit();
        assertDoesNotContain(cacheInfo);
    }
    
    private void assertDoesNotContain(CacheInfo cacheInfo) {
        assertFalse(cacheInfo.cache.contains(BasicEntity.class, 1));
    }
    
    private void assertContains(CacheInfo cacheInfo) {
        assertTrue(cacheInfo.cache.contains(BasicEntity.class, 1));
    }

    private CacheInfo newCacheInfo(String nodeName) throws Exception {
        Dispatcher dispatcher = newDispatcher(nodeName);
        dispatcher.start();
        
        nodeNameToDispatcher.put(nodeName, dispatcher);
        
        Map properties = new HashMap();
        properties.put("openjpa.DataCache", WADIDataCache.class.getName() + "(clusterName=" + nodeName
                + ", name=default, dispatcherRegistryClass=" + MockRegistry.class.getName() + ")");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistence-unit", properties);
        return new CacheInfo(emf, nodeName);
    }

    protected abstract Dispatcher newDispatcher(String nodeName) throws Exception;
    
    public class CacheInfo {
        private final EntityManager em;
        private final StoreCache cache;

        public CacheInfo(EntityManagerFactory emf, String nodeName) {
            OpenJPAEntityManagerFactory oemf1 = OpenJPAPersistence.cast(emf);
            cache = oemf1.getStoreCache();
            em = emf.createEntityManager();
        }
    }

    public static class MockRegistry implements DispatcherRegistry {
        public Dispatcher getDispatcherByClusterName(String clusterName) throws IllegalStateException {
            return nodeNameToDispatcher.get(clusterName);
        }

        public Collection getDispatchers() {
            throw new UnsupportedOperationException();
        }

        public void register(Dispatcher dispatcher) {
            throw new UnsupportedOperationException();
        }

        public void unregister(Dispatcher dispatcher) {
            throw new UnsupportedOperationException();
        }
    }
    
}
