
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterFactory;

public abstract class
  Abstract2TopologyStrategy
  extends AbstractTopologyStrategy
{
  protected Map            _cells=new TreeMap();
  protected String         _clusterId;
  protected ClusterFactory _factory;
  protected int            _k=1;

  public
    Abstract2TopologyStrategy(String nodeId, String clusterId, Cluster cluster, ClusterFactory factory, int k)
  {
    super(nodeId, cluster);
    _clusterId=clusterId;
    _factory=factory;
    _k=k;
  }

  protected Map _oldPeers=new TreeMap();
  protected Map _oldCells=new TreeMap();

  public Object[]
    combine(Peer p)
  {
    Peer localPeer=getLocalPeer();
    localPeer=localPeer!=null?localPeer:p; // TODO - hack - FIXME
    Map newCells=combine(localPeer, _peers.values(), _k);

    _log.info("old peers="+_oldPeers.keySet());
    _log.info("old rel cells="+_oldCells.keySet());
    _log.info("new peers="+_peers.keySet());
    _log.info("new rel cells="+newCells.keySet());

    Map joiningCells=new TreeMap(newCells);
    joiningCells.keySet().removeAll(_oldCells.keySet());
    _log.info("joining cells="+joiningCells.keySet());

    Map leavingCells=new TreeMap(_oldCells);
    leavingCells.keySet().removeAll(newCells.keySet());
    _log.info("leaving cells="+leavingCells.keySet());

    _oldPeers=_peers;
    _oldCells=newCells;

    return new Object[]{joiningCells, leavingCells};
  }

  /**
   * returns a Map of String:Collection - id:set-of-peers representing
   * all cells into which the passed list of Peers should be
   * organised. k is the number of Peers in each Cell.
   *
   * @param e a <code>Collection</code> value
   * @param k an <code>int</code> value
   * @return a <code>Map</code> value
   */
  public abstract Map combine(Peer localPeer, Collection peers, int peersPerCell);
}
