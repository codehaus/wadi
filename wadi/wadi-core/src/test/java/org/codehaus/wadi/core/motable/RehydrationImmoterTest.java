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
package org.codehaus.wadi.core.motable;

import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.RehydrationImmoter;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class RehydrationImmoterTest extends RMockTestCase {

    public void testNewMotable() throws Exception {
        Immoter delegate = (Immoter) mock(Immoter.class);
        Motable motable = (Motable) mock(Motable.class);
        
        Motable newMotable = delegate.newMotable();
        
        motable.getCreationTime();
        int creationTime = 1;
        modify().returnValue(creationTime);
        motable.getLastAccessedTime();
        int lastAccessedTime = 2;
        modify().returnValue(lastAccessedTime);
        motable.getMaxInactiveInterval();
        int maxInactiveInterval = 3;
        modify().returnValue(maxInactiveInterval);
        motable.getName();
        String name = "name";
        modify().returnValue(name);
        motable.getBodyAsByteArray();
        byte[] bodyAsByteArray = new byte[0];
        modify().returnValue(bodyAsByteArray);
        
        newMotable.rehydrate(creationTime, lastAccessedTime, maxInactiveInterval, name, bodyAsByteArray);
        
        startVerification();
        
        RehydrationImmoter immoter = new RehydrationImmoter(delegate, motable);
        immoter.newMotable();
    }
    
}
