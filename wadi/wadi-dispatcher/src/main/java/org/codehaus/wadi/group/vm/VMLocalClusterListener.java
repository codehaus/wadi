/**
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.group.vm;

import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMLocalClusterListener implements ClusterListener {
    private final ClusterListener delegate;
    private final Peer node;

    public VMLocalClusterListener(ClusterListener delegate, Peer node) {
        this.delegate = delegate;
        this.node = node;
    }

    public void onCoordinatorChanged(ClusterEvent event) {
        delegate.onCoordinatorChanged(event);
    }

    public void onPeerAdded(ClusterEvent event) {
        if (event.getPeer().equals(node)) {
            return;
        }
        delegate.onPeerAdded(event);
    }

    public void onPeerFailed(ClusterEvent event) {
        if (event.getPeer().equals(node)) {
            return;
        }
        delegate.onPeerFailed(event);
    }

    public void onPeerRemoved(ClusterEvent event) {
        if (event.getPeer().equals(node)) {
            return;
        }
        delegate.onPeerRemoved(event);
    }

    public void onPeerUpdated(ClusterEvent event) {
        if (event.getPeer().equals(node)) {
            return;
        }
        
        delegate.onPeerUpdated(event);
    }
}
