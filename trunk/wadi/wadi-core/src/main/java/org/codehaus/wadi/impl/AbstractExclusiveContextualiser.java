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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.EvicterConfig;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Locker;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Basic implementation for Contextualisers which maintain a local Map of references
 * to Motables.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractExclusiveContextualiser extends AbstractMotingContextualiser implements EvicterConfig {
	protected final Map _map;
	protected final Evicter _evicter;
	
	public AbstractExclusiveContextualiser(Contextualiser next, Locker locker, boolean clean, Evicter evicter, Map map) {
		super(next, locker, clean);
		_map=map;
		_evicter=evicter;
	}
	
	protected String _stringPrefix="<"+getClass().getName()+":";
	protected String _stringSuffix=">";
	public String toString() {
		return new StringBuffer(_stringPrefix).append(_map.size()).append(_stringSuffix).toString();
	}
	
	public Motable get(String id) {return (Motable)_map.get(id);}
	
	// TODO - sometime figure out how to make this a wrapper around AbstractChainedContextualiser.handle() instead of a replacement...
	public boolean handle(Invocation invocation, String id, Immoter immoter, Sync motionLock) throws InvocationException {
		Motable emotable=get(id);
		if (emotable==null)
			return false; // we cannot proceed without the session...
		
		if (immoter!=null) {
			return promote(invocation, id, immoter, motionLock, emotable); // motionLock will be released here...
		} else {
			return false;
		}
	}
	
	public Emoter getEvictionEmoter(){return getEmoter();}
	
	protected void unload() {
		Emoter emoter=getEmoter();
		
		// emote all our Motables using it
		RankedRWLock.setPriority(RankedRWLock.EVICTION_PRIORITY);
		
		int i=0;
		while (_map.size()>0) {
			try {
				Motable emotable=(Motable)_map.values().iterator().next();
				
				if (emotable!=null) {
					String name=emotable.getName();
					if (name!=null) {
						Immoter immoter=_next.getSharedDemoter();
						Utils.mote(emoter, immoter, emotable, name);
						i++;
					}
				}
			} catch (Exception e) {
				_log.warn("unexpected problem", e);
			}
		}
		
		RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
		if (_log.isInfoEnabled()) _log.info("unloaded sessions: "+i);
		assert(_map.size()==0);
	}
	
	public void init(ContextualiserConfig config) {
		super.init(config);
		_evicter.init(this);
	}
	
	public void start() throws Exception {
		super.start();
		_evicter.start();
	}
	
	public void stop() throws Exception {
		unload();
		_evicter.stop();
		super.stop();
	}
	
	public void destroy() {
		_evicter.destroy();
		super.destroy();
	}
	
	public void load(Emoter emoter, Immoter immoter) { // - TODO - is this where we should do our own load ?
		// MappedContextualisers are all Exclusive
	}
	
	public Evicter getEvicter(){return _evicter;}
	
	public Immoter getDemoter(String name, Motable motable) {
		long time=System.currentTimeMillis();
		if (getEvicter().test(motable, time, motable.getTimeToLive(time)))
			return _next.getDemoter(name, motable);
		else
			return getImmoter();
	}
	
	public int getLocalSessionCount() {
		return getSize()+_next.getLocalSessionCount();
	}
	
	public int getSize() {
		return _map.size();
	}
	
	// EvicterConfig
	
	// BestEffortEvicters
	
	public Map getMap(){return _map;}
	
	// EvicterConfig
	
	public Timer getTimer() {return _config.getTimer();}
	
	// BestEffortEvicters
	
	public Sync getEvictionLock(String id, Motable motable) {
		return _locker.getLock(id, motable);
	}
	
	public void demote(Motable emotable) {
		String id=emotable.getName();
		if (id==null)
			return; // we lost a race...
		Immoter immoter=_next.getDemoter(id, emotable);
		Emoter emoter=getEvictionEmoter();
		Utils.mote(emoter, immoter, emotable, id);
	}
	
	// StrictEvicters
	public int getMaxInactiveInterval() {return _config.getMaxInactiveInterval();}
	
	// enumerate the keys of all exclusively owned sessions in our stack...
	public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
		super.findRelevantSessionNames(numPartitions, resultSet);
		int matches=0;
		for (Iterator i=_map.keySet().iterator(); i.hasNext(); ) {
			String name=(String)i.next();
			int key=Math.abs(name.hashCode()%numPartitions);
			Collection c=resultSet[key];
			if (c!=null) {
				c.add(name);
				matches++;
			}
		}
		if (matches>0)
			if (_log.isDebugEnabled()) _log.debug("matches found: "+matches);
	}
	
}
