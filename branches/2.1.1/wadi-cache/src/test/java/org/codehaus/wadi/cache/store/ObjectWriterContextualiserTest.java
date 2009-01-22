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

import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectWriterContextualiserTest extends RMockTestCase {

    private Contextualiser next;
    private ObjectWriter objectWriter;
    private SessionFactory sessionFactory;
    private StateManager stateManager;
    private ReplicationManager replicationManager;
    private ObjectWriterContextualiser contextualiser;

    @Override
    protected void setUp() throws Exception {
        next = (Contextualiser) mock(Contextualiser.class);
        objectWriter = (ObjectWriter) mock(ObjectWriter.class);
        sessionFactory = (SessionFactory) mock(SessionFactory.class);
        stateManager = (StateManager) mock(StateManager.class);
        replicationManager = (ReplicationManager) mock(ReplicationManager.class);
        contextualiser = new ObjectWriterContextualiser(next,
                objectWriter,
                sessionFactory,
                stateManager,
                replicationManager);
    }
    
    public void testImmoterImmoteWritesObjectAndNotifiesStateAndReplicationManagers() throws Exception {
        Session session = (Session) mock(Session.class);
        String key = "key";
        session.getName();
        modify().returnValue(key);
        
        SessionUtil.getObjectInfoEntry(session);
        Object expectedObject = new Object();
        modify().returnValue(new ObjectInfoEntry(key, new ObjectInfo(expectedObject)));

        objectWriter.write(key, expectedObject);
        stateManager.remove(key);
        replicationManager.destroy(key);
        
        startVerification();
        
        Immoter immoter = contextualiser.getImmoter();
        immoter.immote(null, session);
    }
    
    public void testImmoterNewMotableDelegatesToSessionFactory() throws Exception {
        Session session = sessionFactory.create();
        
        startVerification();
        
        Immoter immoter = contextualiser.getImmoter();
        Motable motable = immoter.newMotable(null);
        assertSame(session, motable);
    }
    
}
