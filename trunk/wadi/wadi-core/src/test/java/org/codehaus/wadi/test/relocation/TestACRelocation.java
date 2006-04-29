/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.test.relocation;

import org.codehaus.wadi.activecluster.ActiveClusterDispatcher;

/**
 * 
 * @version $Revision: 1603 $
 */
public class TestACRelocation extends AbstractTestRelocation {

	public TestACRelocation(String arg0) {
		super(arg0);
	}
	
	public void testSessionRelocation() throws Exception {
		String clusterName="TEST";
		long timeout=5000;
		String clusterUri;
		clusterUri="vm://localhost";
		testSessionRelocation(new ActiveClusterDispatcher(clusterName, "red", clusterUri, timeout), new ActiveClusterDispatcher(clusterName, "green", clusterUri, timeout));
		clusterUri="peer://org.codehaus.wadi";
		//testSessionRelocation(new ActiveClusterDispatcher(clusterName, "red", clusterUri, timeout), new ActiveClusterDispatcher(clusterName, "green", clusterUri, timeout));
	}
	
}
