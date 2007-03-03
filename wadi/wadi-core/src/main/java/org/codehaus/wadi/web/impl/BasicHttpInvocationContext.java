/**
 * Copyright 2007 The Apache Software Foundation
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

import javax.servlet.http.HttpServletRequestWrapper;

import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.web.HttpInvocationContext;

/**
 * @version $Revision: 1886 $
 */
public class BasicHttpInvocationContext extends HttpServletRequestWrapper implements HttpInvocationContext {
    private final WebInvocation webInvocation;
    
    public BasicHttpInvocationContext(Invocation invocation) {
        super(((WebInvocation) invocation).getHreq());
        this.webInvocation = (WebInvocation) invocation;
    }

    public Invocation getInvocation() {
        return webInvocation;
    }
    
    public WebInvocation getWebInvocation() {
        return webInvocation;
    }
    
}
