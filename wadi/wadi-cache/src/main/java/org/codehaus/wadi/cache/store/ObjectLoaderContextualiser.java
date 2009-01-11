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
import org.codehaus.wadi.core.contextualiser.AbstractSharedContextualiser;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.motable.BaseEmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectLoaderContextualiser extends AbstractSharedContextualiser {
    private final ObjectLoader objectLoader;
    private final SessionFactory sessionFactory;
    private final SessionMonitor sessionMonitor;
    private final StateManager stateManager;
    private final ReplicationManager replicationManager;
    private final Emoter emoter;
    
    public ObjectLoaderContextualiser(Contextualiser next,
            ObjectLoader objectLoader,
            SessionFactory sessionFactory,
            SessionMonitor sessionMonitor,
            StateManager stateManager,
            ReplicationManager replicationManager) {
        super(next);
        if (null == objectLoader) {
            throw new IllegalArgumentException("objectStore is required");
        } else if (null == sessionFactory) {
            throw new IllegalArgumentException("sessionFactory is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.objectLoader = objectLoader;
        this.sessionFactory = sessionFactory;
        this.sessionMonitor = sessionMonitor;
        this.stateManager = stateManager;
        this.replicationManager = replicationManager;
        
        emoter = new BaseEmoter();
    }

    @Override
    protected Motable get(String id, boolean exclusiveOnly) {
        ObjectInfoEntry objectInfoEntry = loadObjectInfoEntry(id);
        if (null == objectInfoEntry) {
            return null;
        }
        
        stateManager.insert(id);
        Session session = createSession(id, objectInfoEntry);
        sessionMonitor.notifySessionCreation(session);
        
        replicationManager.create(id, session);
        
        return session;
    }

    protected ObjectInfoEntry loadObjectInfoEntry(String id) {
        Object object = objectLoader.load(id);
        if (null == object) {
            return null;
        }
        return new ObjectInfoEntry(id, new ObjectInfo(object));
    }

    protected Session createSession(String id, ObjectInfoEntry objectInfoEntry) {
        Session session = sessionFactory.create();
        long timeAsMs = System.currentTimeMillis();
        session.init(timeAsMs, timeAsMs, 0, id);
        SessionUtil.setObjectInfoEntry(session, objectInfoEntry);
        return session;
    }
    
    @Override
    public Immoter getSharedDemoter() {
        return next.getSharedDemoter();
    }

    @Override
    protected Emoter getEmoter() {
        return emoter;
    }

    @Override
    protected Immoter getImmoter() {
        throw new UnsupportedOperationException();
    }

}
