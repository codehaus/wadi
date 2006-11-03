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
import java.util.Collection;

import org.codehaus.wadi.PartitionResponseMessage;
import org.codehaus.wadi.group.Peer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionRepopulateResponse implements PartitionResponseMessage, Serializable {
    protected final Peer peer;
    protected final Collection[] _keys;

    public PartitionRepopulateResponse(Peer peer, Collection[] keys) {
        if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        } else if (null == keys) {
            throw new IllegalArgumentException("keys is required");            
        }
        this.peer = peer;
        _keys = keys;
    }

    public Peer getPeer() {
        return peer;
    }

    public Collection[] getKeys() {
        return _keys;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("<PartitionRepopulateResponse: ");
        for (int i = 0; i < _keys.length; i++) {
            Collection c = _keys[i];
            if (c != null)
                buffer.append("" + i + ":" + c.toString() + ", ");
        }
        buffer.append(">");
        return buffer.toString();
    }

}
