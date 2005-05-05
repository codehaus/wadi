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

package org.codehaus.wadi.old.test;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.activecluster.Cluster;
import org.activecluster.ClusterException;
import org.activecluster.ClusterFactory;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.old.cluster.NChooseKTopologyStrategy;
import org.codehaus.wadi.old.cluster.TopologyStrategy;

// originally based on James' ClusterDemo from activecluster...

/**
 * @version $Revision$
 */
public class
  ClusterDemo
{
  protected Cluster                   _cluster;
  protected ActiveMQConnectionFactory _connFactory=Utils.getConnectionFactory();
  protected String                    _nodeId;
  protected TopologyStrategy          _topology;
  protected int                       _cellSize=2;

  public
    ClusterDemo(String id, int cellSize)
  {
    _nodeId=id;
    _cellSize=cellSize;
  }

  protected void
    start()
    throws JMSException, ClusterException
  {
    String clusterId="ORG.CODEHAUS.WADI.TEST.CLUSTER";
    ClusterFactory factory = new DefaultClusterFactory(_connFactory);
    //    factory.setInactiveTime(20000); // 20 secs ?
    _cluster= factory.createCluster(clusterId);
    Map state=new HashMap();
    state.put("id", _nodeId);
    _cluster.getLocalNode().setState(state);
    _topology=new NChooseKTopologyStrategy(_nodeId, clusterId, _cluster, factory, _cellSize);
    //_topology=new RingTopologyStrategy(_nodeId, clusterId, _cluster, factory, _cellSize);
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
