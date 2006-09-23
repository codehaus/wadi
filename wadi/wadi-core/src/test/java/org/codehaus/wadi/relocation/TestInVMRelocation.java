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
package org.codehaus.wadi.relocation;

import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;

/**
 * 
 * @version $Revision: 1603 $
 */
public class TestInVMRelocation extends AbstractTestRelocation {

	public TestInVMRelocation(String arg0) {
		super(arg0);
	}
	
	public void testSessionRelocation() throws Exception {
        VMBroker cluster = new VMBroker("TEST");
		long timeout=5000;
		testSessionRelocation(new VMDispatcher(cluster, "red", timeout), new VMDispatcher(cluster, "green", timeout));
	}
}
