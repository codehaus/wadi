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

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.activecluster.Cluster;
import org.activecluster.ClusterFactory;

public abstract class
  Abstract2TopologyStrategy
  extends AbstractTopologyStrategy
{
  protected Map            _cells=new TreeMap(); // would a HashMap be faster?
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

  protected Collection _oldPeers=new TreeSet(new CollectionComparator());
  protected Map _oldCells=new TreeMap();
  protected Collection _oldCells2=new TreeSet(new CollectionComparator());

  public Object[]
    combineMap(Peer p)
  {
    Peer localPeer=getLocalPeer();
    localPeer=localPeer!=null?localPeer:p; // TODO - hack
    Map newCells=combineMap(localPeer, _peers.values(), Math.min(_k,_peers.size()));

    _log.info("old peers="+_oldPeers);
    _log.info("old rel cells="+_oldCells.keySet());
    _log.info("new peers="+_peers.keySet());
    _log.info("new rel cells="+newCells.keySet());

    Map joiningCells=new TreeMap(newCells);
    joiningCells.keySet().removeAll(_oldCells.keySet());
    _log.info("joining cells="+joiningCells.keySet());

    Map leavingCells=new TreeMap(_oldCells);
    leavingCells.keySet().removeAll(newCells.keySet());
    _log.info("leaving cells="+leavingCells.keySet());

    _oldPeers=new TreeSet(_peers.values());
    _oldCells=newCells;

    return new Object[]{joiningCells, leavingCells};
  }

  public Object[]
    combineCollection(Peer p)
  {
    Peer localPeer=getLocalPeer();
    localPeer=localPeer!=null?localPeer:p; // TODO - hack
    Collection newCells=combineCollection(localPeer, _peers.values(), Math.min(_k,_peers.size()));

    _log.info("old peers="+_oldPeers);
    _log.info("old rel cells="+_oldCells2);
    _log.info("new peers="+_peers.keySet());
    _log.info("new rel cells="+newCells);

    Collection joiningCells=new TreeSet(new CollectionComparator());
    joiningCells.addAll(newCells);
    joiningCells.removeAll(_oldCells2);
    _log.info("joining cells="+joiningCells);

    Collection leavingCells=new TreeSet(new CollectionComparator());
    leavingCells.addAll(_oldCells2);
    leavingCells.removeAll(newCells);
    _log.info("leaving cells="+leavingCells);

    _oldPeers=new TreeSet(_peers.values());
    _oldCells2=newCells;

    return new Object[]{joiningCells, leavingCells};
  }

  /**
   * returns a Map of String:Collection (id:set-of-peers) representing
   * all cells into which the passed list of Peers should be
   * organised. K is the number of Peers in each Cell.
   *
   * @param localPeer a <code>Peer</code> value
   * @param peers a <code>Collection</code> value
   * @param peersPerCell an <code>int</code> value
   * @return a <code>Map</code> value
   */
  public abstract Map combineMap(Peer localPeer, Collection peers, int peersPerCell);
  public abstract Collection combineCollection(Peer localPeer, Collection peers, int peersPerCell);

  public Cell getCell(String id) {return (Cell)_cells.get(id);}
  public void putCell(String id, Cell cell) {_cells.put(id, cell);}
}
