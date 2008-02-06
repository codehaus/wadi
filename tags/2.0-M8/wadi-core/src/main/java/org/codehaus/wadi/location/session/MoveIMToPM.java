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
public class MoveIMToPM extends SessionRequestImpl implements Serializable {
    private final Peer peer;
    private final boolean relocateSession;
    private final boolean relocateInvocation;
    private final long exclusiveSessionLockWaitTime;

    public MoveIMToPM(Peer peer,
            String sessionName,
            boolean relocateSession,
            boolean relocateInvocation,
            long exclusiveSessionLockWaitTime) {
        super(sessionName);
        if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        }
        this.peer = peer;
        this.relocateSession = relocateSession;
        this.relocateInvocation = relocateInvocation;
        this.exclusiveSessionLockWaitTime = exclusiveSessionLockWaitTime;
    }

    public boolean isRelocateSession() {
        return relocateSession;
    }

    public boolean isRelocateInvocation() {
        return relocateInvocation;
    }

    public Peer getIMPeer() {
        return peer;
    }

    public SessionResponseMessage newResponseFailure() {
        return new MovePMToIM();
    }
    
    public long getExclusiveSessionLockWaitTime() {
        return exclusiveSessionLockWaitTime;
    }

    public String toString() {
        return "MoveIMToPM [" + _key + "]->[" + peer + "]";
    }
    
}
