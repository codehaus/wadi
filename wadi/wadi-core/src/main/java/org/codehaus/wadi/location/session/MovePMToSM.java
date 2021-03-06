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
public class MovePMToSM extends SessionRequestImpl implements Serializable {
	protected final Peer imPeer;
    protected final String imCorrelationId;
    private final long exclusiveSessionLockWaitTime;

    public MovePMToSM(Object id, Peer im, String imCorrelationId, long exclusiveSessionLockWaitTime) {
        super(id);
        if (null == im) {
            throw new IllegalArgumentException("im is required");
        } else if (null == imCorrelationId) {
            throw new IllegalArgumentException("imCorrelationId is required");
        }
        this.imPeer = im;
        this.imCorrelationId = imCorrelationId;
        this.exclusiveSessionLockWaitTime = exclusiveSessionLockWaitTime;
    }

    public Peer getIMPeer() {
        return imPeer;
    }

    public String getIMCorrelationId() {
        return imCorrelationId;
    }

    public SessionResponseMessage newResponseFailure() {
        throw new UnsupportedOperationException();
    }
    
    public long getExclusiveSessionLockWaitTime() {
        return exclusiveSessionLockWaitTime;
    }

    public String toString() {
        return "<MovePMToSM:" + id + ">";
    }
    
}
