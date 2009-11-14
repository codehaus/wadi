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
package org.codehaus.wadi.servicespace.replyaccessor;

import java.io.ObjectStreamException;

import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.ReplyRequiredAssessor;

/**
 * 
 * @version $Revision: 1538 $
 */
public class DoNotReplyWithNull implements ReplyRequiredAssessor {
    public static final DoNotReplyWithNull ASSESSOR = new DoNotReplyWithNull();
    
    public boolean isReplyRequired(InvocationResult result) {
        if (result.isSuccess()) {
            return result.getResult() != null;
        }
        return true;
    }
    
    public Object readResolve() throws ObjectStreamException {
        return ASSESSOR;
    }
    
}
