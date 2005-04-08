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

import java.io.Serializable;

/**
 * Help with the [de]serialisation of non-Serializable types. Registered
 * via AttributeWrapper.registerHelper(Class type, AttributeHelper helper).
 * N.B that Serializables produced by the write() method should be of a type
 * that is both registered (otherwise the Helper will not be found on deserialisation)
 * and sufficiently unique that it does not match other non-problematic Attributes
 * that started life as Serializables.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public interface AttributeHelper extends Serializable {
    
    /**
     * Used during the writing out of a non-Serializable. If its type matches
     * a registered Helper, that Helper's write() method will be used to convert
     * it into a Serializable representation, which will then be written out.
     * 
     * @param output - a non-Serializable, which is about to be serialised
     * @return - a Serializable, which will be serialised in place of the original Object
     */
    public Serializable write(Object output);

    /**
     * Used during reading in of an, originally, non-Serializable. If the type of the 
     * replacement Object read in matches a registered Helper, that Helper's read()
     * method will be used to convert it back into its original non-Serializable form,
     * which is then placed back into its Attribute.
     * 
     * @param input
     * @return
     */
    public Object read(Serializable input);
    
}
