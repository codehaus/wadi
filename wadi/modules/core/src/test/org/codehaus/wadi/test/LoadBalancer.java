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

package org.codehaus.wadi.test;

import java.net.InetSocketAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// How WADI sees the load-balancer tier... - a ContextFactory

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
interface
  LoadBalancer
{
  Context createContext(InetSocketAddress hostAndPort, String path);
  void destroyContext(Context context);
}

interface
  Context			// a webapp
{
  InetSocketAddress getHostAndPort();
  String getPath();

  int getWeight();		// do we do weighting on a per host, hostandport, jvm or context basis ?
  void setWeight(int weight);

  boolean getAcceptingRequests();
  void setAcceptingRequests(boolean b);

  boolean getAcceptingSessions();
  void setAcceptingSessions();

  boolean getOwnsSession(String sessionId);
  void setOwnsSession(String sessionId);

  void start();
  void stop();

  void redirectRequestTo(HttpServletRequest req); // can you redirect a post ?
  void proxyRequestTo(HttpServletRequest req, HttpServletResponse res);
  Object migratSessionFrom(String sessionId);
}

// How LoadBalancer tier sees WADI cluster... - a ServiceLocator

interface
  Cluster
{
  Context locate(String sessionId);
}

// Other thoughts:

// Could LB run either as Apache plugin, or as standalone LB ?
// Could it listen on ActiveMQ in spite of language barrier
// Needs a nice stats page
// Needs to work as a tier, rather than just a single, fragile instance

// what about virtual hosts ?
