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

package org.codehaus.wadi.cache.basic;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.codehaus.wadi.group.Peer;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectInfoEntry implements Serializable {
    private final Object id;
    private final ObjectInfo objectInfo;
    private final Set<Peer> readOnlyPeers;
    
    public ObjectInfoEntry(Object id, ObjectInfo objectInfo) {
        if (null == id) {
            throw new IllegalArgumentException("key is required");
        } else if (null == objectInfo) {
            throw new IllegalArgumentException("objectInfo is required");
        }
        this.id = id;
        this.objectInfo = objectInfo;

        readOnlyPeers = new CopyOnWriteArraySet<Peer>();
    }
    
    public void addReadOnlyPeer(Peer peer) {
        readOnlyPeers.add(peer);
    }
    
    public void removeReadOnlyPeer(Peer peer) {
        readOnlyPeers.remove(peer);
    }

    public Set<Peer> getReadOnlyPeers() {
        return Collections.unmodifiableSet(readOnlyPeers);
    }

    public ObjectInfo getObjectInfo() {
        return objectInfo;
    }

    public Object getId() {
        return id;
    }

}
