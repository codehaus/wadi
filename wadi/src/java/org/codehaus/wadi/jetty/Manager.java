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

package org.codehaus.wadi.jetty;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionContext;
import org.codehaus.wadi.Filter;
//import org.mortbay.jetty.servlet.Dispatcher;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;
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
  extends org.codehaus.wadi.Manager
  implements SessionManager, Serializable
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

  public javax.servlet.http.HttpSession
    newHttpSession(HttpServletRequest request)
  {
    return acquireImpl(this, _useRequestedId?getRoutingStrategy().strip(request.getRequestedSessionId()):null).getFacade();
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

  public synchronized void
    initialize(ServletHandler handler)
  {
    setServletHandler(handler);
    String filterName="WadiFilter";
    _handler.defineFilter(filterName, Filter.class.getName());
    _handler.addFilterPathMapping("/*", filterName,
				  //				  Dispatcher.__REQUEST | Dispatcher.__FORWARD | Dispatcher.__INCLUDE | Dispatcher.__ERROR
				  FilterHolder.__REQUEST | FilterHolder.__FORWARD | FilterHolder.__INCLUDE | FilterHolder.__ERROR

				  ); // TODO - improve mapping, all 'stateful' servlets/filters

    _context=(WebApplicationContext)_handler.getHttpContext();
    boolean distributable=_context.isDistributable();
    if (distributable && !_distributable)
      setDistributable(distributable);
  }

  public void
    setServletHandler(ServletHandler servletHandler)
    {
      _handler=(WebApplicationHandler)servletHandler;
      _servletContext=_handler.getServletContext();
    }

  public synchronized void
    start()
      throws Exception
  {
    _implFactory=new HttpSessionImplFactory();

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
    _housekeeper.interrupt();// - try without this
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

  //  protected org.codehaus.wadi.HttpSession createFacade(org.codehaus.wadi.HttpSessionImpl impl){return new org.codehaus.wadi.jetty.HttpSession((org.codehaus.wadi.jetty.HttpSessionImpl)impl);}

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
    String path=_servletContext.getInitParameter(__SessionPath);

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
  public String getSessionCookieDomain(){return _servletContext.getInitParameter(__SessionDomain);}

  /**
   * return the key used for the session url path parameter
   *
   * @return a <code>String</code> value
   */
  public String getSessionUrlParamName(){return __SessionURL;};

  public int getHttpPort(){return Integer.parseInt(System.getProperty("http.port"));} // TODO - temporary hack...

  public HttpSessionContext getSessionContext() {return org.mortbay.jetty.servlet.SessionContext.NULL_IMPL;}

  // cut-n-pasted from Jetty src - aarg !
  // Greg uses Apache-2.0 as well - so no licensing issue as yet - TODO

  public javax.servlet.http.Cookie
    getSessionCookie(javax.servlet.http.HttpSession session,boolean requestIsSecure)
  {
    if (_handler.isUsingCookies())
    {
      javax.servlet.http.Cookie cookie=getHttpOnly()
	?new org.mortbay.http.HttpOnlyCookie(SessionManager.__SessionCookie,session.getId())
	:new javax.servlet.http.Cookie(SessionManager.__SessionCookie,session.getId());
      String domain=_handler.getServletContext().getInitParameter(SessionManager.__SessionDomain);
      String maxAge=_handler.getServletContext().getInitParameter(SessionManager.__MaxAge);
      String path=_handler.getServletContext().getInitParameter(SessionManager.__SessionPath);
      if (path==null)
	path=getUseRequestedId()?"/":_handler.getHttpContext().getContextPath();
      if (path==null || path.length()==0)
	path="/";

      if (domain!=null)
	cookie.setDomain(domain);
      if (maxAge!=null)
	cookie.setMaxAge(Integer.parseInt(maxAge));
      else
	cookie.setMaxAge(-1);

      cookie.setSecure(requestIsSecure && getSecureCookies());
      cookie.setPath(path);

      return cookie;
    }
    return null;
  }

  protected boolean _httpOnly=true;
  public boolean getHttpOnly() {return _httpOnly;}
  public void setHttpOnly(boolean httpOnly) {_httpOnly=httpOnly;}

  protected boolean _secureCookies=false;
  public boolean getSecureCookies() {return _secureCookies;}
  public void setSecureCookies(boolean secureCookies) {_secureCookies=secureCookies;}

  protected boolean _useRequestedId=false;
  public boolean getUseRequestedId() {return _useRequestedId;}
  public void setUseRequestedId(boolean useRequestedId) {_useRequestedId=useRequestedId;}
}
