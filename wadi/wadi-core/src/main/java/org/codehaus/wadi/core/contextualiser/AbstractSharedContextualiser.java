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

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.core.motable.Motable;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractSharedContextualiser extends AbstractMotingContextualiser {
    
    public AbstractSharedContextualiser(Contextualiser next) {
        super(next);
    }

    public boolean contextualise(Invocation invocation, String key, Immoter immoter, boolean exclusiveOnly)
            throws InvocationException {
        if (exclusiveOnly) {
            return false;
        }
        return super.contextualise(invocation, key, immoter, exclusiveOnly);
    }
    
    public void promoteToExclusive(Immoter immoter) {
        Emoter emoter = getEmoter();
        load(emoter, immoter);
        next.promoteToExclusive(immoter);
    }
    
    public Immoter getSharedDemoter() {
        return getImmoter();
    }
    
    public Immoter getDemoter(String name, Motable motable) {
        return getImmoter();
    }

    protected void load(Emoter emoter, Immoter immoter) {
    }

    public void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
    }
    
}
