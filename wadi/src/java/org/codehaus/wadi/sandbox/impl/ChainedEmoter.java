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

import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Motable;


/**
 * A basic Emoter for ChainedContextualisers
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ChainedEmoter implements Emoter {

	public boolean prepare(String id, Motable emotable, Motable immotable) {
		return true;
	}

	public void commit(String id, Motable emotable) {
		emotable.tidy(); // remove copy in store
	}

	public void rollback(String id, Motable emotable) {
	}

	public String getInfo() {
		return "chained";
	}
}
