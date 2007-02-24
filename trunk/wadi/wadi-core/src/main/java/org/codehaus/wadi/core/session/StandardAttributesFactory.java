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
package org.codehaus.wadi.core.session;

import org.codehaus.wadi.ValueFactory;
import org.codehaus.wadi.web.Attributes;
import org.codehaus.wadi.web.AttributesFactory;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public class StandardAttributesFactory implements AttributesFactory {
    protected final ValueFactory valueFactory;

    public StandardAttributesFactory(ValueFactory valueFactory) {
        if (null == valueFactory) {
            throw new IllegalArgumentException("valueFactory is required");
        }
        this.valueFactory = valueFactory;
    }

    public Attributes create() {
        return new StandardAttributes(valueFactory);
    }
    
}
