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

import org.codehaus.wadi.shared.RoutingStrategy;

// TODO - this class needs to be better integrated with the
// IdGenerator API so that, in cases where the id is of fixed length,
// we can take advantage of the fact.

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
  public String strip(String session) {return session;}
  public String strip(String bucket, String session) {return session;}
  public String augment(String bucket, String session) {return session;}

  // not used ?
  public String get(String id){return _nodeId;}
  protected String _nodeId;
  public String getNodeId(){return _nodeId;}
  public void setNodeId(String nodeId){_nodeId=nodeId;}
}
