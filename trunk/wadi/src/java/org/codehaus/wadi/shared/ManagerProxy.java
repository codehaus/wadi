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

package org.codehaus.wadi.shared;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.plugins.NoRoutingStrategy;

// TODO - should these be cached ?

// TODO - should this be returned as a serialised object from the
// remote server or constructed from a String returned as it's
// response ? String is more secure - we are not running code that has
// come over the wire...

/**
 * Client-side proxy for a remote Node. Interactions with this remote
 * Node may be achieved through calling methods on the local proxy.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  ManagerProxy
{
  protected final Log _log=LogFactory.getLog(getClass());

//   public boolean
//     relocateSession(String id, HttpSessionImpl impl, StreamingStrategy streamingStrategy)
//   {
//     return migrateSessionFrom(id, impl, streamingStrategy);
//   }

  //----------------------------------------
  // migration - use if we are relocating state rather than request
  protected final InetAddress             _migrationAddress;
  protected final int                     _migrationPort;
  //  protected final NewMigrationService.Client _migrationClient;

//   protected boolean
//     migrateSessionFrom(String id, HttpSessionImpl impl, StreamingStrategy streamingStrategy)
//   {
//     return _migrationClient.run(id, impl, _migrationAddress, _migrationPort, streamingStrategy);
//   }

  //----------------------------------------

  public boolean
    relocateRequest(HttpServletRequest req, HttpServletResponse res, Manager manager)
  {
    if (manager.getRoutingStrategy() instanceof NoRoutingStrategy)
      // TODO - not quite right - if request is direct, we could
      // redirect directly to other host - unusual situation, however
      return proxyRequestTo(req, res); // we have no way to control where request falls
    else
      return redirectRequestTo(req, res, manager); // we should be able to redirect to the correct host
  }

  //----------------------------------------
  // proxying - use if session id encoded in cookie
  protected final InetAddress _httpAddress;
  protected final int         _httpPort;

  protected boolean
    proxyRequestTo(HttpServletRequest req, HttpServletResponse res)
  {
    return HttpProxy.proxy(_httpAddress.getHostAddress(), _httpPort, req, res);
  }

  //----------------------------------------
  // redirection - use if session id encoded in url
  protected final InetAddress _lbAddress; // TODO - we should be able to get this from request ?
  protected final int         _lbPort; // TODO - we should be able to get this from request ?
  protected final String      _route;

  protected boolean
    redirectRequestTo(HttpServletRequest req, HttpServletResponse res, Manager manager)
  {
    // TODO - NYI

    // if original request came directly to us, redirect directly to
    // other node, otherwise it came via load-balancer. Rewrite
    // session routing info on it and redirect back to lb...

    // get RoutingStrategy and BucketName from Manager

    // currently assume it came indirectly...

    // TODO - this assumes that the bucketname on the request matches
    // the bucketname on this node - this is unlikely.. - move the
    // bucketName replacement code into the RoutingStrategy and fix...

    String oldId=req.getRequestedSessionId();
    RoutingStrategy router=manager.getRoutingStrategy();
    String root=router.strip(oldId);
    String newId=router.augment(root);

    String oldUrl=req.getRequestURL().toString();
    String newUrl=oldUrl;

    if (isDirect(req))
    {
      _log.trace("is direct");
      // this request came to us directly, not via an e.g. proxy/lb

      // arrange for request to be relocated correctly by rewriting
      // its url to point directly to where it should go and then
      // redirecting it...

      try
      {
	// TODO - clumsy but...
	URL url=new URL(oldUrl);
	String oldPrefix=url.getProtocol()+"://"+url.getHost()+(url.getPort()==url.getDefaultPort()?"":(":"+url.getPort()));
	String newPrefix=url.getProtocol()+"://"+_httpAddress.getHostAddress()+(_httpPort==url.getDefaultPort()?"":(":"+_httpPort));

      if (_log.isTraceEnabled()) _log.trace("old prefix:"+oldPrefix);
      if (_log.isTraceEnabled()) _log.trace("new prefix:"+newPrefix);
      if (_log.isTraceEnabled()) _log.trace("old url:"+oldUrl);
	newUrl=newUrl.replaceFirst(oldPrefix, newPrefix);
      if (_log.isTraceEnabled()) _log.trace("new url:"+newUrl);
      }
      catch (MalformedURLException e)
      {
	_log.warn("unexpected problem synthesising url", e);
      }
    }
    else
    {
      _log.trace("is indirect");
      // this request came to us indirectly, via an e.g. proxy/lb

      // arrange for request to be relocated correctly by rewriting
      // the routing info on its session id and then redirecting it
      // back to the address:port to which it was originally sent (the
      // lb)), which should then route it the correct node...

      if (req.isRequestedSessionIdFromCookie())
	rerouteSessionCookie(req, res, manager, oldId, newId);
      else
      {
	if (_log.isTraceEnabled()) _log.trace("old url="+oldUrl);
	newUrl=oldUrl.replaceAll("jsessionid="+oldId, "jsessionid="+newId);
	if (_log.isTraceEnabled()) _log.trace("new url="+newUrl);
      }
    }

    try
    {
      if (_log.isTraceEnabled()) _log.trace("redirecting: "+oldUrl+" -> "+newUrl);
      res.sendRedirect(newUrl);
      return true;
    }
    catch (IOException e)
    {
      _log.warn("failed to redirect request", e);
      return false;
    }
  }

  public static void		// used by JkRoutingStrategy...
    rerouteSessionCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String oldId, String newId)
  {
    String cookieName=manager.getSessionCookieName();
    Cookie[] cookies=req.getCookies();

    // TODO - what about case sensitivity on value ?
    for (int i=0;i<cookies.length;i++)
    {
      Cookie cookie=cookies[i];
      if (cookie.getName().equalsIgnoreCase(cookieName) &&
	  cookie.getValue().equals(oldId))
      {
	// name, path and domain must match those on client side,
	// for cookie to be updated in browser...

	// TODO - move this into Jetty stuff...
	String cookiePath=manager.getSessionCookiePath(req);
	if (cookiePath!=null)
	{
	  //	  if (_log.isTraceEnabled()) _log.trace("cookie path="+cookiePath);
	  cookie.setPath(cookiePath);
	}

	String cookieDomain=manager.getSessionCookieDomain();
	if (cookieDomain!=null)
	{
	  //	  if (_log.isTraceEnabled()) _log.trace("cookie domain="+cookieDomain);
	  cookie.setDomain(cookieDomain);
	}

	cookie.setValue(newId); // the session id with redirected routing info

	res.addCookie(cookie);
      }
    }
  }

  //----------------------------------------

  protected final String _sessionId;

  public
    ManagerProxy(String id, String response)
    throws Exception
  {
    _sessionId=id;

    String[] args=response.split(",");

    // args[0]=="org.codehaus.wadi"
    // args[1]==<protocol>
    // args[2]==id
    _httpAddress      = InetAddress.getByName(args[3]);
    _httpPort         = Integer.parseInt(args[4]);
    _migrationAddress = InetAddress.getByName(args[5]);
    _migrationPort    = Integer.parseInt(args[6]);
    //    _migrationClient  = new NewMigrationService.Client(); // TODO - should be static ?
    _lbAddress        = null;	// TODO - NYI
    _lbPort           = 0;	// TODO - NYI
    _route            = (args.length>7)?args[7]:null; // TODO - NYI

    // should also contain e.g. acceptingSessions?,
    // acceptingRequests?, howManyMoreSessions ? etc...
  }

  //----------------------------------------

  public boolean isDirect(HttpServletRequest req){return true;}	// TODO - how do we work this out ?
}
