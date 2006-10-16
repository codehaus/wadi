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
public class InvocationInfo implements Serializable {
    private static final long DEFAULT_TIMEOUT = 5000;
    
    private final String methodName;
    private final Class[] paramTypes;
    private final Object[] params;
    private final InvocationMetaData metaData;

    public InvocationInfo(String methodName, Class[] paramTypes, Object[] params, InvocationMetaData metaData) {
        if (null == methodName) {
            throw new IllegalArgumentException("methodName is required");
        } else if (null == paramTypes) {
            throw new IllegalArgumentException("paramTypes is required");
        } else if (null == params) {
            throw new IllegalArgumentException("params is required");
        } else if (null == metaData) {
            throw new IllegalArgumentException("metaData is required");
        }
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.params = params;
        this.metaData = metaData;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getParams() {
        return params;
    }

    public Class[] getParamTypes() {
        return paramTypes;
    }

    public InvocationMetaData getMetaData() {
        return metaData;
    }

}
