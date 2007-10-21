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
package org.codehaus.wadi.aop.reflect.base;

import org.codehaus.wadi.aop.reflect.MemberUpdater;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicClassIndexerTest extends RMockTestCase {

    private BasicClassIndexer indexer;
    private MemberUpdater[] updaters;

    @Override
    protected void setUp() throws Exception {
        MemberUpdater updater1 = newUpdater("field1");
        MemberUpdater updater2 = newUpdater("field2");
        
        updaters = new MemberUpdater[] {updater1, updater2};
    }

    public void testGetIndex() throws Exception {
        startVerification();
        
        indexer = new BasicClassIndexer(updaters);
        int index =indexer.getIndex(updaters[0].getMember());
        assertEquals(updaters[0].getIndex(), index);
    }
    
    public void testGetMemberUpdater() throws Exception {
        startVerification();
        
        indexer = new BasicClassIndexer(updaters);
        MemberUpdater memberUpdater = indexer.getMemberUpdater(1);
        assertEquals(updaters[1], memberUpdater);
    }
    
    private MemberUpdater newUpdater(String fieldName) throws NoSuchFieldException {
        MemberUpdater updater = (MemberUpdater) mock(MemberUpdater.class);
        updater.getMember();
        modify().multiplicity(expect.from(0)).returnValue(DummyClass.class.getDeclaredField(fieldName));
        updater.getIndex();
        modify().multiplicity(expect.from(0)).returnValue(0);
        return updater;
    }

    private static class DummyClass {
        private String field1;
        private String field2;
    }
    
}
