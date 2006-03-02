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
package org.codehaus.wadi.gridstate.jgroups;

import java.util.Map;
import javax.jms.Destination;
import org.activecluster.Node;

public class JGroupsRemoteNode implements Node {
  
  protected final JGroupsDestination _destination;
  
  protected Map _state;
  
  public JGroupsRemoteNode(JGroupsDestination destination, Map state) {
    super();
    _destination=destination;
    _state=state;
  }

  // 'JGroupsRemoteNode' api
  
  public void setState(Map state) {
    _state=state;
  }
  
  // 'Node' api
  
  public Destination getDestination() {
    return _destination;
  }

  public Map getState() {
    return _state;
  }

  public String getName() {
    throw new UnsupportedOperationException("NYI");
  }

  public boolean isCoordinator() {
    throw new UnsupportedOperationException("NYI");
  }

  public Object getZone() {
    throw new UnsupportedOperationException("NYI");
  }

}
