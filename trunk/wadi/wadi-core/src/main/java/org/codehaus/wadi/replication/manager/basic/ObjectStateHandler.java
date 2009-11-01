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
package org.codehaus.wadi.replication.manager.basic;

import org.codehaus.wadi.core.motable.Motable;


/**
 * 
 * @version $Revision: 2340 $
 */
public interface ObjectStateHandler {
    byte[] extractFullState(Object key, Motable target);
    
    byte[] extractUpdatedState(Object key, Motable target);
    
    void resetObjectState(Motable target);
    
    Motable restoreFromFullState(Object key, Motable motable);

    Motable restoreFromFullState(Object key, byte[] state);
    
    Motable restoreFromUpdatedState(Object key, byte[] state);
    
    Motable restoreFromFullStateTransient(Object key, byte[] state);
    
    void setObjectFactory(Object factory);

    void discardState(Object key, Motable payload);

    void initState(Object key, Motable payload);

}