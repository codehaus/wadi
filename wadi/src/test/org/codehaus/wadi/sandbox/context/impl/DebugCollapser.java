/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DebugCollapser implements Collapser {
	protected final Log _log = LogFactory.getLog(getClass());

	class DebugSync implements Sync {
		protected int _counter;
		public void acquire(){_log.info("acquire: "+ (_counter++));}
		public void release(){_log.info("release: "+ (--_counter));}
		public boolean attempt(long timeout){_log.info("attempt: "+ (++_counter));return true;}
	}

	protected Sync _sync=new DebugSync();

	/**
	 *
	 */
	public DebugCollapser() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Collapser#getLock(java.lang.String)
	 */
	public Sync getLock(String id) {
		return _sync;
	}
}
