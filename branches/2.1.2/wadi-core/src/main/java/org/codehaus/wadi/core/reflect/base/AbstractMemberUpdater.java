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
package org.codehaus.wadi.core.reflect.base;

import java.lang.reflect.Member;

import org.codehaus.wadi.core.reflect.MemberUpdater;




/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractMemberUpdater implements MemberUpdater {
    private final int index;
    protected final Member member;
    
    public AbstractMemberUpdater(int index, Member member) {
        if (null == member) {
            throw new IllegalArgumentException("member is required");
        }
        this.index = index;
        this.member = member;
    }

    public int getIndex() {
        return index;
    }

    public Member getMember() {
        return member;
    }
    
}