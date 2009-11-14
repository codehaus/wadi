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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;

import org.codehaus.wadi.core.manager.Manager;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicWebSessionConfig implements WebSessionConfig {
    private HttpSessionAttributeListener[] attributeListeners = new HttpSessionAttributeListener[0];
    private ServletContext servletContext;
    private Manager manager;
                                 
    public HttpSessionAttributeListener[] getAttributeListeners() {
        return attributeListeners;
    }

    public void setAttributeListeners(HttpSessionAttributeListener[] attributeListeners) {
        if (null == attributeListeners) {
            attributeListeners = new HttpSessionAttributeListener[0];
        }
        this.attributeListeners = attributeListeners;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }
    
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

}
