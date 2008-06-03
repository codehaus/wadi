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
package org.codehaus.wadi.core.store;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicStoreMotableTest extends RMockTestCase {

    private Store store;
    private BasicStoreMotable storeMotable;

    protected void setUp() throws Exception {
        store = (Store) mock(Store.class);
        storeMotable = new BasicStoreMotable(store);
    }
    
    public void testGetBodyAsByteArray() throws Exception {
        store.loadBody(storeMotable);
        byte[] body = new byte[0];
        modify().returnValue(body);
        
        startVerification();
        
        byte[] actualBody = storeMotable.getBodyAsByteArray();
        assertSame(body, actualBody);
    }
    
    public void testSetBodyAsByteArray() throws Exception {
        store.insert(storeMotable);
        store.loadBody(storeMotable);
        
        startVerification();
        
        byte[] body = new byte[0];
        storeMotable.setBodyAsByteArray(body);
        storeMotable.getBodyAsByteArray();
    }
    
    public void testDestroy() throws Exception {
        store.delete(storeMotable);
        startVerification();
        
        storeMotable.destroy();
    }

    public void testDestroyForMotion() throws Exception {
        store.delete(storeMotable);
        startVerification();
        
        storeMotable.destroyForMotion();
    }
    
}
