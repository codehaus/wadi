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

package org.codehaus.wadi.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterEvent;
import org.codehaus.activecluster.Node;

public abstract class
  AbstractTopologyStrategy
  implements TopologyStrategy
{
  //  protected Log _log=LogFactory.getLog(getClass().getName()+"#"+_id);
  protected Log _log=LogFactory.getLog(getClass().getName());
  protected Map _peers=new TreeMap();

  protected Cluster _cluster;
  protected Peer    _localPeer;

  public
    AbstractTopologyStrategy(Cluster cluster)
  {
    _cluster=cluster;
  }

  public Peer getLocalPeer(){return _localPeer;}

  public void
    start()
  {
    Node localNode=_cluster.getLocalNode();
    onNodeAdd(new ClusterEvent(_cluster, localNode, ClusterEvent.ADD_NODE));
    _localPeer=(Peer)_peers.get(localNode.getState().get("id"));
  }

  public void
    stop()
  {
    onNodeRemove(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.REMOVE_NODE));
    _localPeer=null;
  }

  public void
    onNodeAdd(ClusterEvent event)
  {
    Peer p=new Peer(event.getNode());
    Collection peers=null;

    synchronized (_peers)
    {
      _peers.put(p.getId(), p);
      peers=_peers.values();
    }

    _log.info("adding: " + p);
    //	  _log.info("nodes : " + peers);

    add(p);
  }

  // do we need this yet ?
  public void
    onNodeUpdate(ClusterEvent event)
  {
    Node node=event.getNode();
    String id=(String)(node.getState().get("id"));
    Peer p=null;
    Collection peers=null;
    synchronized (_peers)
    {
      p=(Peer)_peers.get(id);
      peers=_peers.values();
    }
    p.setNode(node);	// important - this is the update...

    _log.info("updating: " + p);
    _log.info("nodes   : " + peers);
  }

  public void
    onNodeRemove(ClusterEvent event)
  {
    Node node=event.getNode();
    String id=(String)node.getState().get("id");
    Peer p=null;
    Collection peers=null;
    synchronized (_peers)
    {
      p=(Peer)_peers.remove(id);
      peers=_peers.values();
    }

    _log.info("removing: " + p);
    //	  _log.info("nodes   : " + peers);

    remove(p);
  }

  public abstract void add(Peer p);
  public abstract void remove(Peer p);
}

