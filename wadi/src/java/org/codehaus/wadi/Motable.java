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

/**
 * Lit. 'able to be moved' - an Object the can be [promoted and] demoted.
 * An Evictable with an ID and a payload.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Motable extends Evictable {
    
    void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name);
    void destroy();
	void copy(Motable motable) throws Exception;

	String getName();

     // TODO - should bytes[] include simply payload, or entire Motable - decide..
	byte[] getBytes() throws Exception;
	void setBytes(byte[] bytes) throws Exception;
}
