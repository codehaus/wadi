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
import org.codehaus.activecluster.LocalNode;
import org.codehaus.activecluster.Node;


// not sure how many layers to this API yet:

// 1 converts nodes to Peers

// 1 calls combine and then works out relevant joining/leaving cells -
// and later creates/destroys them...

// it should be possible to override the above with a more
// efficient/specific algorithm which calculates which cells should be
// created/destroyed directly....

// and more...

public abstract class
  AbstractTopologyStrategy
  implements TopologyStrategy
{
  protected Log       _log=LogFactory.getLog(getClass().getName());
  protected Map       _peers=new TreeMap();
  protected String    _id;
  protected Cluster   _cluster;
  protected Peer      _localPeer;
  protected LocalNode _localNode;

  public
    AbstractTopologyStrategy(String id, Cluster cluster)
  {
    _id=id;
    _log=LogFactory.getLog(getClass().getName()+"#"+_id);
    _cluster=cluster;
  }

  public Peer getLocalPeer(){return _localPeer;}

  public void
    start()
  {
    _localNode=_cluster.getLocalNode();
    onNodeAdd(new ClusterEvent(_cluster, _localNode, ClusterEvent.ADD_NODE));
    _localPeer=(Peer)_peers.get(_id);
  }

  public void
    stop()
  {
    onNodeRemoved(new ClusterEvent(_cluster, _localNode, ClusterEvent.REMOVE_NODE));
    _localNode=null;
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

    Object[] diffs=combineCollection(p);
  }

  public void onCoordinatorChanged(ClusterEvent ce){} // TODO - what does thi mean ?
  public void onNodeFailed(ClusterEvent event){onNodeRemoved(event);}
  public void
    onNodeRemoved(ClusterEvent event)
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

    Object[] diffs=combineCollection(p);
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

  public abstract Object[] combineMap(Peer p);
  public abstract Object[] combineCollection(Peer p);
}
