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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
    combine(Peer local, Collection e, int k)
    {
      int l=e.size();

      Map combs=new TreeMap();

      if (k>0)
      {
	Object[] array=e.toArray();
	for (int i=0; i<l; i++)
	{
	  Set comb=new TreeSet();

	  for (int j=0; j<k; j++)
	    comb.add(array[(i+j)%l]);

	  if (comb.contains(local)) // TODO - could be more efficient...
	    combs.put(Cell.id(comb), comb);
	}
      }

      return combs;
    }
}
