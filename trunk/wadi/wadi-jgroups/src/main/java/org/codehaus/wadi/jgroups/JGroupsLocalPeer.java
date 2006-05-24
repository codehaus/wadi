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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.jgroups.messages.StateUpdate;

/**
 * A WADI LocalPeer mapped onto JGroups
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsLocalPeer implements LocalPeer, Comparable {

    protected static final String _prefix="<"+Utils.basename(JGroupsLocalPeer.class)+": ";
    protected static final String _suffix=">";

    protected final Log _log=LogFactory.getLog(getClass());
    protected final JGroupsAddress _address=new JGroupsAddress(this);
    protected final JGroupsCluster _cluster;
    protected final Map _clusterState;

    protected Map _localState;

    public JGroupsLocalPeer(JGroupsCluster cluster, Map clusterState) {
        super();
        _cluster=cluster;
        _clusterState=clusterState;
        _localState=new HashMap();
    }

    // 'java.lang.Object' API

    public String toString() {
        return _prefix+getName()+_suffix;
    }

    public boolean equals(Object object) {
        return (object instanceof JGroupsLocalPeer && ((JGroupsLocalPeer)object).getAddress()==_address);
    }

    // 'java.lang.Comparable' API

    public int compareTo(Object object) {
        return _address.compareTo(object); 
    }

    // 'org.codehaus.wadi.group.Peer' API

    public Map getState() {
        if (_address==null) {
            return _localState;
        } else
            synchronized (_clusterState) {
                return (Map)_clusterState.get(_address.getAddress());
            }
    }

    public Address getAddress() {
        return _address;
    }

    public void init(org.jgroups.Address jgaddress) {
        _address.init(jgaddress);
        synchronized (_clusterState) {
            _clusterState.put(jgaddress, _localState);
        }
    }

    public String getName() {
        Map state=getState();
        return (state==null)?"<unknown>":(String)state.get("nodeName");
    }

    // 'org.codehaus.wadi.group.LocalPeer' API

    public void setState(Map state) throws MessageExchangeException {
        if (_address==null) {
            // we have not yet been initialised...
            _localState=state;
        } else {
            _localState=null;
            Object tmp;
            synchronized (_clusterState) {
                tmp=_clusterState.put(_address.getAddress(), state);
            }

            if (tmp!=null) {
                Message message=new JGroupsMessage();
                message.setReplyTo(_address);
                message.setPayload(new StateUpdate(state));
                _cluster.send(_cluster.getAddress(), message);
            }
        }
    }

}
