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
    combineMap(Peer local, Collection e, int k)
    {
      Map combs=null;
      boolean filter=(local!=null);

      if (filter)
      {
	// calculate all subcombinations...
	e=new TreeSet(e);
	e.remove(local);
	k--;
	combs=combineMap(e, k);

	// combine them with local node...
	Map tmp=new TreeMap();
	for (Iterator i=combs.entrySet().iterator(); i.hasNext(); )
	{
	  Map.Entry entry=(Map.Entry)i.next();
	  String key=(String)entry.getKey();
	  Collection value=(Collection)entry.getValue();
	  value.add(local);
	  key=Cell.id(value);
	  tmp.put(key, value);
	}
	combs=tmp;
      }
      else
      {
	combs=combineMap(e, k);
      }

      return combs;
    }

  protected Map
    combineMap(Collection e, int k)
  {
    Map combsOut=new TreeMap();

    if (k==0)
      combsOut.put("", new TreeSet());
    else
    {
      Map combsIn=combineMap(e, k-1);

      for (Iterator i=combsIn.values().iterator(); i.hasNext(); )
      {
	Collection comb=((Collection)i.next());
	for (Iterator j=e.iterator(); j.hasNext(); )
	{
	  Object peer=j.next();
	  if (!comb.contains(peer))
	  {
	    Set newComb=new TreeSet(comb);
	    newComb.add(peer);
	    String id=Cell.id(newComb);
	    combsOut.put(id, newComb);
	  }
	}
      }
    }

    return combsOut;
  }

  public Collection
    combineCollection(Peer local, Collection e, int k)
    {
      Collection combs=null;
      boolean filter=(local!=null);

      if (filter)
      {
	_log.info("[1] START : "+e);
	// calculate all subcombinations...
	e=new TreeSet(e);
	e.remove(local);
	k--;
	combs=combineCollection(e.toArray(), -1, k);

	// combine them with local node...
	for (Iterator i=combs.iterator(); i.hasNext(); )
	  ((Collection)i.next()).add(local);
      }
      else
      {
	_log.info("[2] START : "+e);
	_log.warn("",new Exception());
	combs=combineCollection(e.toArray(), -1, k);
      }

      return combs;
    }

  protected Collection
    combineCollection(Object[] e, int offset, int k)
  {
    Collection combsOut=new TreeSet(new CollectionComparator());

    if (k==0)
      combsOut.add(new TreeSet());
    else
    {
      Collection combsIn=combineCollection(e, ++offset, k-1);

      for (Iterator i=combsIn.iterator(); i.hasNext(); )
      {
	Collection comb=((Collection)i.next());
	for (int j=offset; j<e.length; j++)
	{
	  Peer peer=(Peer)e[j];
	  Set newComb=new TreeSet(comb);
	  newComb.add(peer);
	  combsOut.add(newComb);
	  _log.info("newComb="+newComb+", "+offset);
	}
      }
    }

    return combsOut;
  }
}
