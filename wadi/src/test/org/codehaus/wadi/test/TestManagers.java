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

import java.io.IOException;
import javax.servlet.http.HttpSessionContext;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Filter;
import org.codehaus.wadi.Manager;

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

  protected org.codehaus.wadi.jetty.Manager  _jetty =new org.codehaus.wadi.jetty.Manager();
  protected org.codehaus.wadi.tomcat.Manager _tomcat=new org.codehaus.wadi.tomcat.Manager();

  protected Filter _jettyFilter = new Filter();
  protected Filter _tomcatFilter= new Filter();

  public TestManagers(String name)
    {
      super(name);
    }

  protected void
    setUp()
    throws Exception
    {
      javax.servlet.ServletContext servletContext=new ServletContext();
      javax.servlet.FilterConfig filterConfig=new FilterConfig(servletContext);
      _jetty.setServletContext(servletContext);
      _jetty.start();
      _jettyFilter.init(filterConfig);
      _tomcat.setServletContext(servletContext);
      _tomcat.start();
      _tomcatFilter.init(filterConfig);
    }

  protected void
    tearDown()
    throws JMSException
    {
      try
      {
	_tomcat.stop();
	_tomcatFilter.destroy();
	_log.info("here 0");
	_jetty.stop();
	_jettyFilter.destroy();
	_log.info("here 1");
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
	_log.info("[0]");
	String id=_jetty.newHttpSession().getId();
	javax.servlet.http.HttpSession session=_jetty.getHttpSession(id);
	assertTrue(session.getId()==id); // must be ==/same object
	// what do we do about locking issues ?
	//	session.invalidate();
      }
      {
	_log.info("[1]");
	HttpServletRequest req=new HttpServletRequest();
	req.setManager(_jetty);
	req.setSessionId("xxx");
	String id=_jetty.newHttpSession(req).getId();
	javax.servlet.http.HttpSession session=_jetty.getHttpSession(id);
	assertTrue(session.getId()==id); // must be ==/same object
	// what do we do about locking issues ?
	//	session.invalidate();
      }
      _log.info("[2]");
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
	_log.info("[3]");
	int i=45*60;
	_jetty.setHouseKeepingInterval(i);
	assertTrue(_jetty.getHouseKeepingInterval()==i);
      }
      {
	_log.info("[4]");
	String n="JSESSIONID";
	assertTrue(_jetty.getSessionCookieName().equals(n));
	assertTrue(_tomcat.getSessionCookieName().equals(n));
      }
      {
	_log.info("[5]");
	String n="jsessionid";
	assertTrue(_jetty.getSessionUrlParamName().equals(n));
	assertTrue(_tomcat.getSessionUrlParamName().equals(n));
      }
      {
	_log.info("[6]");
	String d=null;
	assertTrue(_jetty.getSessionCookieDomain()==d);
	assertTrue(_tomcat.getSessionCookieDomain()==d);
      }
      {
	_log.info("[7]");
	int n=-1;
	_tomcat.setMaxActive(n);
	assertTrue(_tomcat.getMaxActive()==n);
      }
      {
	_log.info("[8]");
	int n=1000;
	_tomcat.setRejectedSessions(n);
	assertTrue(_tomcat.getRejectedSessions()==n);
      }
      {
	_log.info("[9]");
	int n=1000;
	_tomcat.setExpiredSessions(n);
	assertTrue(_tomcat.getExpiredSessions()==n);
      }
      {
	_log.info("[10]");
	int n=1000;
	_tomcat.setSessionCounter(n);
	assertTrue(_tomcat.getSessionCounter()==n);
      }
      {
	_log.info("[11]");
	HttpServletRequest req=new HttpServletRequest();
	req.setSessionId("xxx");
	req.setManager(_jetty);
	_log.info("cookie path= "+_jetty.getSessionCookiePath(req));
	req.setManager(_tomcat);
	_log.info("cookie path= "+_tomcat.getSessionCookiePath(req));

	// since it is up to the container what these return, there is
	// not much point in checking the value - they are just here
	// for coverage...
      }
      {
	_log.info("[12]");
	HttpSessionContext c0=_jetty.getSessionContext();
	HttpSessionContext c1=_tomcat.getSessionContext();
      }
      {
	_log.info("[13]");
	assertTrue(_jetty.getHttpSession("dummy")==null);
	try
	{
	  assertTrue(_tomcat.findSession("dummy")==null);
	}
	catch (IOException e)
	{
	  assertTrue(false);
	}
      }
      //       {
      // 	int n=1000;
      // 	_tomcat.setActiveSessions(n);
      // 	assertTrue(_tomcat.getActiveSessions()==n);
      //       }
    }

  public void
    runInvocation(Filter filter, Manager manager, Invocation i, HttpServletRequest request, HttpServletResponse response)
  {
    FilterChain chain=new FilterChain(manager, i);
    try
    {
      filter.doFilter(request, response, chain);
    }
    catch (Exception e)
    {
      _log.warn("unexpected problem", e);
      assertTrue(false);
    }
  }

  public void
    testFilter()
  {
    HttpServletRequest req=new HttpServletRequest();
    HttpServletResponse res=new HttpServletResponse();
    Invocation i=new Invocation(){
	public void
	  invoke(Manager manager, javax.servlet.ServletRequest requset, javax.servlet.ServletResponse response)
	{
	  _log.info("made it !");
	}
      };

    runInvocation(_jettyFilter, _jetty, i, req, res);
    runInvocation(_tomcatFilter, _tomcat, i, req, res);
  }
}
