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
import org.codehaus.wadi.NewReplicater;
import org.codehaus.wadi.impl.DistributableSession;

/**
 * A DistributableSession enhanced with functionality associated with replication - the frequent 'backing-up' of its content to provide against catastrophic failure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class ReplicableSession extends DistributableSession implements RWLockListener {

	interface Semantic {
		
	}
	
	class ValueSemantic implements Semantic {
		
	}
	
	class ReferenceSemantic implements Semantic {
		
	}
	
	interface Risk {
		
		// immediate(Replicater replicater, Object this);
		// endOfRequestGroup(Replicater replicater, Object this);
		
	}
	
	class ZeroRisk implements Risk { // immediate
		
	}
	
	class LowRisk implements Risk { // end of current request group
		
	}
	
	
	protected final transient NewReplicater _replicater;
	protected final transient Semantic _semantic=new ReferenceSemantic();
	protected final transient Risk _risk=new ZeroRisk();

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
    
    // lastAccessedTime NOT replicated - assume that, just as the node died, requests
    // were landing for every session that it contained. All the calls the setLastAccessedTime()
    // that should have happened were lost. So we have to refresh any session replicant as it is
    // promoted from slave to master to make up for this possibility. If we are going to do this,
    // then it would be redundant and very expensive to replicate this data, since it will change
    // with every request.
    
    // other session mutators...
    
    public void setMaxInactiveInterval(int maxInactiveInterval) {
    	super.setMaxInactiveInterval(maxInactiveInterval);
    	// if MII changes - dirties the session metadata
    }
    
    public Object setAttribute(String name, Object value) {
    	return super.setAttribute(name, value);
    	// I think we must assume that this always dirties the session data - it would be prohibitively
    	// expensive to check whether the value associated with key has actually been changed - this
    	// check is a job for the application to perform.
    	// we could do a quick oldObj==newObj check, but that would mess up people who are using this call to indicate session dirtiness...
    }
    
    public Object removeAttribute(String name) {
    	return super.removeAttribute(name);
    	// if name!=null - dirties the session data
    }
    
    public Object getAttribute(String name) {
    	return _attributes.get(name);
    	// gives away ref to session content - may be used to dirty the session data.
    }
    
    
    interface ReplicationGranularity {
    	
    }
    
    class SessionReplicationGranularity implements ReplicationGranularity {
    	
    }
    
    class AttributeReplicationGranularity implements ReplicationGranularity {
    	
    }
    
    interface Semantics {
    	
    }
    
    class ByValueSemantics implements Semantics {
    	
    }
    
    class ByReferenceSemantics implements Semantics {
    	
    }
    
    
    interface ReplicationPolicy {
    	
    }
    
    class ImmediateReplicationPolicy implements ReplicationPolicy {
    
    	public ImmediateReplicationPolicy(ByValueSemantics semantics) {
    		
    	}
    	
    	// init, setAttr, remAttr, setMII, destroy
    }
    
    class EndOfRequestGroupPolicy implements ReplicationPolicy {
    	
    	public EndOfRequestGroupPolicy(Semantics semantic) {
    		
    	}
    	
    	// init, setAttr
    }
    
    
}
