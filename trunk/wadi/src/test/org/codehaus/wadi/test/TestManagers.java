/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

import javax.jms.JMSException;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestManagers
  extends TestCase
{
  protected Log _log=LogFactory.getLog(TestManagers.class);

  protected org.codehaus.wadi.jetty.Manager _jetty=new org.codehaus.wadi.jetty.Manager();
  protected org.codehaus.wadi.tomcat.Manager _tomcat=new org.codehaus.wadi.tomcat.Manager();

  public TestManagers(String name)
    {
      super(name);
    }

  protected void
    setUp()
    throws Exception
    {
      _jetty.start();
      _tomcat.start();
    }

  protected void
    tearDown()
    throws JMSException
    {
      try
      {
	_tomcat.stop();
	_jetty.stop();
      }
      catch (Exception e)
      {
	_log.warn("something broke", e);
	assertTrue(false);
      }
    }

  public void
    testManagers()
    {
      assertTrue(true);
    }
}
