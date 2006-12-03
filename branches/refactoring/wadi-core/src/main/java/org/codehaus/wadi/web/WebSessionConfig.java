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
package org.codehaus.wadi.web;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.codehaus.wadi.Config;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.ValuePool;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1885 $
 */
public interface WebSessionConfig extends Config {
    ValuePool getValuePool();

    AttributesFactory getAttributesFactory();
    
    HttpSessionListener[] getSessionListeners();
    
    HttpSessionAttributeListener[] getAttributeListeners();
    
    ServletContext getServletContext();

    void destroy(Invocation invocation, WebSession session);
    
    WebSessionWrapperFactory getSessionWrapperFactory();
    
    SessionIdFactory getSessionIdFactory();

    int getMaxInactiveInterval();

    Router getRouter();
}
