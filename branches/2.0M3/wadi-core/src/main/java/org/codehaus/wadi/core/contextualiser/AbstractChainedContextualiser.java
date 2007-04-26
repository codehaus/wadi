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
package org.codehaus.wadi.core.contextualiser;

import java.util.Map;

import org.codehaus.wadi.location.partitionmanager.PartitionMapper;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractChainedContextualiser extends AbstractContextualiser {
    protected final Contextualiser next;

    public AbstractChainedContextualiser(Contextualiser next) {
        if (null == next) {
            throw new IllegalArgumentException("next is required");
        }
        this.next = next;
    }

    public void start() throws Exception {
        super.start();
        next.start();
    }

    public void stop() throws Exception {
        next.stop();
        super.stop();
    }

    public void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
        super.findRelevantSessionNames(mapper, keyToSessionNames);
        next.findRelevantSessionNames(mapper, keyToSessionNames);
    }
    
}
