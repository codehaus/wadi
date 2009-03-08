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

import java.net.URI;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import junit.framework.TestCase;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.StoreCache;
import org.codehaus.wadi.group.StaticDispatcherRegistry;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.web.impl.URIEndPoint;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class SmokeTest extends TestCase {

    public void testSmoke() throws Exception {
        VMBroker broker = new VMBroker("CLUSTER");
        broker.start();
        
        VMDispatcher node1Dispatcher = new VMDispatcher(broker, "NODE1", new URIEndPoint(URI.create("mock")));
        node1Dispatcher.start();
        
        StaticDispatcherRegistry dispatcherRegistry = new StaticDispatcherRegistry();
        dispatcherRegistry.register(node1Dispatcher);
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistence-unit");

        OpenJPAEntityManagerFactory oemf = OpenJPAPersistence.cast(emf);
        StoreCache cache = oemf.getStoreCache();
        
        EntityManager em = emf.createEntityManager();
        
        BasicEntity entity = new BasicEntity();
        entity.setId(1);

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(entity);
        assertFalse(cache.contains(BasicEntity.class, 1));
        tx.commit();
        assertTrue(cache.contains(BasicEntity.class, 1));
        
        tx.begin();
        BasicEntity foundEntity = em.find(BasicEntity.class, 1);
        em.remove(foundEntity);
        assertTrue(cache.contains(BasicEntity.class, 1));
        tx.commit();
        assertFalse(cache.contains(BasicEntity.class, 1));
    }
    
}
