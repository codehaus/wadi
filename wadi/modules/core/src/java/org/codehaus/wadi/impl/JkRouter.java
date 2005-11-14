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
package org.codehaus.wadi.impl;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.RouterConfig;

public class JkRouter implements Router {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final String _info;
    protected final String _suffix;

    public JkRouter(String info) {
        _info=info;
        _suffix="."+_info;
    }

    protected RouterConfig _config;
    
    public void init(RouterConfig config) {
        _config=config;
    }
    
    public void destroy() {
        _config=null;
    }
    
    public String strip(String session) {
        int i=session.lastIndexOf(".");
        if (i<0)
            return session;
        else
            return session.substring(0, i);
    }

    public String augment(String id) {
        return augment(id, _suffix);
    }

    public String augment(String id, String target) {
        assert id!=null;
        assert target.startsWith(".");

        int i=id.lastIndexOf(".");
        if (i<0) // id has no routing info
            return id+target;
        else // routing info already present
            if (id.endsWith(target))
                return id; // it's our routing info - leave it
            else
                return id.substring(0, i)+target; // it's someone else's - replace it

    }

    public String getInfo() {
        return _info;
    }

    public boolean canReroute() {
        return true;
    }

  public boolean reroute(HttpServletRequest req, HttpServletResponse res) {
    String id=req.getRequestedSessionId();

    if (id.endsWith(_suffix))
      return false;

    if (req.isRequestedSessionIdFromCookie())
      return rerouteCookie(req, res, id);
    else
      return false;
  }

    public boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, String id) {
        return rerouteCookie(req, res, id, _suffix);
    }

    public boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, String id, String target) {
        assert target.startsWith(".");

        String oldId=id;
        String newId=augment(id);

        if ( _log.isInfoEnabled() ) {

            _log.info("rerouting cookie: " + oldId + " -> " + newId);
        }

        Cookie[] cookies=req.getCookies();

        // TODO - what about case sensitivity on value ?
        for (int i=0;i<cookies.length;i++)
        {
            Cookie cookie=cookies[i];
            if (cookie.getName().equalsIgnoreCase(_config.getSessionCookieName()) && cookie.getValue().equals(oldId))
            {
                // name, path and domain must match those on client side,
                // for cookie to be updated in browser...

                String cookiePath=_config.getSessionCookiePath(req);
                if (cookiePath!=null)
                    cookie.setPath(cookiePath);

                String cookieDomain=_config.getSessionCookieDomain();
                if (cookieDomain!=null)
                    cookie.setDomain(cookieDomain);

                cookie.setValue(newId); // the session id with redirected routing info

                res.addCookie(cookie);
            }
        }

        return false;
    }

    public boolean rerouteURL() {
        return rerouteURL(_suffix);
    }

    public boolean rerouteURL(String target) {
        assert target.startsWith(".");
        // TODO Auto-generated method stub
        return false;
    }

}
