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

import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;

/**
 * An Evicter which never evicts
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class NeverEvicter implements Evicter {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Evicter#evict(java.lang.String, org.codehaus.wadi.sandbox.context.Motable)
	 */
	public boolean evict(String id, Motable motable) {
		// TODO Auto-generated method stub
		return false;
	}

}
