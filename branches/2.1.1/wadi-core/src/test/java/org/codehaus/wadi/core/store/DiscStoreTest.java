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
package org.codehaus.wadi.core.store;

import java.io.File;
import java.io.IOException;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.core.store.Store.Putter;
import org.codehaus.wadi.core.util.SimpleStreamer;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DiscStoreTest extends RMockTestCase {
    private static final File BASE_DIR = new File(System.getProperty("basedir", System.getProperty("user.dir")), "target");
    
    public void testDiscMotables() throws Exception {
        Store store = new DiscStore(new SimpleStreamer(), BASE_DIR, false);
        Motable sm0 = new SimpleMotable();
        long creationTime = System.currentTimeMillis();
        long lastAccessedTime = creationTime + 1;
        int maxInactiveInterval = 30 * 60;
        String name = "foo";
        byte[] bytes = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f' };
        sm0.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        sm0.setBodyAsByteArray(bytes);

        File file = new File(new File(BASE_DIR, DiscStore.SESSION_STORE_DIR_NAME), name + ".ser");
        file.delete();
        assertFalse(file.exists());

        Motable edm0 = store.create();
        assertFalse(file.exists());
        edm0.copy(sm0);
        assertTrue(file.exists());

        Motable sm1 = new SimpleMotable();
        sm1.copy(edm0);
        assertTrue(file.exists());

        edm0.destroy();
        assertTrue(!file.exists());

        assertEquals(name, sm1.getName());
        assertEquals(creationTime, sm1.getCreationTime());
        assertEquals(lastAccessedTime, sm1.getLastAccessedTime());
        assertEquals(maxInactiveInterval, sm1.getMaxInactiveInterval());
        byte[] actualBytes = sm1.getBodyAsByteArray();
        assertEquals(bytes.length, actualBytes.length);
    }

    public void testLoad() throws Exception {
        Store store = new DiscStore(new SimpleStreamer(), BASE_DIR, false);
        final Motable sm0 = new SimpleMotable();
        long creationTime = System.currentTimeMillis();
        long lastAccessedTime = creationTime + 1;
        int maxInactiveInterval = 30 * 60;
        String name = "foo";
        byte[] bytes = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f' };
        sm0.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        sm0.setBodyAsByteArray(bytes);
        
        File file = new File(new File(BASE_DIR, DiscStore.SESSION_STORE_DIR_NAME), name + ".ser");
        file.delete();
        
        Motable edm0 = store.create();
        edm0.copy(sm0);
        
        Putter putter = (Putter) mock(Putter.class);
        putter.put(name, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                Motable motable = (Motable) arg0;
                assertEquals(sm0.getCreationTime(), motable.getCreationTime()); 
                assertEquals(sm0.getLastAccessedTime(), motable.getLastAccessedTime()); 
                assertEquals(sm0.getMaxInactiveInterval(), motable.getMaxInactiveInterval()); 
                return true;
            }
        });
        
        startVerification();
        
        store.load(putter);
    }
    
}
