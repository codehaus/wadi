/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.codehaus.wadi.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.EvictionPolicy;
import org.codehaus.wadi.shared.HttpSessionImpl;

public class
TotalEvictionPolicy
implements EvictionPolicy
 {
	protected final Log _log=LogFactory.getLog(getClass());
    public boolean evictable(long currentTimeMillis, HttpSessionImpl impl){return true;}
 }
