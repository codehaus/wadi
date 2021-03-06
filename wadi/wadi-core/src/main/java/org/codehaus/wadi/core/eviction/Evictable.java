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
package org.codehaus.wadi.core.eviction;

/**
 * API for objects that may be inspected to determine whether they should
 * be timed out after certain period of inactivity.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public interface Evictable {
    void init(long creationTime, long lastAccessedTime, int maxInactiveInterval);

    void destroy() throws Exception;
    
    void copy(Evictable evictable) throws Exception;

	void mote(Evictable recipient) throws Exception;
    
	long getCreationTime();
	
	long getLastAccessedTime();

    void setLastAccessedTime(long lastAccessedTime);
	
    int  getMaxInactiveInterval();
	
    void setMaxInactiveInterval(int maxInactiveInterval);
    
    boolean isNeverEvict();

    void setNeverEvict(boolean neverEvict);
	
    long getTimeToLive(long time);
	
    boolean getTimedOut(long time);
}
