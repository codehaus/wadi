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

package org.codehaus.wadi.test;

import javax.jms.Destination;
import javax.jms.ObjectMessage;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterFactory;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestSimpleCluster
  extends TestCase
{
  protected Log _log=LogFactory.getLog(TestSimpleCluster.class);

  public TestSimpleCluster(String name)
  {
    super(name);
  }

  public void
    testSendToSelf()
    throws Exception
  {
    ActiveMQConnectionFactory conFact =new ActiveMQConnectionFactory("vm://localhost");// "multicast://224.1.2.3:5123"
    ClusterFactory clusFact           =new DefaultClusterFactory(conFact);
    Cluster node                      =clusFact.createCluster("ORG.CODEHAUS.WADI.TEST.CLUSTER");
    Destination thisNode              =node.getLocalNode().getDestination();
    Destination thisCluster           =node.getDestination();

    if (_log.isInfoEnabled()) {
        _log.info("Node:    " + thisNode);
        _log.info("Cluster: " + thisCluster);
    }


    // attach listeners here...

    node.start();

      if (_log.isInfoEnabled()) _log.info("started node: " + thisNode);

    ObjectMessage om=node.createObjectMessage();
    om.setObject("payload");
    node.send(thisNode, om);

    // wait for messages here...

    node.stop();

    _log.info("request/response to self OK");
  }
}
