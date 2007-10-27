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
package org.codehaus.wadi.core.reflect.jdk;

import java.lang.reflect.Method;

import org.codehaus.wadi.core.reflect.MemberUpdaterException;
import org.codehaus.wadi.core.reflect.base.AbstractMemberUpdater;





/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodUpdater extends AbstractMemberUpdater {

    public MethodUpdater(int index, Method method) {
        super(index, method);
        method.setAccessible(true);
    }

    public Object executeWithParameters(Object target, Object[] parameters) throws MemberUpdaterException {
        try {
            return ((Method) member).invoke(target, parameters);
        } catch (Exception e) {
            throw new MemberUpdaterException(e);
        }
    }

}
