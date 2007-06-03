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
package org.codehaus.wadi.location.partitionmanager.local;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.codehaus.wadi.group.Dispatcher;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractLocalPartitionAction {
    protected final Dispatcher dispatcher;
    protected final Map nameToLocation;
    protected final Log log;
    
    protected AbstractLocalPartitionAction(Dispatcher dispatcher, Map nameToLocation, Log log) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == nameToLocation) {
            throw new IllegalArgumentException("nameToLocation is required");
        } else if (null == log) {
            throw new IllegalArgumentException("log is required");
        }
        this.dispatcher = dispatcher;
        this.nameToLocation = nameToLocation;
        this.log = log;
    }

}
