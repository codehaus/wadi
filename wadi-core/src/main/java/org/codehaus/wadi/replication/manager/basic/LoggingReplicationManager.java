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

package org.codehaus.wadi.replication.manager.basic;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.manager.ReplicationKeyNotFoundException;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class LoggingReplicationManager implements ReplicationManager {
    private static final Log LOG = LogFactory.getLog(LoggingReplicationManager.class);
    
    private final ReplicationManager delegate;

    public LoggingReplicationManager(ReplicationManager delegate) {
        if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.delegate = delegate;
    }

    public void create(Object key, Motable tmp) throws ReplicationKeyAlreadyExistsException,
            InternalReplicationManagerException {
        LOG.debug(delegate + " - create key [" + key + "]");
        delegate.create(key, tmp);
    }

    public void destroy(Object key) {
        LOG.debug(delegate + " - destroy key [" + key + "]");
        delegate.destroy(key);
    }

    public Set<Object> getManagedReplicaInfoKeys() {
        LOG.debug(delegate + " - getManagedReplicaInfoKeys");
        return delegate.getManagedReplicaInfoKeys();
    }

    public void insertReplicaInfo(Object key, ReplicaInfo replicaInfo) throws ReplicationKeyAlreadyExistsException {
        LOG.debug(delegate + " - insertReplicaInfo key [" + key + "]");
        delegate.insertReplicaInfo(key, replicaInfo);
    }

    public ReplicaInfo releaseReplicaInfo(Object key, Peer newPrimary) {
        LOG.debug(delegate + " - releaseReplicaInfo key [" + key + "]");
        return delegate.releaseReplicaInfo(key, newPrimary);
    }

    public Motable retrieveReplica(Object key) throws InternalReplicationManagerException {
        LOG.debug(delegate + " - retrieveReplica key [" + key + "]");
        return delegate.retrieveReplica(key);
    }

    public void promoteToMaster(Object key, ReplicaInfo replicaInfo, Motable motable, Peer blackListedSecondary)
            throws InternalReplicationManagerException {
        LOG.debug(delegate + " - promoteToMaster key [" + key + "]");
        delegate.promoteToMaster(key, replicaInfo, motable, blackListedSecondary);
    }
    
    public void start() throws Exception {
        LOG.debug(delegate + " - start");
        delegate.start();
    }

    public void stop() throws Exception {
        LOG.debug(delegate + " - stop");
        delegate.stop();
    }

    public void update(Object key, Motable tmp) throws ReplicationKeyNotFoundException,
            InternalReplicationManagerException {
        LOG.debug(delegate + " - update key [" + key + "]");
        delegate.update(key, tmp);
    }

}
