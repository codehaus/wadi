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
package org.codehaus.wadi.tribes;

import org.codehaus.wadi.group.Address;

/**
 * 
 * @version $Revision: 1538 $
 */
public class TribesClusterAddress implements Address {
    private final TribesPeer[] peers;

    public TribesClusterAddress(TribesPeer[] peers) {
        if (null == peers) {
            throw new IllegalArgumentException("peers is required");
        } else if (peers.length == 0) {
            throw new IllegalArgumentException("peers must not be empty");
        }
        this.peers = peers;
    }

    public TribesPeer[] getPeers() {
        return peers;
    }
    
}
