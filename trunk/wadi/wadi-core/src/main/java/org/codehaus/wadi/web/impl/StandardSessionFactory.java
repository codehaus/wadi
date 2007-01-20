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

import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.BasicWebSessionConfig;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionConfig;
import org.codehaus.wadi.web.WebSessionFactory;
import org.codehaus.wadi.web.WebSessionWrapperFactory;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1885 $
 */
public class StandardSessionFactory implements WebSessionFactory {

    protected final WebSessionConfig config;
    protected final AttributesFactory attributesFactory;
    protected final WebSessionWrapperFactory wrapperFactory;
    protected final ValuePool valuePool;
    protected final Router router;

    public StandardSessionFactory(AttributesFactory attributesFactory,
            WebSessionWrapperFactory wrapperFactory,
            ValuePool valuePool,
            Router router) {
        if (null == attributesFactory) {
            throw new IllegalArgumentException("attributesFactory is required");
        } else if (null == wrapperFactory) {
            throw new IllegalArgumentException("wrapperFactory is required");
        } else if (null == valuePool) {
            throw new IllegalArgumentException("valuePool is required");
        } else if (null == router) {
            throw new IllegalArgumentException("router is required");
        }
        this.attributesFactory = attributesFactory;
        this.wrapperFactory = wrapperFactory;
        this.valuePool = valuePool;
        this.router = router;
        
        config = new BasicWebSessionConfig();
    }

    public WebSessionConfig getWebSessionConfig() {
        return config;
    }
    
    public WebSession create() {
        return new StandardSession(config,
                attributesFactory,
                wrapperFactory,
                valuePool,
                router,
                getManager());
    }

    protected Manager getManager() {
        return config.getManager();
    }
    
}
