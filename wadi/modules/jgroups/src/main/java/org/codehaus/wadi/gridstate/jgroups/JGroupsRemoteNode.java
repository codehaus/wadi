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

import java.util.Collections;
import java.util.Map;
import javax.jms.Destination;
import org.activecluster.Node;
import org.codehaus.wadi.impl.Utils;

public class JGroupsRemoteNode implements Node {
  
  protected final JGroupsCluster _cluster;
  protected final JGroupsDestination _destination;
  protected Map _clusterState;
  
  public JGroupsRemoteNode(JGroupsCluster cluster, JGroupsDestination destination, Map state) {
    super();
    _cluster=cluster;
    _destination=destination;
    _clusterState=state;
  }

  // 'Node' api
  
  public Destination getDestination() {
    return _destination;
  }

  public Map getState() {
    synchronized (_clusterState) {
      return (Map)_clusterState.get(_destination.getAddress());
    }
  }

  public String getName() {
    Map state=getState();
    state=(state==null?Collections.EMPTY_MAP:state);
    String name=(String)state.get("nodeName");
    name=(name==null?"<unknown>":name);
    return name; // FIXME - duplicates code in Dispatcher...
  }

  public boolean isCoordinator() {
    throw new UnsupportedOperationException("NYI");
  }

  public Object getZone() {
    throw new UnsupportedOperationException("NYI");
  }

  // 'JGroupsRemoteNode' api
  
  public void setState(Map state) {
    synchronized (_clusterState) {
      _clusterState.put(_destination.getAddress(), state);
    }
  }
  
  protected static final String _prefix="<"+Utils.basename(JGroupsRemoteNode.class)+": ";
  protected static final String _suffix=">";
  
  public String toString() {
    return _prefix+getName()+_suffix;
  }

}
