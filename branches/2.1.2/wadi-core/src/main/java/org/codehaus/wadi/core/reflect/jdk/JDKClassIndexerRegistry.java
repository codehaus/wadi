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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.wadi.core.reflect.MemberUpdater;
import org.codehaus.wadi.core.reflect.base.AbstractClassIndexerRegistry;
import org.codehaus.wadi.core.reflect.base.MemberFilter;


/**
 * 
 * @version $Revision: 1538 $
 */
public class JDKClassIndexerRegistry extends AbstractClassIndexerRegistry {

    public JDKClassIndexerRegistry(MemberFilter memberFilter) {
        super(memberFilter);
    }

    @Override
    protected MemberUpdater newMemberUpdater(int index, Field field) {
        return new FieldUpdater(index, field);
    }

    @Override
    protected MemberUpdater newMemberUpdater(int index, Method method) {
        return new MethodUpdater(index, method);
    }

    @Override
    protected MemberUpdater newMemberUpdater(int index, Constructor constructor) {
        return new ConstructorUpdater(index, constructor);
    }

}
