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

package org.codehaus.wadi.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterEvent;
import org.codehaus.activecluster.ClusterException;
import org.codehaus.activecluster.ClusterListener;
import org.codehaus.activecluster.Node;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activecluster.impl.StateServiceImpl;
import org.codehaus.activemq.ActiveMQConnectionFactory;

// originally based on James' ClusterDemo from activecluster...

/**
 * @version $Revision$
 */
public class
  ClusterDemo
{
  protected Cluster                   _cluster;
  protected ActiveMQConnectionFactory _connFactory = new ActiveMQConnectionFactory("multicast://224.1.2.3:5123");
  protected String                    _id;
  protected TopologyStrategy          _topology;
  protected int                       _cellSize=2;

  public
    ClusterDemo(String id, int cellSize)
    {
      _id=id;
      _cellSize=cellSize;
    }

  protected void
    start()
    throws JMSException, ClusterException
    {
      _cluster = createCluster();
      Map state=new HashMap();
      state.put("id", _id);
      _cluster.getLocalNode().setState(state);
      _topology=new NChooseKTopologyStrategy(_cluster, _cellSize);
      _topology.start();
      _cluster.addClusterListener(_topology);
      _cluster.start();
    }

  protected void
    stop()
    throws JMSException
    {
      _cluster.stop();
      _topology.stop();
      _connFactory.stop();
    }

  protected Cluster
    createCluster()
    throws JMSException, ClusterException
    {
      Connection connection = _connFactory.createConnection();
      DefaultClusterFactory factory = new DefaultClusterFactory(connection);
      return factory.createCluster("ORG.CODEHAUS.WADI.TEST.CLUSTER");

    }

  public static class
    Peer
    implements Comparable
    {
      protected String      _id;
      protected Destination _dest;
      protected Node        _node;

      public int
	compareTo(Object o)
	{
	  assert o.getClass()==Peer.class;
	  return _id.compareTo(((Peer)o)._id);
	}

      public
	Peer(Node node)
	{
	  _id=(String)(node.getState().get("id"));
	  _dest=null;
	  _node=node;
	}

      public void setNode(Node node){_node=node;}

      public String
	toString()
	{
	  return "<Peer:"+_id+">";
	}

      public String getId(){return _id;}
    }

  public interface
    TopologyStrategy
    extends ClusterListener
    {
      // should extend some LifeCycle i/f...
      void start();
      void stop();
    }

  public abstract class
    AbstractTopologyStrategy
    implements TopologyStrategy
    {
      protected Log _log=LogFactory.getLog(getClass().getName()+"#"+_id);
      protected Map _peers=new TreeMap();

      protected Cluster _cluster;
      protected Peer    _localPeer;

      public
	AbstractTopologyStrategy(Cluster cluster)
	{
	  _cluster=cluster;
	}

      public Peer getLocalPeer(){return _localPeer;}

      public void
	start()
	{
	  Node localNode=_cluster.getLocalNode();
	  onNodeAdd(new ClusterEvent(_cluster, localNode, ClusterEvent.ADD_NODE));
	  _localPeer=(Peer)_peers.get(localNode.getState().get("id"));
	}

      public void
	stop()
	{
	  onNodeRemove(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.REMOVE_NODE));
	  _localPeer=null;
	}

      public void
	onNodeAdd(ClusterEvent event)
	{
	  Peer p=new Peer(event.getNode());
	  Collection peers=null;

	  synchronized (_peers)
	  {
	    _peers.put(p.getId(), p);
	    peers=_peers.values();
	  }

	  _log.info("adding: " + p);
	  //	  _log.info("nodes : " + peers);

	  add(p);
	}

      // do we need this yet ?
      public void
	onNodeUpdate(ClusterEvent event)
	{
	  Node node=event.getNode();
	  String id=(String)(node.getState().get("id"));
	  Peer p=null;
	  Collection peers=null;
	  synchronized (_peers)
	  {
	    p=(Peer)_peers.get(id);
	    peers=_peers.values();
	  }
	  p.setNode(node);	// important - this is the update...

	  _log.info("updating: " + p);
	  _log.info("nodes   : " + peers);
	}

      public void
	onNodeRemove(ClusterEvent event)
	{
	  Node node=event.getNode();
	  String id=(String)node.getState().get("id");
	  Peer p=null;
	  Collection peers=null;
	  synchronized (_peers)
	  {
	    p=(Peer)_peers.remove(id);
	    peers=_peers.values();
	  }

	  _log.info("removing: " + p);
	  //	  _log.info("nodes   : " + peers);

	  remove(p);
	}

      public abstract void add(Peer p);
      public abstract void remove(Peer p);
    }

  public class
    Cell
    {
      protected Log        _log=LogFactory.getLog(getClass());
      protected String     _id;
      protected Collection _peers;

      public Cell(String id, Collection peers)
	{
	  _id=id;
	  _peers=peers;
	}

      public void
	start()
	{
	  _log.info("starting: "+_id);
	}

      public void
	stop()
	{
	  _log.info("stopping: "+_id);
	}
    }

  public class
    NChooseKTopologyStrategy
    extends AbstractTopologyStrategy
    {
      protected int _k=1;
      protected Map _cells=new TreeMap();

      public
	NChooseKTopologyStrategy(Cluster cluster, int k)
	{
	  super(cluster);
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
		  String id="";
		  for (Iterator n=comb.iterator(); n.hasNext(); )
		    id+=((id.length()==0)?"":"-")+((Peer)n.next()).getId();
		  combs.put(id, comb);
		}
	      }
	    }
	  }

	  return combs;
	}
    }

  //----------------------------------------


  public static void
    main(String[] args)
    {
      Log log=LogFactory.getLog(ClusterDemo.class);

      int nPeers=Integer.parseInt(args[0]);
      int cellSize=Integer.parseInt(args[1]);

      for (int i=0; i<nPeers; i++)
      {
	try
	{
	  String pid=System.getProperty("pid");
	  ClusterDemo test = new ClusterDemo("node"+pid+"."+i, cellSize);
	  test.start();
	}
	catch (JMSException e)
	{
	  log.warn("unexpected problem", e);
	  Exception c = e.getLinkedException();
	  if (c != null)
	    log.warn("unexpected problem", c);
	}
	catch (Throwable e)
	{
	  log.warn("unexpected problem", e);
	}
      }
    }
}
