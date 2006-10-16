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
package org.codehaus.wadi.servicespace;

import java.io.Serializable;

/**
 * 
 * @version $Revision: 1538 $
 */
public class InvocationResult implements Serializable {
    private final boolean success;
    private final Object result;
    private final Throwable throwable;

    public InvocationResult(Object result) {
        this.result = result;
        
        success = true;
        throwable = null;
    }

    public InvocationResult(Throwable throwable) {
        this.throwable = throwable;
        
        success = false;
        result = null;
    }

    public Object getResult() {
        if (!success) {
            throw new IllegalStateException("No result as it is a result failure");
        }
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public Throwable getThrowable() {
        if (success) {
            throw new IllegalStateException("No throwable as it is a result success");
        }
        return throwable;
    }
    
}
