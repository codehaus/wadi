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

package org.codehaus.wadi.jetty;

import java.io.Serializable;
import java.net.InetAddress;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionContext;
import org.codehaus.wadi.shared.Filter;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;

//TODO - remember max number of sessions in map

/**
 * A WADI session manager for Jetty (jetty.mortbay.org).
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  Manager
  extends org.codehaus.wadi.shared.Manager
  implements org.mortbay.jetty.servlet.SessionManager, Serializable
{
  //----------------//
  // SessionManager //
  //----------------//

  public javax.servlet.http.HttpSession
    getHttpSession(String id)
  {
    HttpSessionImpl impl=(HttpSessionImpl)get(getRoutingStrategy().strip(id));
    org.codehaus.wadi.jetty.HttpSession session=impl==null?null:(org.codehaus.wadi.jetty.HttpSession)impl.getFacade();

    javax.servlet.http.HttpSession answer=(session==null?null:(session.isValid()?session:null)); // TODO - or should session just be removed from map as soon as it is invalidated..
    return answer;
  }

  public javax.servlet.http.HttpSession
    newHttpSession()
  {
    return acquireImpl(this).getFacade();
  }

  protected boolean _reuseIds=false; // TODO - make this explicit

  public javax.servlet.http.HttpSession
    newHttpSession(HttpServletRequest request)
  {
    return acquireImpl(this, _reuseIds?getRoutingStrategy().strip(request.getRequestedSessionId()):null).getFacade();
  }

  //-----------//
  // lifecycle //
  //-----------//

  protected Thread _housekeeper;

  class
    HouseKeeper
    implements Runnable
  {
    public void
      run()
    {
      Thread.currentThread().setContextClassLoader(_loader);
      _log.debug("beginning housekeeping thread");
      while (_running)
      {
 	try
 	{
	  Thread.sleep(_housekeepingInterval*1000);
 	  housekeeper();
 	}
 	catch (InterruptedException e)
 	{
	  // someone wants us to exit quickly...
	  Thread.interrupted();
	  _log.trace("housekeeper thread interrupted");
 	}
 	catch (Throwable t)
	{
	  _log.warn("housekeeping problem", t);
	}
      }
      _log.debug("ending housekeeping thread");
    }
  }

  protected int _housekeepingInterval=30; // seconds
  public void setHouseKeepingInterval(int seconds){_housekeepingInterval=seconds;}
  public int getHouseKeepingInterval(){return _housekeepingInterval;}

  protected WebApplicationHandler _handler;
  protected WebApplicationContext _context;

  boolean _initialized=false;	// temporary hack - remove when Jetty is fixed...

  public synchronized void
    initialize(ServletHandler handler)
  {
    if (!_initialized)
    {
      _handler=(WebApplicationHandler)handler;
      String filterName="WadiFilter";
      _handler.defineFilter(filterName, Filter.class.getName());
      _handler.mapPathToFilter("/*", filterName); // TODO - improve mapping, all 'stateful' servlets/filters

      _context=(WebApplicationContext)_handler.getHttpContext();
      boolean distributable=_context.isDistributable();
      if (distributable && !_distributable)
	setDistributable(distributable);

      _initialized=true;
    }
    else
      _log.warn("multiple initialisation");
  }

  public synchronized void
    start()
      throws Exception
  {
    super.start();
    (_housekeeper=new Thread(new HouseKeeper())).start();
    //    _filter=(Filter)(_handler.getFilter("WadiFilter").getFilter());
  }

  public synchronized boolean
    isStarted()
  {
    return _running;
  }

  public synchronized void
    stop()
      throws InterruptedException
  {
    _running=false;		// super.stop() does this - but we need it here :-(
    _housekeeper.interrupt();
    _housekeeper.join();
    _housekeeper=null;

    try
    {
      super.stop();
    }
    catch (Exception e)
    {
      _log.warn("unexpected", e); // TODO - this should disappear
    }

  }

  //--------------//
  // Serializable //
  //--------------//

  // why does the session manager need to be serialisable ?

  //  protected org.codehaus.wadi.shared.HttpSession createFacade(org.codehaus.wadi.shared.HttpSessionImpl impl){return new org.codehaus.wadi.jetty.HttpSession((org.codehaus.wadi.jetty.HttpSessionImpl)impl);}

  //----------------------------------------

  /**
   * return the name used for the session cookie
   *
   * @return a <code>String</code> value
   */
  public String getSessionCookieName()  {return __SessionCookie;}


  /**
   * return the path that is given to new session cookies.
   *
   * @return a <code>String</code> value
   */
  public String
    getSessionCookiePath(HttpServletRequest req)
  {
    String path=_handler.getServletContext().getInitParameter(__SessionPath);

    if (path==null)
      path=req.getContextPath();

    if (path==null || path.length()==0)
      path="/";

    return path;
  }

  /**
   * return the domain that is given to new session cookies.
   *
   * @return a <code>String</code> value
   */
  public String getSessionCookieDomain(){return _handler.getServletContext().getInitParameter(__SessionDomain);}

  /**
   * return the key used for the session url path parameter
   *
   * @return a <code>String</code> value
   */
  public String getSessionUrlParamName(){return __SessionURL;};


  public boolean isServing(InetAddress address, int port){return true;}	// TODO

  public int getHttpPort(){return Integer.parseInt(System.getProperty("http.port"));} // TODO - temporary hack...

  public ServletContext getServletContext(){return _handler.getServletContext();}
  public HttpSessionContext getSessionContext() {return org.mortbay.jetty.servlet.SessionContext.NULL_IMPL;}

  protected org.codehaus.wadi.shared.HttpSessionImpl createImpl(){return new HttpSessionImpl();}
  protected void destroyImpl(org.codehaus.wadi.shared.HttpSessionImpl impl){} // TODO - cache later
}