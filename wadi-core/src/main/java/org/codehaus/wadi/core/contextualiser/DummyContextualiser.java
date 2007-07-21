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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.core.eviction.DummyEvicter;
import org.codehaus.wadi.core.eviction.Evicter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.location.partitionmanager.PartitionMapper;

/**
 * A Contextualiser that does no contextualising
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyContextualiser implements Contextualiser {
    protected final Evicter _evicter = new DummyEvicter();
    protected final Immoter _immoter = new DummyImmoter();

    public boolean contextualise(Invocation invocation, String key, Immoter immoter, boolean exclusiveOnly)
            throws InvocationException {
        return false;
    }

    public Evicter getEvicter() {
        return _evicter;
    }
    
    public Set getSessionNames() {
        return Collections.EMPTY_SET;
    }
    
    public void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
    }

    public Immoter getDemoter(String name, Motable motable) {
        return _immoter;
    }

    public Immoter getSharedDemoter() {
        return _immoter;
    }

    public void promoteToExclusive(Immoter immoter) {
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

    public static class DummyImmoter implements Immoter {
        public boolean immote(Motable emotable, Motable immotable) {
            return true;
        }
        
        public Motable newMotable(Motable emotable) {
            return new SimpleMotable();
        }

        public boolean contextualise(Invocation invocation, String id, Motable immotable)
                throws InvocationException {
            return false;
        }
    }

}
