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


public class TestACReplication extends AbstractTestReplication {

  public TestACReplication(String arg0) {
    super(arg0);
  }

  public void testReplication() throws Exception {
    String clusterName="org.codehaus.wadi.TEST-"+Math.random();
    String nodeName="test."+Math.random();
    long timeout=5000;

    testReplication(new ActiveClusterDispatcher(clusterName, nodeName, "vm://localhost", timeout));
    testReplication(new ActiveClusterDispatcher(clusterName, nodeName, "peer://org.codehaus.wadi", timeout)); // TODO - stop() seems to run asynchronously - resolve
  }

}

