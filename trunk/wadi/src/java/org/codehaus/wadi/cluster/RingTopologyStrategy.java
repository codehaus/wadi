
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
  RingTopologyStrategy
  extends Abstract2TopologyStrategy
{
  public
    RingTopologyStrategy(String nodeId, String clusterId, Cluster cluster, ClusterFactory factory, int k)
    {
      super(nodeId, clusterId, cluster, factory, k);
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
  public Map
    combine(Collection e, int k)
    {
      Map combs=new TreeMap();

      if (k>0)
      {
	Object[] array=e.toArray();
	for (int i=0; i<e.size(); i++)
	{
	  Set comb=new TreeSet();

	  for (int j=0; j<k; j++)
	    comb.add(array[(i+j)%k]);

	  String id=comb.toString();
	  combs.put(id, comb);
	}
      }

      return combs;
    }
}
