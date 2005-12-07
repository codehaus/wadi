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

import java.io.Serializable;

/**
 * Help with the [de]serialisation of non-Serializable types. Registered
 * via AttributeWrapper.registerHelper(Class type, AttributeHelper helper).
 * See the doc on java.io.Serializable for an explanation of the readResolve()
 * method.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public interface ValueHelper extends Serializable {
    
    /**
     * Used during the writing out of a non-Serializable. If its type matches
     * a registered Helper, that Helper's replace() method will be used to return
     * a Serializable Object that implements readResolve() to return an instance
     * with the same value as the original non-Serializable on deserialisation.
     * 
     * @param output - a non-Serializable, which is about to be serialised
     * @return - a Serializable, which will be serialised in place of the original Object
     */
    public Serializable replace(Object output);

}
