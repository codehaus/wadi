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
package org.codehaus.wadi.evacuation;

import org.codehaus.wadi.group.vm.SysOutMessageRecorder;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;


public class TestInVMEvacuation extends AbstractTestEvacuation {
	
	public TestInVMEvacuation(String arg0) {
		super(arg0);
	}
	
	public void testEvacuation() throws Exception {
        VMBroker cluster = new VMBroker("TEST");
        cluster.setMessageRecorder(new SysOutMessageRecorder());
		long timeout=5000;
		testEvacuation(new VMDispatcher(cluster, "red", timeout), new VMDispatcher(cluster, "green", timeout));
	}
	
}

