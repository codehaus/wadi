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
package org.codehaus.wadi.activecluster;

import org.codehaus.wadi.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.evacuation.AbstractTestEvacuation;


public class TestACInVMEvacuation extends AbstractTestEvacuation {
	
	public TestACInVMEvacuation(String name) {
        super(name);
    }

    public void testEvacuation() throws Exception {
        String clusterName = ACTestUtil.CLUSTER_NAME;
        String clusterUri = ACTestUtil.CLUSTER_URI_VM;
        long timeout = ACTestUtil.CLUSTER_INACTIVE_TIME;
        testEvacuation(new ActiveClusterDispatcher(clusterName, "red", clusterUri, null, timeout),
                new ActiveClusterDispatcher(clusterName, "green", clusterUri, null, timeout));
    }
	
}
