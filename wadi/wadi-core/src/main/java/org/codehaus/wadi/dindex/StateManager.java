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
package org.codehaus.wadi.dindex;


import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.group.Message;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface StateManager {

	void init(StateManagerConfig config);
	void start() throws Exception;
	void stop() throws Exception;

	interface ImmigrationListener { void onImmigration(Message message, Motable immigrant); }

	boolean offerEmigrant(String key, Motable emotable, long timeout);
	void acceptImmigrant(Message message, Location location, String name, Motable immotable);

	void setImmigrationListener(ImmigrationListener listener);
	void unsetImmigrationListener(ImmigrationListener listener);

}