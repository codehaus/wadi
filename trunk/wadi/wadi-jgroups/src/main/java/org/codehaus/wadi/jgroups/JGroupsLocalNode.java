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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.jgroups.messages.StateUpdate;

public class JGroupsLocalNode implements LocalPeer {

  protected final Log _log=LogFactory.getLog(getClass());
  protected final JGroupsCluster _cluster;
  protected JGroupsAddress _destination;
  protected Map _clusterState;
  protected Map _localState;

  public JGroupsLocalNode(JGroupsCluster cluster, Map state) {
    super();
    _cluster=cluster;
    _clusterState=state;
  }

  // 'Node' api

  public Map getState() {
    if (_destination==null) {
      return _localState;
    } else
      synchronized (_clusterState) {
        return (Map)_clusterState.get(_destination.getAddress());
      }
  }

  public Address getAddress() {
    return _destination;
  }

  public void setDestination(JGroupsAddress destination) {
    _destination=destination;
    synchronized (_clusterState) {
      _clusterState.put(_destination.getAddress(), _localState);
      _localState=null;
    }

  }

  public String getName() {
      Map state=getState();
      return (state==null)?"<unknown>":(String)state.get("nodeName");
  }

  public boolean isCoordinator() {
    throw new UnsupportedOperationException("NYI");
  }

  public Object getZone() {
    throw new UnsupportedOperationException("NYI");
  }

  // 'JGroupsLocalNode' api

  public void setState(Map state) throws MessageExchangeException {
    if (_destination==null) {
      // we have not yet been initialised...
      _localState=state;
    } else {
      _localState=null;
      Object tmp;
      synchronized (_clusterState) {
        tmp=_clusterState.put(_destination.getAddress(), state);
      }

      if (tmp!=null) {
        Message message=new JGroupsMessage();
        message.setReplyTo(_destination);
        message.setPayload(new StateUpdate(state));
        _cluster.send(_cluster.getAddress(), message);
      }
    }
  }

  protected static final String _prefix="<"+Utils.basename(JGroupsLocalNode.class)+": ";
  protected static final String _suffix=">";

  public String toString() {
    return _prefix+getName()+_suffix;
  }

}
