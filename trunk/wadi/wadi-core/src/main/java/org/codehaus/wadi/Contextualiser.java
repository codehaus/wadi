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
package org.codehaus.wadi;

import java.util.Collection;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Contextualising an Invocation is bringing it and the relevant Session into the same JVM and then
 * invoke()-ing the Invocation...
 * 
 * Each Contextualiser contains a store of Sessions. These may be held e.g. in Memory, on Disc,
 * in a Database, or on another Peer in the Cluster. These are arranged in a linked list.
 * 
 * This 'Contextualiser stack' is generally arranged with fastest, most volatile storage (i.e. Memory) at
 * the top, and slowest, securest storage (e.g. Database), at the bottom.
 * 
 * As Sessions age, they may be evicted downwards from faster, more expensive storage (e.g. Memory) 
 * to cheaper, longer term storage (e.g. Disc) to free up resources.
 * 
 * Incoming Invocations are passed down a linked list of Contextualisers until they meet the relevant
 * Session. At this point, the Session is promoted up to Memory and the Invocation is invoke()-ed in
 * its presence.
 * 
 * If the Invocation reaches the ClusterContextualiser without its Session being found and its
 * Session is found to be located elsewhere in the Cluster, then the ClusterContextualiser has the option
 * of relocating the Session to the Invocation in the local JVM OR the Invocation to the Session
 * in the remote JVM. The location of the invocations contextualisation is unimportant, provided that
 * it occurs somewhere.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Contextualiser extends Lifecycle {
	
	// I'd like to add Manager to param list, but it bloats dependency tree - can we get along without it ?
	public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException;
    
	void setLastAccessedTime(Evictable evictable, long oldTime, long newTime);
    
	void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval);

	/**
     * Does this Contextualiser exclusively own, or share, the store in which it keeps its Sessions ?
     * 
	 * @return whether or not our store is exclusively owned
	 */
	boolean isExclusive();
	
	/**
	 * Return a Demoter to the first Contextualiser which would be happy to accept this Motable - in other words - would not evict() it.
	 * @param name - uid of the Motable
	 * @param motable - the Motable in question
	 * @return - a Demoter - a delegate capable of arranging immotion into the correct Contextualiser
	 */
	Immoter getDemoter(String name, Motable motable);
    
	Immoter getSharedDemoter();
	
	// perhaps these two could be collapsed...
	void promoteToExclusive(Immoter immoter); // TODO - 'orrible name...
    
	void load(Emoter emoter, Immoter immoter);
	
	void init(ContextualiserConfig config);
    
	void destroy();
	
	void findRelevantSessionNames(int numPartitions, Collection[] resultSet);
	
	int getLocalSessionCount();
	
}
