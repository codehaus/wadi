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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.MessageExchangeException;


/**
 * 
 * @version $Revision: 1538 $
 */
public class InvocationInfo implements Externalizable {
    private static final Log log = LogFactory.getLog(InvocationInfo.class);

    private Class targetClass;
    private int memberUpdaterIndex;
    private Object[] params;
    private InvocationMetaData metaData;

    public InvocationInfo() {
    }
    
    public InvocationInfo(Class targetClass, int memberUpdaterIndex, Object[] params, InvocationMetaData metaData) {
        if (null == targetClass) {
            throw new IllegalArgumentException("targetClass is required");
        } else if (null == params) {
            throw new IllegalArgumentException("params is required");
        } else if (null == metaData) {
            throw new IllegalArgumentException("metaData is required");
        }
        this.targetClass = targetClass;
        this.memberUpdaterIndex = memberUpdaterIndex;
        this.params = params;
        this.metaData = metaData;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public int getMemberUpdaterIndex() {
        return memberUpdaterIndex;
    }

    public Object[] getParams() {
        return params;
    }

    public InvocationMetaData getMetaData() {
        return metaData;
    }

    public void handleOneWayException(MessageExchangeException e) throws MessageExchangeException, ServiceProxyException {
        if (metaData.isOneWay()) {
            if (metaData.isIgnoreMessageExchangeExceptionOnSend()) {
                log.debug("Explicitly ignoring MessageExchangeException", e);
            } else {
                throw new ServiceProxyException("Cannot send [" + this + "]", e);
            }
        } else {
            throw e;
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        targetClass = (Class) in.readObject();
        memberUpdaterIndex = in.readInt();
        params = (Object[]) in.readObject();
        metaData = (InvocationMetaData) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(targetClass);
        out.writeInt(memberUpdaterIndex);
        out.writeObject(params);
        out.writeObject(metaData);
    }

    public String toString() {
        return "InvocationInfo Class [" + targetClass + "]. index [" + memberUpdaterIndex
        + "]; parameters [" + params + "]; metaData [" + metaData + "]";
    }
    
}
