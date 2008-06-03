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
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.core.reflect.ClassIndexer;
import org.codehaus.wadi.core.reflect.MemberUpdater;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicClassIndexer implements ClassIndexer {
    private final MemberUpdater[] updaters;
    private final Map<Member, MemberUpdater> memberToUpdaters;

    public BasicClassIndexer(MemberUpdater[] updaters) {
        if (null == updaters) {
            throw new IllegalArgumentException("updaters is required");
        }
        this.updaters = updaters;
        
        memberToUpdaters = new HashMap<Member, MemberUpdater>();
        for (MemberUpdater memberUpdater : updaters) {
            memberToUpdaters.put(memberUpdater.getMember(), memberUpdater);
        }
    }

    public int getIndex(Member member) {
        MemberUpdater memberUpdater = memberToUpdaters.get(member);
        if (null == memberUpdater) {
            throw new IllegalArgumentException("[" + member + "] is not indexed.");
        }
        return memberUpdater.getIndex();
    }

    public MemberUpdater getMemberUpdater(int index) {
        return updaters[index];
    }
    
}