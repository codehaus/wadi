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
package org.codehaus.wadi.tribes;

import org.codehaus.wadi.evacuation.AbstractTestEvacuation;
import org.codehaus.wadi.tribes.TribesDispatcher;

public class TestTribesEvacuation extends AbstractTestEvacuation {

  public TestTribesEvacuation(String name) {
    super(name);
  }

  public void testEvacuation() throws Exception {
      String clusterName="dummy"+System.currentTimeMillis();
	  long timeout=5000;
      testEvacuation(new TribesDispatcher(clusterName, "red", timeout, ""), new TribesDispatcher(clusterName, "green", timeout, ""));
  }

}
