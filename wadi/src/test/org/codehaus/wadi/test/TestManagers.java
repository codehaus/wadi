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
      javax.servlet.ServletContext servletContext=new ServletContext();
      _jetty.setServletContext(servletContext);
      _jetty.start();
      _tomcat.setServletContext(servletContext);
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
      {
	String id=_jetty.newHttpSession().getId();
	javax.servlet.http.HttpSession session=_jetty.getHttpSession(id);
	assertTrue(session.getId()==id); // must be ==/same object

	// what do we do about locking issues ?
	//	session.invalidate();
      }
      {
	org.apache.catalina.Session session=_tomcat.createSession();
	_tomcat.add(session);
	String id=session.getId();
	session=null;
	try
	{
	  session=_tomcat.findSession(id);
	}
	catch (java.io.IOException e)
	{
	  // this will cause a failure in the following assertation - no body
	}
	assertTrue(session.getId()==id); // must be ==/same object

	// what do we do about locking issues ?
	//	session.invalidate();
      }
      {
	int i=45*60;
	_jetty.setHouseKeepingInterval(i);
	assertTrue(_jetty.getHouseKeepingInterval()==i);
      }
      {
	String n="JSESSIONID";
	assertTrue(_jetty.getSessionCookieName().equals(n));
	assertTrue(_tomcat.getSessionCookieName().equals(n));
      }
      {
	String n="jsessionid";
	assertTrue(_jetty.getSessionUrlParamName().equals(n));
	assertTrue(_tomcat.getSessionUrlParamName().equals(n));
      }
      {
	String d=null;
	assertTrue(_jetty.getSessionCookieDomain()==d);
	assertTrue(_tomcat.getSessionCookieDomain()==d);
      }
      {
	int n=-1;
	_tomcat.setMaxActive(n);
	assertTrue(_tomcat.getMaxActive()==n);
      }
      {
	int n=1000;
	_tomcat.setRejectedSessions(n);
	assertTrue(_tomcat.getRejectedSessions()==n);
      }
      {
	int n=1000;
	_tomcat.setExpiredSessions(n);
	assertTrue(_tomcat.getExpiredSessions()==n);
      }
      {
	int n=1000;
	_tomcat.setSessionCounter(n);
	assertTrue(_tomcat.getSessionCounter()==n);
      }
//       {
// 	int n=1000;
// 	_tomcat.setActiveSessions(n);
// 	assertTrue(_tomcat.getActiveSessions()==n);
//       }
    }
}
