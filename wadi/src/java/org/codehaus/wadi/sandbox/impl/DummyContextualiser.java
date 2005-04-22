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
package org.codehaus.wadi.sandbox.impl;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;


/**
 * A Contextualiser that does no contextualising
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyContextualiser implements Contextualiser {

	/**
	 *
	 */
	public DummyContextualiser() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, boolean localOnly) {
		return false;
	}

	public void evict(){
	    // we carry no state...
	    }
	
	protected final Evicter _evicter=new NeverEvicter();
	public Evicter getEvicter(){return _evicter;}

	public boolean isLocal(){return false;}

	public class DummyImmoter implements Immoter {
		public Motable nextMotable(String id, Motable emotable){return null;}
		public String getInfo(){return "dummy";}

		public boolean prepare(String id, Motable emotable, Motable immotable) {return true;}
		
		public void commit(String id, Motable immotable) {
		    // throw away incoming state...
		    }
		
		public void rollback(String id, Motable immotable) {
		    // nothing to do...
		    }
		
		public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable, Sync promotionLock) {
		        return false;
		    }
	}
	
	protected final Immoter _immoter=new DummyImmoter();
	public Immoter getDemoter(String id, Motable motable) {return _immoter;}
    public Immoter getSharedDemoter(){throw new UnsupportedOperationException();}
    
    public void start(){/* empty */}
    public void stop(){/* empty */}
    
}
