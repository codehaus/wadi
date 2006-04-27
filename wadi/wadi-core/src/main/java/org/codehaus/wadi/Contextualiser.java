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
 * Contextualising a request is realising/processing it within the correct Context, in other words, in the presence of the required HttpSession, if any.
 *
 * A Contextualiser can choose to either process the request within itself, or promote a Context to its caller, within which the request may be processed.
 * It should indicate to its caller, via return code, whether said processing has already been carried out or not.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Contextualiser extends Lifecycle {
	
	// I'd like to add Manager to param list, but it bloats dependency tree - can we get along without it ?
	public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException;
	void setLastAccessedTime(Evictable evictable, long oldTime, long newTime);
	void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval);

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
