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
package org.codehaus.wadi.web.impl;

import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RWLockListener;
import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.web.ReplicableSessionConfig;

/**
 * A DistributableSession enhanced with functionality associated with replication - the frequent 'backing-up' of its content to provide against catastrophic failure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1725 $
 */

public abstract class AbstractReplicableSession extends DistributableSession implements RWLockListener {
	
	public AbstractReplicableSession(ReplicableSessionConfig config) {
        super(config);
        _lock.setListener(this); // we could use a lock subclass to save space - need a lock factory in config - aargh !
    }
    
    public abstract void readEnded();
    
    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
    	super.init(creationTime, lastAccessedTime, maxInactiveInterval, name);

    	Replicater replicater=((ReplicableSessionConfig)_config).getReplicater();
    	replicater.create(this);
    }

    public void init2(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
    	super.init(creationTime, lastAccessedTime, maxInactiveInterval, name);

    	Replicater replicater=((ReplicableSessionConfig)_config).getReplicater();

    	if (!replicater.getReusingStore()) {
            replicater.create(this);
        }
    }
    

    public void copy(Motable motable) throws Exception {
        super.copy(motable);

        Replicater replicater=((ReplicableSessionConfig)_config).getReplicater();
        
    	if (!replicater.getReusingStore()) {
            replicater.create(this);
        } else {
            replicater.acquireFromOtherReplicater(this);
        }
	}

    public void mote(Motable recipient) throws Exception {
    	Replicater replicater=((ReplicableSessionConfig)_config).getReplicater();
    	if (replicater.getReusingStore()) {
    		recipient.init(_creationTime, _lastAccessedTime, _maxInactiveInterval, _name); // only copy metadata
    	} else {
            recipient.copy(this); // copy metadata and data
        }
    	
    	destroy(recipient); // this is a transfer, so use special case destructor...
    }

    // we have two destroy usecases :
    
    // destruction through explicit (by app code) or implicit (by container timeout) invalidation
    // MUST destroy replicated backups - else we spring a leak...
    
    public void destroy() throws Exception {
    	((ReplicableSessionConfig)_config).getReplicater().destroy(this);
    	super.destroy();
    }
    
    // destruction as our data is transferred to another storage medium.
    // if this is the same medium as we are using for replication, we do not want to remove our replicated copies...
    public void destroy(Motable recipient) throws Exception {
    	Replicater replicater=((ReplicableSessionConfig)_config).getReplicater();

    	if (!replicater.getReusingStore()) {
            replicater.destroy(this);
        }

        super.destroy();
    }

    // lastAccessedTime NOT replicated - assume that, just as the node died, requests
    // were landing for every session that it contained. All the calls the setLastAccessedTime()
    // that should have happened were lost. So we have to refresh any session replicant as it is
    // promoted from slave to master to make up for this possibility. If we are going to do this,
    // then it would be redundant and very expensive to replicate this data, since it will change
    // with every request.
    
}
