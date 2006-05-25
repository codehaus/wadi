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
package org.codehaus.wadi.activecluster;

import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;

public class ActiveClusterPeer implements Peer, Address, Comparable {

   protected final transient ActiveClusterCluster _cluster;
   protected final transient Map _state;

   protected javax.jms.Destination _acDestination; // set at init()-time

   public ActiveClusterPeer(ActiveClusterCluster cluster) {
       super();
       _cluster=cluster;
       _state=new HashMap();
   }

   // 'java.lang.Object' API

   protected Object readResolve() throws ObjectStreamException {
       // somehow always return same instance...
       return ActiveClusterCluster.get(_acDestination);
   }

   public int hashCode() {
       return _acDestination==null?0:_acDestination.hashCode();
   }

   public boolean equals(Object object) {
       return this==object;
   }

   // 'java.lang.Comparable' API

   public int compareTo(Object object) {
       return _acDestination.toString().compareTo(((ActiveClusterPeer)object).getACDestination().toString()); // painful - improve - TODO
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

   public void init(javax.jms.Destination acDestination) {
       _acDestination=acDestination;
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
           if (_state!=state) {
               _state.clear();
               _state.putAll(state);
           }
       }
   }

   public javax.jms.Destination getACDestination() {
       return _acDestination;
   }

}

