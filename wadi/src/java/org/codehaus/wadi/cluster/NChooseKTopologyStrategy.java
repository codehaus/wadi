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
import org.codehaus.activecluster.ClusterFactory;

public class
  NChooseKTopologyStrategy
  extends Abstract2TopologyStrategy
{
  protected Log _log=LogFactory.getLog(getClass().getName());

  public
    NChooseKTopologyStrategy(String nodeId, String clusterId, Cluster cluster, ClusterFactory factory, int k)
    {
      super(nodeId, clusterId, cluster, factory, k);
    }

  public Map
    combine(Peer local, Collection e, int k)
    {
      Map combs=combine(e, k);

      // only return combinations that contain the local node...
      for (Iterator i=combs.values().iterator(); i.hasNext(); )
	if (!((Collection)i.next()).contains(local))
	  i.remove();

      return combs;
    }

  protected Map
    combine(Collection e, int k)
  {
    Map combsOut=new TreeMap();

    if (k==1)
    {
      for (Iterator i=e.iterator(); i.hasNext(); )
      {
	Peer peer=((Peer)i.next());
	Set comb=new TreeSet();
	comb.add(peer);
	//	_log.info("combining [] and "+peer);
	combsOut.put(Cell.id(comb), comb);
      }
    }
    else
    {
      Map combsIn=combine(e, k-1);

      for (Iterator i=combsIn.values().iterator(); i.hasNext(); )
      {
	Collection comb=((Collection)i.next());
	for (Iterator j=e.iterator(); j.hasNext(); )
	{
	  Peer peer=((Peer)j.next());
	  //	  _log.info("combining "+comb+" and "+peer);
	  if (!comb.contains(peer))
	  {
	    Set newComb=new TreeSet(comb);
	    newComb.add(peer);
	    String id=Cell.id(newComb);
	    if (!combsOut.containsKey(id))
	      combsOut.put(id, newComb);
	  }
	}
      }
    }

    return combsOut;
  }
}
