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
package org.codehaus.wadi.impl;

import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RWLockListener;
import org.codehaus.wadi.ReplicableSessionConfig;
import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.impl.DistributableSession;

/**
 * A DistributableSession enhanced with functionality associated with replication - the frequent 'backing-up' of its content to provide against catastrophic failure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class ReplicableSession extends DistributableSession implements RWLockListener {

	protected final transient Replicater _replicater;

	public ReplicableSession(ReplicableSessionConfig config) {
        super(config);
        _replicater=new DummyReplicater();
        _lock.setListener(this); // we could use a lock subclass to save space - need a lock factory in config - aargh !
    }
    
    public void readEnded() {
    	_replicater.replicate(this); // checks for dirtiness and replicates
    }

    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
    	super.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
    	_replicater.create(this);        
    }
    
	public void copy(Motable motable) throws Exception {
		super.copy(motable);
		_replicater.create(this);
	}
    
    public void destroy() {
    	_replicater.destroy(this);
    	super.destroy();
    }
}
