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
package org.codehaus.wadi.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.PartitionMapper;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractContextualiser implements Contextualiser {
    protected final Log _log = LogFactory.getLog(getClass());

    public void init(ContextualiserConfig config) {
    }

    public void start() throws Exception {
    }

    public String getStartInfo() {
        return "";
    }

    public void stop() throws Exception {
    }

    public String getStopInfo() {
        return "";
    }

    public void destroy() {
    }

    public void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
    }
    
}
