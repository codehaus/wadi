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

import java.io.Serializable;
import org.codehaus.wadi.group.Message;
import org.jgroups.Address;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1647 $
 */
public class JGroupsMessage implements Message, Serializable {

	protected transient Address _replyTo;
	protected transient Address _address;
	protected String _outgoingCorrelationId;
	protected String _incomingCorrelationId;
	protected Serializable _letter;

  protected transient JGroupsCluster _cluster;

  // JGroupObjectMessage API

  public String getIncomingCorrelationId() {
    return _incomingCorrelationId;
  }

  public void setIncomingCorrelationId(String correlationId) {
    _incomingCorrelationId=correlationId;
  }

  public String getOutgoingCorrelationId() {
    return _outgoingCorrelationId;
  }

  public void setOutgoingCorrelationId(String correlationId) {
    _outgoingCorrelationId=correlationId;
  }
  
  public void setCluster(JGroupsCluster cluster) {
    _cluster=cluster;
  }

  protected static String _prefix="<"+Utils.basename(JGroupsMessage.class)+": ";
  protected static String _suffix=">";

  public String toString() {
    return _prefix+_letter+_suffix;
  }

    public org.codehaus.wadi.group.Address getReplyTo() {
        return _cluster.getAddress(_replyTo);
    }

    public void setReplyTo(org.codehaus.wadi.group.Address replyTo) {
        _replyTo=((JGroupsAddress)replyTo).getAddress();
    }

    public org.codehaus.wadi.group.Address getAddress() {
        return _cluster.getAddress(_address);
    }

    public void setAddress(org.codehaus.wadi.group.Address address) {
        _address = ((JGroupsAddress)address).getAddress();
    }
    
    public void setPayload(Serializable payload) {
        _letter = payload;
    }

    public Serializable getPayload() {
        return _letter;
    }
}
