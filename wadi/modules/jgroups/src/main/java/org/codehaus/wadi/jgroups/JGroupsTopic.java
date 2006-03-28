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
package org.codehaus.wadi.jgroups;

import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Topic;
import org.apache.activecluster.LocalNode;
import org.apache.activecluster.Node;
import org.jgroups.Address;

public class JGroupsTopic extends JGroupsDestination implements Topic {

  protected final String _name;

  public JGroupsTopic(String name, Address address) {
    super(address); // null Node
    _name=name;
    _node=new ClusterNode();
  }

  public void init(Node node) {
  }

  public String getTopicName() throws JMSException {
    return _name;
  }

  class ClusterNode implements LocalNode {

    // 'Node' api

    public Map getState() {
      throw new UnsupportedOperationException("NYI");
    }

    public void setState(Map state) {
      throw new UnsupportedOperationException("NYI");
    }

    public Destination getDestination() {
      throw new UnsupportedOperationException("NYI");
    }

    public void setDestination(JGroupsDestination destination) {
      throw new UnsupportedOperationException("NYI");
    }

    public String getName() {
      return "cluster";
    }

    public boolean isCoordinator() {
      throw new UnsupportedOperationException("NYI");
    }

    public Object getZone() {
      throw new UnsupportedOperationException("NYI");
    }
  }

}
