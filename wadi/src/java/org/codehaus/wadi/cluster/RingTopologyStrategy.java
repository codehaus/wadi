
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;

public class
  RingTopologyStrategy
  extends AbstractTopologyStrategy
{
  protected int _k=1;
  protected Map _cells=new TreeMap();

  public
    RingTopologyStrategy(String id, Cluster cluster, int k)
  {
    super(id, cluster);
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
    Map relCells=relevant(_oldCells, newCells, localPeer);

    int n=relCells.size();

    if (n>0)
      _log.info("gaining: "+n+" cell[s] - "+relCells.keySet());

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

    Map diffCells=new TreeMap(newCells);
    diffCells.keySet().removeAll(oldCells.keySet());

    // 2nd, figure out which of these cells this peer is
    // involved in - these are the ones that are relevant...

    for (Iterator i=diffCells.values().iterator(); i.hasNext();)
      if (!((Set)i.next()).contains(localPeer))
	i.remove();

    return diffCells;
  }

  public void
    remove(Peer p)
  {
    Peer localPeer=getLocalPeer();
    localPeer=localPeer!=null?localPeer:p; // TODO - hack - FIXME
    Map newCells=combine(_peers.values(), _k);
    Map relCells=relevant(newCells, _oldCells, localPeer);

    int n=relCells.size();

    if (n>0)
      _log.info("losing: "+n+" cell[s] - "+relCells.keySet());

    _oldCells=newCells;
  }

  public Map
    combine(Collection e, int k)
  {
    Map combs=new TreeMap();

    if (k<1)
    {
      return combs;
    }
    else if (k==1)
    {
      for (Iterator i=e.iterator(); i.hasNext(); )
      {
	Peer p=(Peer)i.next();
	String id=p.getId();
	Set comb=new TreeSet();
	comb.add(p);
	combs.put(id, comb);
      }
    }
    else
    {
      // NYI

      // Arrange Cells in an overlapping ring...

      // This is a clumsy topology because every join will result in
      // at least one cell destruction. Cell destruction is BAD
      // because the state that was held in the cell will have to be
      // rehomed - this will involve IO.
    }

    return combs;
  }
}
