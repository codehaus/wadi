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

package org.codehaus.wadi.plugins;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.Manager;
import org.codehaus.wadi.shared.ManagerProxy;
import org.codehaus.wadi.shared.RoutingStrategy;


// TODO - this class needs to be better integrated with the
// IdGenerator API so that, in cases where the id is of fixed length,
// we can take advantage of the fact.

/**
 * An integration strategy for maintaining session affinity through
 * cooperation with Apache/mod_jk.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  JkRoutingStrategy
  implements RoutingStrategy
{
  protected Log _log = LogFactory.getLog(getClass());

  protected String _name;
  public JkRoutingStrategy(String name){_name=name;}

  public String
    strip(String session)
  {
    int index=session.lastIndexOf('.');
    return index>-1?session.substring(0, index):session;
  }

  public String
    augment(String session)
  {
    return _name==null?session:session+"."+_name; // TODO - can we be more efficient ?
  }

  public String
    getInfo()
  {
    return "."+_name;
  }

  public boolean
    canReroute()
  {
    return true;
  }

  public boolean
    rerouteCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String id)
  {
    return rerouteCookie(req, res, manager, id, _name);
  };

  public boolean
    rerouteCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String id, String route)
  {
    int i=id.lastIndexOf(".")+1;

    if (i<1)
      return false;		// id has no routing info to switch - perhaps we should add some - TODO ?
    else
    {
      int idlen=id.length();
      int sufLen=idlen-i;
      int routeLen=route.length();

      if (sufLen==routeLen && id.regionMatches(i, route, 0, sufLen))
	return false;		// no need to switch - already has correct info
      else
      {
	// OK - we need to fix routing info on this session id, so
	// that subsequent requests are routed back to this node -
	// this avoids thrashing the session round the cluster...
	_log.info("switching routing info on "+id+" to "+route);

	String newId=augment(strip(id));
	ManagerProxy.rerouteSessionCookie(req, res, manager, id, newId);
	return true;
      }
    }
  }

  public boolean rerouteURL(){return rerouteURL(_name);}
  public boolean rerouteURL(String route){_log.warn("rerouting via url NYI:-("); return false;}
}
