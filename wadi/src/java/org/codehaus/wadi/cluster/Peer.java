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

package org.codehaus.wadi.cluster;

import javax.jms.Destination;
import org.codehaus.activecluster.Node;

public class
  Peer
  implements Comparable
{
  protected String      _id;
  protected Destination _dest;
  protected Node        _node;

  public int
    compareTo(Object o)
  {
    assert o.getClass()==Peer.class;
    return _id.compareTo(((Peer)o)._id);
  }

  public
    Peer(Node node)
  {
    _id=(String)(node.getState().get("id"));
    _dest=null;
    _node=node;
  }


  /**
   * This ctor is for testing only...
   *
   * @param id a <code>String</code> value
   */
  public
    Peer(String id)
    {
      _id=id;
    }

  public void setNode(Node node){_node=node;}

  public String
    toString()
  {
    return "<Peer:"+_id+">";
  }

  public String getId(){return _id;}
}
