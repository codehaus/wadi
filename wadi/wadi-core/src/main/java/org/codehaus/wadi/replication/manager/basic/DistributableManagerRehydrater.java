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
package org.codehaus.wadi.replication.manager.basic;

import org.codehaus.wadi.RehydrationException;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.impl.DistributableManager;

/**
 * 
 * @version $Revision: 1603 $
 */
public class DistributableManagerRehydrater implements SessionRehydrater {
    private DistributableManager manager;

    public DistributableManagerRehydrater() {
    }

    public DistributableManagerRehydrater(DistributableManager manager) {
        this.manager = manager;
    }
    
    public Session rehydrate(String key, byte[] body) throws RehydrationException {
        return manager.rehydrate(key, body);
    }

    // TODO - fix this dependency.
    public void setManager(DistributableManager manager) {
        this.manager = manager;
    }
}
