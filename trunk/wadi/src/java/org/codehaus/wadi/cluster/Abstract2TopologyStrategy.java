
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

  protected Collection _oldPeers=new TreeSet();
  protected Map        _oldCells=new TreeMap();

  public void
    add(Peer p)
  {
    Peer localPeer=getLocalPeer();
    localPeer=localPeer!=null?localPeer:p; // TODO - hack - FIXME
    Map newCells=combine(_peers.values(), _k);
    _log.info("topology="+newCells.keySet());
    Map relCells=relevant(_oldCells, newCells, localPeer);

    int n=relCells.size();

    if (n>0)
    {
      _log.info("gaining: "+n+" relevant cell[s] - "+relCells.keySet());

      for (Iterator i=relCells.entrySet().iterator(); i.hasNext(); )
      {
	Map.Entry entry=(Map.Entry)i.next();
	String cellId=(String)entry.getKey();
	Collection peers=(Collection)entry.getValue();
	Cell cell=new Cell(cellId, _clusterId, peers, _factory);
	_cells.put(cellId, cell);
	cell.start();
      }
    }
    else
      _log.debug("no relevant change to cell structure");

    _oldCells=newCells;
  }

  public void
    remove(Peer p)
  {
    Peer localPeer=getLocalPeer();
    localPeer=localPeer!=null?localPeer:p; // TODO - hack - FIXME
    Map newCells=combine(_peers.values(), _k);
    _log.info("topology="+newCells.keySet());
    Map relCells=relevant(newCells, _oldCells, localPeer);

    int n=relCells.size();

    if (n>0)
    {
      _log.info("losing: "+n+" relevant cell[s] - "+relCells.keySet());
      for (Iterator i=relCells.entrySet().iterator(); i.hasNext(); )
      {
	Map.Entry entry=(Map.Entry)i.next();
	String cellId=(String)entry.getKey();
	Collection peers=(Collection)entry.getValue();
	Cell cell=(Cell)_cells.get(cellId);
	cell.stop();
      }
    }
    else
      _log.debug("no relevant change to cell structure");

    _oldCells=newCells;
  }

  /**
   * A Cell is relevant if it is joining/leaving and contains the
   * LocalPeer...
   *
   * @param oldCells a <code>Map</code> value
   * @param newCells a <code>Map</code> value
   * @param localPeer a <code>Peer</code> value
   */
  public Map
    relevant(Map oldCells, Map newCells, Peer localPeer)
  {
    // 1st, figure out the difference between the old and new
    // topologies for whole cluster...

    _log.debug("oldCells: "+oldCells.keySet());
    _log.debug("newCells: "+newCells.keySet());

    Map diffCells=new TreeMap(newCells);
    diffCells.keySet().removeAll(oldCells.keySet());

    _log.debug("diffCells: "+diffCells.keySet());

    // 2nd, figure out which of these cells this peer is
    // involved in - these are the ones that are relevant...

    for (Iterator i=diffCells.values().iterator(); i.hasNext();)
      if (!((Set)i.next()).contains(localPeer))
	i.remove();

    _log.debug("relCells: "+diffCells.keySet());

    return diffCells;
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
  public abstract Map combine(Collection peers, int peersPerCell);
}
