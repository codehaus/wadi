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
package org.codehaus.wadi.core.session;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValueHelperRegistry;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicValueHelperRegistry implements ValueHelperRegistry {
    protected final List helpers = new ArrayList();

    /**
     * Register a ValueHelper for a particular type. During [de]serialisation
     * Objects flowing in/out of the persistance medium will be passed through
     * this Helper, which will have the opportunity to convert them between
     * Serializable and non-Serializable representations. Helpers will be
     * returned in their registration order, so this is significant (as an
     * Object may implement more than one interface or registered type).
     * 
     * @param type
     * @param helper
     */
    public void registerHelper(Class type, ValueHelper helper) {
        helpers.add(new HelperPair(type, helper));
    }

    public boolean deregisterHelper(Class type) {
        int l = helpers.size();
        for (int i = 0; i < l; i++)
            if (type.equals(((HelperPair) helpers.get(i))._type)) {
                helpers.remove(i);
                return true;
            }
        return false;
    }

    public ValueHelper findHelper(Class type) {
        int l = helpers.size();
        for (int i = 0; i < l; i++) {
            HelperPair p = (HelperPair) helpers.get(i);
            if (p._type.isAssignableFrom(type))
                return p._helper;
        }
        return null;
    }

    protected static class HelperPair {
        final Class _type;
        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
            _type = type;
            _helper = helper;
        }
    }

}
