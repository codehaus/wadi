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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

/**
 * Abstract out common functionality for Immoters
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractImmoter implements Immoter {
	protected static final Log _log = LogFactory.getLog(AbstractImmoter.class);

	public boolean prepare(String id, Motable emotable, Motable immotable) {
		try {
			immotable.copy(emotable);
			return true;
		} catch (Exception e) {
			_log.warn("problem during insertion: "+id, e);
			return false;
		}
	}

	public void commit(String id, Motable immotable) {
	}

	public void rollback(String id, Motable immotable) {
		try {
			immotable.tidy();
		} catch (Exception e) {
			_log.error("problem rolling back insertion: "+id, e);
		}
	}

	public String getInfo() {
		return "abstract";
	}
}
