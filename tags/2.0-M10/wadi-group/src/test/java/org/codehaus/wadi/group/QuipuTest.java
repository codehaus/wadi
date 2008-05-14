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
package org.codehaus.wadi.group;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class QuipuTest extends TestCase {

    public void testPutException() throws Exception {
        final Quipu quipu = new Quipu(2, "id");
        
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                quipu.putException(new Exception());
            }
        }.run();
        
        try {
            quipu.waitFor(200);
            fail();
        }  catch (QuipuException e) {
        }
    }
    
    public void testPutResult() throws Exception {
        final Quipu quipu = new Quipu(1, "id");
        final Object expectedResult = new Object();

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                quipu.putResult(expectedResult);
            }
        }.run();
        
        boolean success = quipu.waitFor(200);
        assertTrue(success);
        Collection results = quipu.getResults();
        assertEquals(1, results.size());
        assertTrue(results.contains(expectedResult));
    }
    
}
