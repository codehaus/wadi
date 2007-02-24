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
package org.codehaus.wadi.location.session;

import java.io.Serializable;

import org.codehaus.wadi.group.Peer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class EvacuateIMToPM extends SessionRequestImpl implements Serializable {
    private final Peer peer;
    
    public EvacuateIMToPM(String name, Peer peer) {
        super(name);
        if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        }
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public SessionResponseMessage newResponseFailure() {
        return new EvacuatePMToIM(false);
    }

    public String toString() {
        return "<EvacuateIMToPM: " + _key + ">";
    }

}
