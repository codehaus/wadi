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
package org.codehaus.wadi.core.motable;

import org.codehaus.wadi.RehydrationException;
import org.codehaus.wadi.core.eviction.Evictable;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;



/**
 * Lit. 'able to be moved' - an Object the can be [promoted and] demoted
 * up and down a Contextualiser stack. An Evictable with an ID and a payload.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Motable extends Evictable {
    void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name);

    void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body) throws RehydrationException;

    void restore(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body) throws RehydrationException;

    void copy(Motable motable) throws Exception;
	
	void mote(Motable recipient) throws Exception;

	String getName();
    
    boolean isNew();

    byte[] getBodyAsByteArray() throws Exception;

    void setBodyAsByteArray(byte[] bytes) throws Exception;

    ReadWriteLock getReadWriteLock();
}
