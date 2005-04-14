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
package org.codehaus.wadi.sandbox;

import java.util.List;

import javax.servlet.ServletContext;

import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.sandbox.impl.Manager;

public interface SessionConfig {
    
    ValuePool getValuePool();
    AttributesPool getAttributesPool();
    List getSessionListeners();
    List getAttributeListeners();
    ServletContext getServletContext();
    
    Manager getManager(); // TODO - just for destroySession()...
    SessionWrapperFactory getSessionWrapperFactory();
    IdGenerator getSessionIdFactory();
    
    int getMaxInactiveInterval();
    
}
