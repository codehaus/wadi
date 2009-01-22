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

import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.core.contextualiser.AbstractSharedContextualiser;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
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
public class ObjectWriterContextualiser extends AbstractSharedContextualiser {
    private final ObjectWriter objectWriter;
    private final SessionFactory sessionFactory;
    private final StateManager stateManager;
    private final ReplicationManager replicationManager;
    private final Emoter emoter;
    private final Immoter immoter;
    
    public ObjectWriterContextualiser(Contextualiser next,
            ObjectWriter objectWriter,
            SessionFactory sessionFactory,
            StateManager stateManager,
            ReplicationManager replicationManager) {
        super(next);
        if (null == objectWriter) {
            throw new IllegalArgumentException("objectWriter is required");
        } else if (null == sessionFactory) {
            throw new IllegalArgumentException("sessionFactory is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.objectWriter = objectWriter;
        this.sessionFactory = sessionFactory;
        this.stateManager = stateManager;
        this.replicationManager = replicationManager;
        
        emoter = new BaseEmoter();
        immoter = new ObjectWriterContextualiserImmoter();
    }

    @Override
    protected Motable get(String id, boolean exclusiveOnly) {
        return null;
    }

    @Override
    public Immoter getDemoter(String name, Motable motable) {
        return immoter;
    }
    
    @Override
    protected Emoter getEmoter() {
        return emoter;
    }

    @Override
    protected Immoter getImmoter() {
        return immoter;
    }

    protected class ObjectWriterContextualiserImmoter implements Immoter {
        public boolean contextualise(Invocation invocation, String id, Motable immotable) throws InvocationException {
            throw new UnsupportedOperationException();
        }

        public boolean immote(Motable emotable, Motable immotable) {
            Session session = (Session) immotable;
            ObjectInfoEntry objectInfoEntry = SessionUtil.getObjectInfoEntry(session);
            Object object = objectInfoEntry.getObjectInfo().getObject();
            String key = session.getName();
            objectWriter.write(key, object);
            stateManager.remove(key);
            replicationManager.destroy(key);
            return true;
        }

        public Motable newMotable(Motable emotable) {
            return sessionFactory.create();
        }
    }

}
