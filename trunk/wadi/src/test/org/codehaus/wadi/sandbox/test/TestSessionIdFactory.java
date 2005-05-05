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
package org.codehaus.wadi.sandbox.test;

import java.util.Set;
import java.util.TreeSet;

import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.impl.TomcatIdGenerator;

import junit.framework.TestCase;

public class TestSessionIdFactory extends TestCase {

    public TestSessionIdFactory(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

  public void testIdFactory() throws Exception {
      IdGenerator factory=new TomcatIdGenerator();
      Set ids=new TreeSet();
      int numIds=100000;
      for (int i=0; i<numIds; i++)
          ids.add(factory.take());
      assertTrue(ids.size()==numIds);
  }
}
