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
import org.codehaus.wadi.Session;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1533 $
 */

public abstract class AbstractSession extends AbstractMotable implements Session {
    protected static Log _log = LogFactory.getLog(AbstractSession.class);

    protected final RankedRWLock _lock = new RankedRWLock();

    public Sync getSharedLock() {
        return _lock.readLock();
    }

    public Sync getExclusiveLock() {
        return _lock.writeLock();
    }

    public byte[] getBodyAsByteArray() throws Exception {
        return Utils.getContent(this, new SimpleStreamer());
    }

}
