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
import java.util.ArrayList;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterFactory;

public class
  RingTopologyStrategy
  extends Abstract2TopologyStrategy
{
  public
    RingTopologyStrategy(String nodeId, String clusterId, Cluster cluster, ClusterFactory factory, int k)
    {
      super(nodeId, clusterId, cluster, factory, k);
    }

  public Map
    combineMap(Peer local, Collection e, int k)
    {
      int l=e.size();

      Map combs=new TreeMap();

      if (k>0)
      {
	Object[] array=e.toArray();
	for (int i=0; i<l; i++)
	{
	  // use an ArrayList because:
	  // the algorithm does not produce duplicates
	  // we want cell peers to use ordering produced by algorithm - 3-0, not 0-3
	  Collection comb=new ArrayList(k);

	  for (int j=0; j<k; j++)
	    comb.add(array[(i+j)%l]);

	  boolean filter=(local!=null);

	  if (!filter || comb.contains(local)) // TODO - could be more efficient...
	    combs.put(Cell.id(comb), comb);
	}
      }

      return combs;
    }

  public Collection
    combineCollection(Peer local, Collection e, int k)
    {
      int l=e.size();

      Collection combs=new ArrayList();

      if (k>0)
      {
	Object[] array=e.toArray();
	for (int i=0; i<l; i++)
	{
	  // use an ArrayList because:
	  // the algorithm does not produce duplicates
	  // we want cell peers to use ordering produced by algorithm - 3-0, not 0-3
	  Collection comb=new ArrayList(k);

	  for (int j=0; j<k; j++)
	    comb.add(array[(i+j)%l]);

	  boolean filter=(local!=null);

	  if (!filter || comb.contains(local)) // TODO - could be more efficient...
	    combs.add(comb);
	}
      }

      return combs;
    }
}
