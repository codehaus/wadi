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
package org.codehaus.wadi.dindex;


import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.dindex.impl.PartitionFacade;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface StateManagerConfig {
  PartitionFacade getPartition(int key);

  PartitionFacade getPartition(Object key);
  
  int getNumPartitions();

  String getLocalPeerName();
  
  String getPeerName(Address address);

  Dispatcher getDispatcher();

  boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException;
  
  long getInactiveTime();

  Sync getInvocationLock(String name);
}
