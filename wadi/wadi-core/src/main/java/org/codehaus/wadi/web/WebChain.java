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
package org.codehaus.wadi.web;

import javax.servlet.FilterChain;
import org.codehaus.wadi.Chain;
import org.codehaus.wadi.Invocation;

/**
 * A WebChain encapsulates control flow for the web tier.
 * 
 * @author jules
 * @version $Revision: 1430 $
*/
public class WebChain implements Chain {

    protected FilterChain _chain;
    
    public void init(FilterChain chain) {
        _chain=chain;
    }
    
    public void clear() {
        _chain=null;
    }
    
    public void next(Invocation invocation) {
        
    }
    
}
