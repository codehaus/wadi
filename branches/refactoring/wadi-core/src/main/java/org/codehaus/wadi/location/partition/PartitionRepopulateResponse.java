/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.location.partition;

import java.io.Serializable;
import java.util.Map;

import org.codehaus.wadi.PartitionResponseMessage;
import org.codehaus.wadi.group.Peer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionRepopulateResponse implements PartitionResponseMessage, Serializable {
    private final Peer peer;
    private final Map keyToSessionNames;

    public PartitionRepopulateResponse(Peer peer, Map keyToSessionNames) {
        if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        } else if (null == keyToSessionNames) {
            throw new IllegalArgumentException("keyToSessionNames is required");            
        }
        this.peer = peer;
        this.keyToSessionNames = keyToSessionNames;
    }

    public Peer getPeer() {
        return peer;
    }

    public Map getKeyToSessionNames() {
        return keyToSessionNames;
    }

    public String toString() {
        return "PartitionRepopulateResponse [" + keyToSessionNames + "]";
    }

}
