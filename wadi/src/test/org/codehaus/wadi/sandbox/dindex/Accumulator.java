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
package org.codehaus.wadi.sandbox.dindex;

import java.util.ArrayList;
import java.util.Collection;

public class Accumulator {
    
    protected Collection _content=new ArrayList();
    
    public synchronized void put(Object object) {
        _content.add(object);
    }

    public synchronized void putAll(Collection collection) {
        _content.addAll(collection);
    }

    public synchronized Collection take() {
        Collection oldContent=_content;
        _content=new ArrayList();
        return oldContent;
    }

}
