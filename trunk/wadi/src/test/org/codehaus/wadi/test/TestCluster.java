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

import javax.jms.JMSException;
import javax.jms.Connection;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterException;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.activecluster.ClusterEvent;
import org.codehaus.activecluster.ClusterListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Test ActiveCluster, ActiveMQ, with an eye to putting WADI on top of
 * them.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestCluster
  extends TestCase
{
  protected Log _log=LogFactory.getLog(TestCluster.class);

  public TestCluster(String name)
    {
      super(name);
    }

  protected ActiveMQConnectionFactory connFactory;
  protected Cluster cluster;

  protected void
    setUp()
    throws Exception
    {
      connFactory = new ActiveMQConnectionFactory("multicast://224.1.2.3:5123");
      cluster = createCluster();
      cluster.start();
    }

  protected void
    tearDown()
    throws JMSException
    {
      cluster.stop();
      connFactory.stop();
    }

  //----------------------------------------

  class MyClusterListener
    implements ClusterListener
  {
    public void
      onNodeAdd(ClusterEvent ce)
    {
      _log.info("node added: " + ce.getNode());
    }

    public void
      onNodeFailed(ClusterEvent ce)
    {
      _log.info("node failed: " + ce.getNode());
    }

    public void
      onNodeRemoved(ClusterEvent ce)
    {
      _log.info("node removed: " + ce.getNode());
    }

    public void
      onNodeUpdate(ClusterEvent ce)
    {
      _log.info("node updated: " + ce.getNode());
    }
  }

  public void
    testCluster()
    throws Exception
    {
      cluster.addClusterListener(new MyClusterListener());

      Map map = new HashMap();
      map.put("text", "testing123");
      cluster.getLocalNode().setState(map);

      _log.info("nodes: " + cluster.getNodes());
      Thread.sleep(10000);
      assertTrue(true);
    }

  protected Cluster createCluster() throws JMSException, ClusterException {
    Connection connection = connFactory.createConnection();
    DefaultClusterFactory factory = new DefaultClusterFactory(connection);
    return factory.createCluster("ORG.CODEHAUS.ACTIVEMQ.TEST.CLUSTER");
  }
}

