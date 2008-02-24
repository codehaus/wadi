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
package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.location.statemanager.StateManager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * @version $Revision: 1563 $
 */
public class DummyContextualiserTest extends RMockTestCase {

    public void testImmotionNotifyStateManager() throws Exception {
        StateManager stateManager = (StateManager) mock(StateManager.class);
        Motable emotable = (Motable) mock(Motable.class);
        emotable.getName();
        String name = "name";
        modify().returnValue(name);
        
        stateManager.remove(name);
        startVerification();
        
        DummyContextualiser contextualiser = new DummyContextualiser(stateManager);
        
        Immoter immoter = contextualiser.getDemoter(name, emotable);
        Motable newMotable = immoter.newMotable(emotable);
        immoter.immote(emotable, newMotable);
    }
    
}
