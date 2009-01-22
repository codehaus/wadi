/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.replication.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.codehaus.wadi.group.Peer;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BlackListSecondaryFilter implements SecondaryFilter {
    private final Set<Peer> blackListed;
    
    public BlackListSecondaryFilter(Peer blackListed) {
        this(Collections.singleton(blackListed));
    }

    public BlackListSecondaryFilter(Set<Peer> blackListed) {
        if (null == blackListed) {
            throw new IllegalArgumentException("blackListed is required");
        }
        this.blackListed = blackListed;
    }
    
    public List<Peer> filter(List<Peer> secondaries) {
        List<Peer> filteredList = new ArrayList<Peer>(secondaries);
        filteredList.removeAll(blackListed);
        return filteredList;
    }
}
