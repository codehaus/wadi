/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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

import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;

public class JGroupsPeer implements Peer, Address, Comparable {

    protected final transient JGroupsCluster _cluster;
    protected final transient Map _state;

    protected org.jgroups.Address _jgAddress; // set at init()-time

    public JGroupsPeer(JGroupsCluster cluster) {
        super();
        _cluster=cluster;
        _state=new HashMap();
    }

    // 'java.lang.Object' API

    protected Object readResolve() throws ObjectStreamException {
        // somehow always return same instance...
        return JGroupsCluster.get(_jgAddress);
    }

    public int hashCode() {
        return _jgAddress==null?0:_jgAddress.hashCode();
    }

    public boolean equals(Object object) {
        return this==object;
    }

    // 'java.lang.Comparable' API

    public int compareTo(Object object) {
        return _jgAddress.compareTo(((JGroupsPeer)object).getJGAddress());
    }

    // 'org.codehaus.wadi.group.Peer' API

    public Address getAddress() {
        return this;
    }

    public String getName() {
        return (String)getAttribute(_peerNameKey);
    }

    public Map getState() {
        return _state;
    }

    // 'org.codehaus.wadi.group.LocalPeer' API

    public void init(org.jgroups.Address jgAddress) {
        _jgAddress=jgAddress;
    }

    public Object getAttribute(Object key) {
        synchronized (_state) {return _state.get(key);}
    }

    public Object setAttribute(Object key, Object value) {
        synchronized (_state) {return _state.put(key, value);}
    }

    public Object removeAttribute(Object key) {
        synchronized (_state) {return _state.remove(key);}
    }

    // 'org.codehaus.wadi.jgroups.JGroupsPeer' API

    public void setState(Map state) throws MessageExchangeException {
        synchronized (_state) {
            _state.clear();
            _state.putAll(state);
        }
    }

    public org.jgroups.Address getJGAddress() {
        return _jgAddress;
    }

}
