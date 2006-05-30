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
import org.codehaus.wadi.replication.AbstractTestReplication;


public class TestACInVMReplication extends AbstractTestReplication {

  public TestACInVMReplication(String arg0) {
    super(arg0);
  }

  public void testReplication() throws Exception {
    String clusterName=ActiveClusterCluster.TEST_CLUSTER_NAME;
    String clusterUri=ActiveClusterCluster.TEST_VM_CLUSTER_URI;
    long timeout=ActiveClusterCluster.TEST_CLUSTER_INACTIVE_TIME;
    String peerName="red";

    testReplication(new ActiveClusterDispatcher(clusterName, peerName, clusterUri, timeout));
  }

}

