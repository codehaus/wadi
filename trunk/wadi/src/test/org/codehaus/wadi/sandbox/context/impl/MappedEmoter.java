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
package org.codehaus.wadi.sandbox.context.impl;

import java.util.Map;

import org.codehaus.wadi.sandbox.context.Motable;


/**
 * A basic Emoter for MappedContextualisers
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MappedEmoter extends ChainedEmoter {

	protected final Map _map;

	public MappedEmoter(Map map) {
		_map=map;
	}

	public boolean prepare(String id, Motable emotable, Motable immotable) {
		if (super.prepare(id, emotable, immotable)) {
			synchronized (_map){_map.remove(id);} // remove ref in cache
			return true;
		} else
			return false;
	}

	public void rollback(String id, Motable emotable) {
		synchronized (_map){_map.put(id, emotable);} // replace ref into cache
	}

	public String getInfo() {
		return "mapped";
	}
}
