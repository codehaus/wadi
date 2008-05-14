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
package org.codehaus.wadi.location.partition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionRepopulateRequest implements PartitionRequestMessage, Serializable {
    private final int[] keys;

    public PartitionRepopulateRequest(int[] keys) {
        if (null == keys || keys.length == 0) {
            throw new IllegalArgumentException("keys is required");
        }
        this.keys = keys;
    }

    public Map createKeyToSessionNames() {
        Map partitionIndexToSessionNames = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            partitionIndexToSessionNames.put(new Integer(keys[i]), new ArrayList());
        }
        return partitionIndexToSessionNames;
    }

    public int[] getKeys() {
        return keys;
    }

    public String toString() {
        return "PartitionRepopulateRequest [" + keys + "]";
    }

}
