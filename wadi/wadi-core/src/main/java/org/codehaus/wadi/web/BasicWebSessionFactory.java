/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.web;

import org.codehaus.wadi.Router;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.core.session.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.core.session.AttributesFactory;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.replication.ReplicaterFactory;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicWebSessionFactory extends AtomicallyReplicableSessionFactory implements WebSessionFactory {

    protected final WebSessionConfig webSessionConfig;
    protected final Router router;
    protected final WebSessionWrapperFactory wrapperFactory;

    public BasicWebSessionFactory(AttributesFactory attributesFactory,
            Streamer streamer,
            ReplicaterFactory replicaterFactory,
            Router router,
            WebSessionWrapperFactory wrapperFactory) {
        super(attributesFactory, streamer, replicaterFactory);
        if (null == router) {
            throw new IllegalArgumentException("router is required");
        } else if (null == wrapperFactory) {
            throw new IllegalArgumentException("wrapperFactory is required");
        }
        this.router = router;
        this.wrapperFactory = wrapperFactory;
        
        webSessionConfig = new BasicWebSessionConfig();
    }

    public WebSessionConfig getWebSessionConfig() {
        return webSessionConfig;
    }
    
    public Session create() {
        return new BasicWebSession(webSessionConfig,
                newAttributes(),
                wrapperFactory,
                router,
                getManager(),
                streamer,
                replicater);
    }
}
