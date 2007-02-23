/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.axis2;

import javax.servlet.http.HttpSession;
import org.codehaus.wadi.web.WebSessionWrapperFactory;

// hmm... - we need to return an Axis2Session, not an HttpSession...

public class Axis2SessionWrapperFactory implements WebSessionWrapperFactory {
    
    public HttpSession create(org.codehaus.wadi.web.WebSession session) {
        return new Axis2Session(session);
    }
    
}