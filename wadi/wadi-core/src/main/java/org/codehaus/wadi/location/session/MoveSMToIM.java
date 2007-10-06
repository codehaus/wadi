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

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.replication.common.ReplicaInfo;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class MoveSMToIM implements Serializable {

	protected final boolean sessionBuzy;
	protected final Motable motable;
    private final ReplicaInfo replicaInfo;

    public MoveSMToIM() {
        motable = null;
        replicaInfo = null;
        sessionBuzy = false;
    }
    
	public MoveSMToIM(Motable motable, ReplicaInfo replicaInfo) {
	    if (null == motable) {
            throw new IllegalArgumentException("motable is required");
        }
		this.motable = motable;
        this.replicaInfo = replicaInfo;
        
        sessionBuzy = false;
	}

	public MoveSMToIM(boolean sessionBuzy) {
	    this.sessionBuzy = sessionBuzy;
	    
	    motable = null;
	    replicaInfo = null;
	}
	
	public Motable getMotable() {
		return motable;
	}

	public ReplicaInfo getReplicaInfo() {
	    return replicaInfo;
	}
	
    public boolean isSessionBuzy() {
        return sessionBuzy;
    }

    public String toString() {
        return "<MoveSMToIM:"+motable+">";
    }

}
