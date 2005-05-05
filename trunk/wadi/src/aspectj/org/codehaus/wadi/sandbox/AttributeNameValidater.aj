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
package org.codehaus.wadi;

public aspect AttributeNameValidater {
    
    pointcut validate(String name) : execution(Object Session+.*Attribute(..)) && args(name,..);
    
    before(String name) : validate(name) {
        if (null==name)
            throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
    }

}
