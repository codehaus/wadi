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
package org.codehaus.wadi.dindex.impl;

import java.util.Comparator;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class PartitionOwnerGreaterThanComparator implements Comparator {

    public int compare(Object o2, Object o1) {
        PartitionOwner p1=(PartitionOwner)o1;
        PartitionOwner p2=(PartitionOwner)o2;
        int tmp=p1._deviation-p2._deviation;
        if (tmp!=0)
            return tmp;
        else
            return p1._node.getName().compareTo(p2._node.getName());
    }

    public boolean equals(Object obj) {
        return obj==this || obj.getClass()==getClass();
    }

}
