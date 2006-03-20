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
package org.codehaus.wadi.test;

import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import junit.framework.TestCase;

public class TestEvacuation extends TestCase {

  protected Log _log = LogFactory.getLog(getClass());
  protected String _url="jdbc:axiondb:WADI";
  protected DataSource _ds=new AxionDataSource(_url);

  public TestEvacuation(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testEvacuation() throws Exception {
    assertTrue(true);

    MyStack stack1=new MyStack("red", _url, _ds);
    _log.info("RED STARTING...");
    stack1.start();
    _log.info("...RED STARTED");
    MyStack stack2=new MyStack("green", _url, _ds);
    _log.info("GREEN STARTING...");
    stack2.start();
    _log.info("...GREEN STARTED");

    String id=stack1.getManager().create().getId();

    _log.info("RED STOPPING...");
    stack1.stop();
    _log.info("...RED STOPPED");

//    stack2.getManager().destroy(id);

    _log.info("GREEN STOPPING...");
    stack2.stop();
    _log.info("...GREEN STOPPED");
  }
}
