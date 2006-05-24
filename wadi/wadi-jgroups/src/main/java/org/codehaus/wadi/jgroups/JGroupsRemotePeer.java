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

import java.util.Collections;
import java.util.Map;


import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;


public class JGroupsRemotePeer implements Peer {

  protected final JGroupsClusterMessageListener _cluster;
  protected final JGroupsAddress _address;
  protected Map _clusterState;

  public JGroupsRemotePeer(JGroupsClusterMessageListener cluster, JGroupsAddress address, Map state) {
    super();
    _cluster=cluster;
    _address=address;
    _clusterState=state;
  }

  // 'Node' api

  public Address getAddress() {
    return _address;
  }

  public Map getState() {
    synchronized (_clusterState) {
      return (Map)_clusterState.get(_address.getAddress());
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
      _clusterState.put(_address.getAddress(), state);
    }
  }

  public boolean equals(Object object) {
      return (object instanceof JGroupsRemotePeer && ((JGroupsRemotePeer)object).getAddress()==_address);
  }

  protected static final String _prefix="<"+Utils.basename(JGroupsRemotePeer.class)+": ";
  protected static final String _suffix=">";

  public String toString() {
    return _prefix+getName()+_suffix;
  }
  
}
