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
package org.codehaus.wadi.replication.strategy;

import org.codehaus.wadi.servicespace.ServiceSpace;



/**
 * 
 * @version $Revision$
 */
public class RoundRobinBackingStrategyFactory implements BackingStrategyFactory {
    private final int nbReplica;
    private ServiceSpace serviceSpace;
    
    public RoundRobinBackingStrategyFactory(int nbReplica) {
        if (nbReplica < 1) {
            throw new IllegalArgumentException("nbReplica must be greater than 0");
        }
        this.nbReplica = nbReplica;
    }

    public void setServiceSpace(ServiceSpace serviceSpace) {
        this.serviceSpace = serviceSpace;
    }
    
    public BackingStrategy factory() {
        if (null == serviceSpace) {
            throw new IllegalStateException("serviceSpace is not set");
        }
        return new RoundRobinBackingStrategy(serviceSpace, nbReplica);
    }

}
