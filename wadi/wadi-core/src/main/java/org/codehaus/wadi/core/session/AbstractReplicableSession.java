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
package org.codehaus.wadi.core.session;

import org.codehaus.wadi.Manager;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.Streamer;

/**
 * A DistributableSession enhanced with functionality associated with replication - the frequent 'backing-up' of 
 * its content to provide against catastrophic failure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1725 $
 */
public abstract class AbstractReplicableSession extends DistributableSession {
    protected final transient Replicater replicater;

	public AbstractReplicableSession(Attributes attributes, Manager manager, Streamer streamer, Replicater replicater) {
        super(attributes, manager, streamer);
        if (null == replicater) {
            throw new IllegalArgumentException("replicater is required");
        }
        this.replicater = replicater;
    }

    public synchronized void mote(Motable recipient) throws Exception {
        recipient.copy(this);
        destroyForMotion(); 
    }

    public synchronized void destroy() throws Exception {
        replicater.destroy(this);
        super.destroy();
    }

}
