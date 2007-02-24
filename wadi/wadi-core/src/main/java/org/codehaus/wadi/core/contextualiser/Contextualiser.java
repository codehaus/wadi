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
package org.codehaus.wadi.core.contextualiser;

import java.util.Map;

import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;

/**
 * Contextualising an Invocation is colocating it and the relevant Session within the same JVM and then
 * invoke()-ing the Invocation...
 * 
 * Each Contextualiser contains a store of Sessions. These may be held e.g. in Memory, on Disc,
 * in a Database, or on another Peer in the Cluster. These are arranged in a linked list.
 * 
 * This 'Contextualiser stack' is generally arranged with the most expensive, fastest, most volatile storage
 * (i.e. Memory) at the top, and cheapest, slowest, securest storage (e.g. Database), at the bottom.
 * 
 * As Sessions age, they are be evicted downwards to free up valuable and scarce resources.
 * 
 * Incoming Invocations are passed down the Contextualiser stack until they meet the relevant
 * Session. At this point, the Session is promoted up to Memory and the Invocation is invoke()-ed in
 * its presence.
 * 
 * If the Invocation reaches the ClusterContextualiser without meeting its Session and its
 * Session is found to be located elsewhere in the Cluster, then the ClusterContextualiser has the option
 * of relocating the Session to the Invocation in the local JVM OR the Invocation to the Session
 * in the remote JVM. The location of the invocation's contextualisation is unimportant, provided that
 * it occurs somewhere.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Contextualiser extends Lifecycle {
    /**
     * Contextualise the given Invocation. 
     * 
     * @param invocation the Invocation
     * @param key the Session key
     * @param immoter an Immoter that can be used to promote the Session when found
     * @param exclusiveOnly whether we should only look in exclusive stores, or descend to shared ones as well
     * @return whether or not the Invocation was contextualised.
     * @throws InvocationException
     */
    public boolean contextualise(Invocation invocation, String key, Immoter immoter, boolean exclusiveOnly) throws InvocationException;

	/**
	 * Return an immoter to the first Contextualiser below us, which would be happy to accept this Motable -
     * in other words - would not evict() it.

	 * @param name - uid of the Motable
	 * @param motable - the Motable in question
	 * @return - a Demoter - a delegate capable of arranging immotion into the correct Contextualiser
	 */
	Immoter getDemoter(String name, Motable motable);
    
	/**
     * Return an Immoter to the first SharedContextualiser below us.
     * 
	 * @return the Immoter
	 */
	Immoter getSharedDemoter();
	
	/**
     * Pass this Immoter up to the first ExclusiveContextualiser above us, where...
     * 
	 * @param immoter the Immoter
	 */
	void promoteToExclusive(Immoter immoter);
    
	void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames);
}
