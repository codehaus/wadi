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

import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A Contextualiser that does no contextualising
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyContextualiser extends AbstractContextualiser {
	
	public DummyContextualiser() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualise(InvocationContext invocationContext, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
		return false;
	}
	
	protected final Evicter _evicter=new DummyEvicter();
	public Evicter getEvicter(){return _evicter;}
	
	public boolean isExclusive(){return false;}
	
	public class DummyImmoter implements Immoter {
		public Motable nextMotable(String id, Motable emotable){return null;}
		public String getInfo(){return "dummy";}
		
		public boolean prepare(String name, Motable emotable, Motable immotable) {return true;}
		
		public void commit(String name, Motable immotable) {
			// throw away incoming state...
		}
		
		public void rollback(String name, Motable immotable) {
			// nothing to do...
		}
		
		public boolean contextualise(InvocationContext invocationContext, String id, Motable immotable, Sync motionLock) throws InvocationException {
			return false;
		}
	}
	
	protected final Immoter _immoter=new DummyImmoter();
	public Immoter getDemoter(String name, Motable motable) {return _immoter;}
	public Immoter getSharedDemoter(){return _immoter;}
	
	public void promoteToExclusive(Immoter immoter){/* empty */}
	public void load(Emoter emoter, Immoter immoter) {/* empty */}
	
	public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {/* do nothing */}
	public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {/* do nothing */}
	
	
	public int getLocalSessionCount() {
		return 0;
	}
	
}