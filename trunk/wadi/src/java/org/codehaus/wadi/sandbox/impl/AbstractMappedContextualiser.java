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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;


/**
 * Basic implementation for Contextualisers which maintain a local Map of references
 * to Motables.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractMappedContextualiser extends AbstractChainedContextualiser {
	protected final Map _map;

	public AbstractMappedContextualiser(Contextualiser next, Map map, Evicter evicter) {
		super(next, evicter);
		_map=map;
	}

	protected String _stringPrefix="<"+getClass().getName()+":";
	protected String _stringSuffix=">";
	public String toString() {
		return new StringBuffer(_stringPrefix).append(_map.size()).append(_stringSuffix).toString();
	}

	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException {
		Motable emotable=(Motable)_map.get(id);
		if (emotable==null)
			return false; // we cannot proceed without the session...

		if (immoter!=null)
			return promote(hreq, hres, chain, id, immoter, promotionLock, emotable); // promotionLock will be released here...

		return contextualiseLocally(hreq, hres, chain, id, promotionLock, emotable);
	}

	public abstract boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Sync promotionLock, Motable motable) throws IOException, ServletException;

	protected final Sync _dummyLock=new NullSync();
	public Sync getEvictionLock(String id, Motable motable){return _dummyLock;} // TODO - should this be the promotionLock ?
	public Emoter getEvictionEmoter(){return getEmoter();}
	
	public void evict() {
	    Collection copy=null;
	    synchronized (_map) {copy=new ArrayList(_map.entrySet());}
	    
	    long time=System.currentTimeMillis();
	    for (Iterator i=copy.iterator(); i.hasNext(); ) {
	        Map.Entry e=(Map.Entry)i.next();
	        String id=(String)e.getKey();
	        Motable emotable=(Motable)e.getValue();
	        if (_evicter.evict(id, emotable, time)) { // first test without lock - cheap
	            Sync lock=getEvictionLock(id, emotable);
	            boolean acquired=false;
	            do {
	                try {
	                    lock.attempt(0);
	                    acquired=true;
	                } catch (InterruptedException ie) {
	                    _log.warn("unexpected interruption - continuing", ie);
	                }
	            } while (Thread.interrupted());
	            
	            try {
	                if (acquired && _evicter.evict(id, emotable, time)) { // second confirmatory test with lock
	                    Immoter immoter=_next.getDemoter(id, emotable);
	                    Emoter emoter=getEvictionEmoter();
	                    Utils.mote(emoter, immoter, emotable, id);
	                }
	            } finally {
	                if (acquired)
	                    lock.release();
	            }
	        }
	    }
	}
}
