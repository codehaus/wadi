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
import org.codehaus.wadi.shared.Manager;
import org.codehaus.wadi.shared.RoutingStrategy;

/**
 * An integration strategy for maintaining session affinity through
 * cooperation with a load balancer that does not use routing info
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  NoRoutingStrategy
  implements RoutingStrategy
{
  public String strip(String id) {return id;}
  public String augment(String id) {return id;}
  public String getInfo() {return "";}
  public boolean canReroute() {return false;}
  public boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String id) {return false;}
  public boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String id, String route) {return false;}
  public boolean rerouteURL(){return false;}
  public boolean rerouteURL(String route){return false;}
}
