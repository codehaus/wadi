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

package org.codehaus.wadi.cache.basic.commitphase;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.cache.TransactionException;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.OptimisticUpdateException;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryState;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.SessionAlreadyExistException;
import org.codehaus.wadi.core.session.Session;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class InsertPhase implements CommitPhase {
    private final Manager manager;

    public InsertPhase(Manager manager) {
        if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        }
        this.manager = manager;
    }

    public void execute(Map<Object, CacheEntry> keyToEntry) throws TransactionException {
        Set<Session> createdSessions = new HashSet<Session>();
        try {
            for (Map.Entry<Object, CacheEntry> entry : keyToEntry.entrySet()) {
                Object key = entry.getKey();
                CacheEntry cacheEntry = entry.getValue();
                if (cacheEntry.getState() == CacheEntryState.INSERTED) {
                    Session session = manager.createWithName(key);
                    SessionUtil.setObjectInfoEntry(session, new ObjectInfoEntry(key, cacheEntry.getObjectInfo()));
                    cacheEntry.acquireExclusiveLock();
                    createdSessions.add(session);
                }
            }
        } catch (SessionAlreadyExistException e) {
            for (Session session : createdSessions) {
                try {
                    session.destroy();
                } catch (Exception ignoredException) {
                }
            }
            throw new OptimisticUpdateException(e);
        }
    }

}
