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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Abstract the encoding and decoding of routing info within the
 * session id, so that different load balancer integrations may be
 * pluggable.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface
  RoutingStrategy
{
  /**
   * Strip any routing info from this session id.
   *
   * @param session a <code>String</code> value
   * @return a <code>String</code> value
   */
  String strip(String session);

  /**
   * Add our routing info to this session id.
   *
   * @param session a <code>String</code> value
   * @return a <code>String</code> value
   */
  String augment(String session);

  /**
   * Return the routing info for this node
   *
   * @param id a <code>String</code> value
   * @return a <code>String</code> value
   */
  String getInfo();

  /**
   * Is this RoutingStrategy sufficiently integrated with its
   * corresponding load-balancer as to be able to 'stick' subsequent
   * requests for the same session to this node?
   *
   * @return a <code>boolean</code> value
   */
  public boolean canReroute();

  /**
   * Reroute to ourselves.
   *
   * @param req a <code>HttpServletRequest</code> value
   * @param res a <code>HttpServletResponse</code> value
   * @param manager a <code>Manager</code> value
   * @param id a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String id);

  /**
   * Alter the value of the session cookie to reflect the route that
   * we now require the load balancer to use.
   *
   * @param req a <code>HttpServletRequest</code> value
   * @param res a <code>HttpServletResponse</code> value
   * @param manager a <code>Manager</code> value
   * @param id a <code>String</code> value
   * @param route a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, Manager manager, String id, String route);

  /**
   * Reroute to ourselves.
   *
   * @return a <code>boolean</code> value
   */
  boolean rerouteURL();

  /**
   * Reroute to target by setting the routing info in the url and
   * redirecting to the load-balancer.
   *
   * @param target a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  boolean rerouteURL(String target);
}
