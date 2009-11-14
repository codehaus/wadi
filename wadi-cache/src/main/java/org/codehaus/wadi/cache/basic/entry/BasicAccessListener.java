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

package org.codehaus.wadi.cache.basic.entry;

import java.util.Set;

import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicAccessListener implements AccessListener {
    private final LocalPeer localPeer;

    public BasicAccessListener(LocalPeer localPeer) {
        if (null == localPeer) {
            throw new IllegalArgumentException("localPeer is required");
        }
        this.localPeer = localPeer;
    }

    public void enterReadOnlyAccess(ObjectInfoEntry objectInfoEntry) {
        objectInfoEntry.addReadOnlyPeer(localPeer);
    }

    public void exitReadOnlyAccess(ObjectInfoEntry objectInfoEntry) {
        objectInfoEntry.removeReadOnlyPeer(localPeer);
    }

    public void enterOptimisticAccess(ObjectInfoEntry objectInfoEntry) {
        objectInfoEntry.removeReadOnlyPeer(localPeer);
    }

    public void exitOptimisticAccess(ObjectInfoEntry objectInfoEntry) {
    }

    public void enterExclusiveAccess(ObjectInfoEntry objectInfoEntry) {
        objectInfoEntry.removeReadOnlyPeer(localPeer);

        Set<Peer> readOnlyPeers = objectInfoEntry.getReadOnlyPeers();
        for (Peer readOnlyPeer : readOnlyPeers) {
            enterExclusiveAccess(objectInfoEntry, readOnlyPeer);    
        }
    }

    public void exitExclusiveAccess(ObjectInfoEntry objectInfoEntry) {
        Set<Peer> readOnlyPeers = objectInfoEntry.getReadOnlyPeers();
        for (Peer readOnlyPeer : readOnlyPeers) {
            exitExclusiveAccess(objectInfoEntry, readOnlyPeer);    
        }
    }

    protected void enterExclusiveAccess(ObjectInfoEntry objectInfoEntry, Peer readOnlyPeer) {
    }
    
    protected void exitExclusiveAccess(ObjectInfoEntry objectInfoEntry, Peer readOnlyPeer) {
    }

}
