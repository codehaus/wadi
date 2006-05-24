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
package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.evacuation.AbstractTestEvacuation;
import org.codehaus.wadi.jgroups.JGroupsDispatcher;

public class TestJGEvacuation extends AbstractTestEvacuation {

  public TestJGEvacuation(String arg0) {
    super(arg0);
  }

  public void testEvacuation() throws Exception {
	  String clusterName="TEST";
	  long timeout=5000;
	  testEvacuation(new JGroupsDispatcher("red", clusterName, timeout, "default.xml"), new JGroupsDispatcher("green", clusterName, timeout, "default.xml"));
  }
  
}
