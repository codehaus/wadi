
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
  public
    NChooseKTopologyStrategy(String nodeId, String clusterId, Cluster cluster, ClusterFactory factory, int k)
    {
      super(nodeId, clusterId, cluster, factory, k);
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
	Map subCombs=combine(e, k-1);
	for (Iterator i=subCombs.values().iterator(); i.hasNext(); )
	{
	  Set subComb=(Set)i.next();
	  for (Iterator j=e.iterator(); j.hasNext(); )
	  {
	    Peer p=(Peer)j.next();
	    if (!subComb.contains(p))
	    {
	      Set comb=new TreeSet(subComb);
	      comb.add(p);
	      String id=Cell.id(comb);
	      combs.put(id, comb);
	    }
	  }
	}
      }

      return combs;
    }
}
