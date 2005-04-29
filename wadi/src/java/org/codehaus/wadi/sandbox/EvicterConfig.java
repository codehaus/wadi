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
package org.codehaus.wadi.sandbox;

import java.util.Map;
import java.util.Timer;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public interface EvicterConfig {

    Timer getTimer();
    
    // BestEffortEvicters
    Map getMap();
    Sync getEvictionLock(String id, Motable motable);
    void expire(Motable motable);
    void demote(Motable motable);
    Emoter getEvictionEmoter();

    // StrictEvicters
    int getMaxInactiveInterval();
    
}
