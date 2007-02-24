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

import junit.framework.TestCase;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.core.store.DiscStore;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.store.StoreMotable;
import org.codehaus.wadi.impl.SimpleStreamer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestDiscStore extends TestCase {

    public void testDiscMotables() throws Exception {
        testDiscMotables(new DiscStore(new SimpleStreamer(), new File("/tmp"), false, false));
        testDiscMotables(new DiscStore(new SimpleStreamer(), new File("/tmp"), true, false));
    }

    public void testDiscMotables(Store store) throws Exception {
        Motable sm0 = new SimpleMotable();
        long creationTime = System.currentTimeMillis();
        long lastAccessedTime = creationTime + 1;
        int maxInactiveInterval = 30 * 60;
        String name = "foo";
        byte[] bytes = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f' };
        sm0.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        sm0.setBodyAsByteArray(bytes);

        File file = new File(new File("/tmp"), name + ".ser");
        file.delete();
        assertFalse(file.exists());

        StoreMotable edm0 = store.create();
        edm0.init(store);
        assertFalse(file.exists());
        edm0.copy(sm0);
        assertTrue(file.exists());

        StoreMotable edm1 = store.create();
        edm1.init(store, name);
        assertTrue(file.exists());

        Motable sm1 = new SimpleMotable();
        sm1.copy(edm1);
        assertTrue(file.exists());

        edm1.destroy();
        assertTrue(!file.exists());

        assertTrue(sm0.equals(sm1));
    }

}