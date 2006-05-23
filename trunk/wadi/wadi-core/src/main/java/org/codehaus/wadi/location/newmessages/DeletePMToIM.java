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
package org.codehaus.wadi.location.newmessages;

import java.io.Serializable;

import org.codehaus.wadi.location.DIndexResponse;

/**
 * Sent, from PartitionMaster to InvocationMaster, confirming removal of an item from the Partition's index.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DeletePMToIM implements DIndexResponse, Serializable {
  
  boolean _success;
  
  public DeletePMToIM(boolean success) {
    super();
    _success=success;
  }
  
  public String toString() {
    return "<DeletePMToIM>";
  }
  
}
