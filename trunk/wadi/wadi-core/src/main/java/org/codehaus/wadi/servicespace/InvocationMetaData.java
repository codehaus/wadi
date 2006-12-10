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

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.replyaccessor.TwoWay;
import org.codehaus.wadi.servicespace.resultcombiner.ReturnFirstSuccessThenFailure;


/**
 * 
 * @version $Revision: 1538 $
 */
public class InvocationMetaData implements Serializable {
    private static final long DEFAULT_TIMEOUT = 500;
    
    private transient Peer[] targets;
    private transient long timeout = DEFAULT_TIMEOUT;
    private boolean oneWay;
    private boolean ignoreMessageExchangeExceptionOnSend;
    private ReplyRequiredAssessor replyAssessor = TwoWay.ASSESSOR;
    private transient InvocationResultCombiner invocationResultCombiner = ReturnFirstSuccessThenFailure.COMBINER;

    public InvocationMetaData() {
    }
    
    public InvocationMetaData(InvocationMetaData prototype) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        }
        targets = prototype.targets;
        timeout = prototype.timeout;
        replyAssessor = prototype.replyAssessor;
        oneWay = prototype.oneWay;
        invocationResultCombiner = prototype.invocationResultCombiner;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        this.timeout = timeout;
    }

    public InvocationResultCombiner getInvocationResultCombiner() {
        return invocationResultCombiner;
    }

    public void setInvocationResultCombiner(InvocationResultCombiner invocationResultCombiner) {
        if (null == invocationResultCombiner) {
            throw new IllegalArgumentException("invocationResultCombiner is required");
        }
        this.invocationResultCombiner = invocationResultCombiner;
    }

    public Peer[] getTargets() {
        if (null == targets) {
            throw new IllegalStateException("Cluster is targeted");
        }
        return targets;
    }

    public void setTargets(Peer[] targets) {
        if (null != targets && targets.length == 0) {
            throw new IllegalArgumentException("targets is empty");
        }
        this.targets = targets;
    }
    
    public boolean isClusterTargeted() {
        return null == targets;
    }

    public ReplyRequiredAssessor getReplyAssessor() {
        return replyAssessor;
    }

    public void setReplyAssessor(ReplyRequiredAssessor replyAssessor) {
        if (null == replyAssessor) {
            throw new IllegalArgumentException("replyAssessor is required");
        }
        this.replyAssessor = replyAssessor;
    }

    public boolean isIgnoreMessageExchangeExceptionOnSend() {
        return ignoreMessageExchangeExceptionOnSend;
    }

    public void setIgnoreMessageExchangeExceptionOnSend(boolean ignoreMessageExchangeExceptionOnSend) {
        this.ignoreMessageExchangeExceptionOnSend = ignoreMessageExchangeExceptionOnSend;
    }

}
