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

package org.codehaus.wadi.cache.store;

import java.io.IOException;

import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.Expression;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectLoaderContextualiserTest extends RMockTestCase {

    private Contextualiser next;
    private ObjectLoader loader;
    private ObjectLoaderContextualiser contextualiser;
    private SessionFactory sessionFactory;
    private SessionMonitor sessionMonitor;
    private StateManager stateManager;
    private ReplicationManager replicationManager;

    @Override
    protected void setUp() throws Exception {
        next = (Contextualiser) mock(Contextualiser.class);
        loader = (ObjectLoader) mock(ObjectLoader.class);
        sessionFactory = (SessionFactory) mock(SessionFactory.class);
        sessionMonitor = (SessionMonitor) mock(SessionMonitor.class);
        stateManager = (StateManager) mock(StateManager.class);
        replicationManager = (ReplicationManager) mock(ReplicationManager.class);
        contextualiser = new ObjectLoaderContextualiser(next,
                loader,
                sessionFactory,
                sessionMonitor,
                stateManager,
                replicationManager);
    }

    public void testGetSharedDemoterReturnsNextSharedDemoter() throws Exception {
        Immoter expectedImmoter = next.getSharedDemoter();
        
        startVerification();
        
        Immoter immoter = contextualiser.getSharedDemoter();
        assertSame(expectedImmoter, immoter);
    }
    
    public void testGetLoadObject() throws Exception {
        String key = "key";
        
        loader.load(key);
        Object expectedObject = new Object();
        modify().returnValue(expectedObject);
        
        Session session = recordNotifyObjectLoad(key, expectedObject);
        
        startVerification();
        
        Motable motable = contextualiser.get(key, false);
        assertSame(session, motable);
    }

    public void testGetLoadNullObject() throws Exception {
        String key = "key";
        
        loader.load(key);

        startVerification();
        
        Motable motable = contextualiser.get(key, false);
        assertNull(motable);
    }
    
    private Session recordNotifyObjectLoad(String key, final Object expectedObject) {
        stateManager.insert(key);
        
        Session session = sessionFactory.create();
        session.init(0, 0, 0, key);
        modify().args(new Expression[] {is.gt(0l), is.gt(0l), is.AS_RECORDED, is.AS_RECORDED});
        SessionUtil.setObjectInfoEntry(session, new ObjectInfoEntry(key, new ObjectInfo()));
        modify().args(is.AS_RECORDED, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                ObjectInfoEntry entry = (ObjectInfoEntry) arg0;
                assertSame(expectedObject, entry.getObjectInfo().getObject());
                return true;
            }
        });
        
        sessionMonitor.notifySessionCreation(session);
        
        replicationManager.create(key, session);
        
        return session;
    }
    
}
