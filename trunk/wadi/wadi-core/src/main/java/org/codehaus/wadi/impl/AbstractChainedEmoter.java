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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Motable;


/**
 * A basic Emoter for ChainedContextualisers
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractChainedEmoter implements Emoter {
	protected final Log _log = LogFactory.getLog(getClass());

    public boolean prepare(String name, Motable emotable, Motable immotable) {
        try {
            emotable.mote(immotable);
            return true;
        } catch (Exception e) {
            _log.warn("problem transferring data ownership: " + name, e);
            return false;
        }
    }

    public void commit(String name, Motable motable) {
        // do nothing
    }

    public void rollback(String name, Motable motable) {
        // immotable.mote(emotable);
    }

}
