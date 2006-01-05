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
/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.wcache;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Cache {
	public RequestProcessor put(String key, RequestProcessor rp);
	public RequestProcessor get(String id);
	public RequestProcessor peek(String id);
	public RequestProcessor remove(String id);
	public void evict();
	public boolean isOffNode();

	interface Evicter {
		boolean evict(String key, RequestProcessor val);
	}
}
