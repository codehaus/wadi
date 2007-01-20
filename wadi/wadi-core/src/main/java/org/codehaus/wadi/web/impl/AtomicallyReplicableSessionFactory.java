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
package org.codehaus.wadi.web.impl;

import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.ValueHelperRegistry;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionWrapperFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1885 $
 */
public class AtomicallyReplicableSessionFactory extends DistributableSessionFactory {

    protected final Replicater replicater;
    
    public AtomicallyReplicableSessionFactory(AttributesFactory attributesFactory,
            WebSessionWrapperFactory wrapperFactory,
            ValuePool valuePool,
            Router router,
            Streamer streamer,
            ValueHelperRegistry valueHelperRegistry,
            ReplicaterFactory replicaterFactory) {
        super(attributesFactory, wrapperFactory, valuePool, router, streamer, valueHelperRegistry);
        if (null == replicaterFactory) {
            throw new IllegalArgumentException("replicaterFactory is required");
        }
        this.replicater = replicaterFactory.create();
    }

    public WebSession create() {
        return new AtomicallyReplicableSession(config,
                attributesFactory,
                wrapperFactory,
                valuePool,
                router,
                getManager(),
                streamer,
                valueHelperRegistry,
                replicater);
    }
    
}

