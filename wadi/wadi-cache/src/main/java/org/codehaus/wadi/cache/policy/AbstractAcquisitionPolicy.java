/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.policy;

import org.codehaus.wadi.cache.AcquisitionInfo;
import org.codehaus.wadi.cache.AcquisitionPolicy;

/**
 *
 * @version $Rev:$ $Date:$
 */
public abstract class AbstractAcquisitionPolicy  implements AcquisitionPolicy {
    protected final AcquisitionInfo acquisitionInfo; 
    
    public AbstractAcquisitionPolicy(AcquisitionInfo acquisitionInfo) {
        if (null == acquisitionInfo) {
            throw new IllegalArgumentException("acquisitionInfo is required");
        }
        this.acquisitionInfo = acquisitionInfo;
    }

    public AcquisitionInfo getAcquisitionInfo() {
        return acquisitionInfo;
    }
    
    public boolean isAcquireForOptimisticUpdate() {
        return false;
    }

    public boolean isAcquireForPessimisticUpdate() {
        return false;
    }

    public boolean isAcquireForReadOnly() {
        return false;
    }
    
    public AcquisitionPolicy toAcquisitionPolicy() {
        return this;
    }
    
}