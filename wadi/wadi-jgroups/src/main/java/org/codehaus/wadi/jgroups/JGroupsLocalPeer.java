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
public class JGroupsLocalPeer extends JGroupsPeer implements LocalPeer {

    protected static final String _prefix="<"+Utils.basename(JGroupsLocalPeer.class)+": ";
    protected static final String _suffix=">";

    public JGroupsLocalPeer(JGroupsCluster cluster) {
        super(cluster);
    }

    // 'java.lang.Object' API

    public String toString() {
        return _prefix+getName()+_suffix;
    }

    // 'org.codehaus.wadi.group.Peer' API
    
    public void setState(Map state) throws MessageExchangeException {
        super.setState(state);
        
        // notify the rest of the Cluster (if we are connected) of the change - TODO - do this more efficiently...
        if (_jgAddress!=null) {
            Message message=new JGroupsMessage();
            message.setReplyTo(this);
            message.setPayload(new StateUpdate(state));
            _cluster.send(_cluster.getAddress(), message);
        }
    }
    
    public Map copyState() {
        synchronized (_state) {return new HashMap(_state);}
    }

}
