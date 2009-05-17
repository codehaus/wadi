/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.EndPoint;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInvocation implements Invocation {

    protected final String sessionKey;
    protected Session session;
    private boolean errorIfSessionNotAcquired;
    private boolean errored;
    private int errorCode;
    private String errorMessage;
    protected long exclusiveSessionLockWaitTime;
    private boolean doNotExecuteOnEndProcessing;
    private boolean acquireLockOnInvocationStart;
    private boolean releaseLockOnInvocationEnd;
    private boolean withExclusiveLock;
    
    public BasicInvocation(String sessionKey, long exclusiveSessionLockWaitTime) {
        this.sessionKey = sessionKey;
        this.exclusiveSessionLockWaitTime = exclusiveSessionLockWaitTime;
        
        acquireLockOnInvocationStart = true;
        releaseLockOnInvocationEnd = true;
    }
    
    public Session getSession() {
        return session;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void invoke() throws InvocationException {
        try {
            doInvoke();
        } catch (Exception e) {
            if (e instanceof InvocationException) {
                throw (InvocationException) e;
            }
            throw new InvocationException(e);
        } finally {
            if (!doNotExecuteOnEndProcessing && null != session) {
                session.onEndProcessing();
            }
        }
    }

    public void invoke(InvocationContext context) throws InvocationException {
        try {
            doInvoke(context);
        } catch (Exception e) {
            if (e instanceof InvocationException) {
                throw (InvocationException) e;
            }
            throw new InvocationException(e);
        } finally {
            if (!doNotExecuteOnEndProcessing && null != session) {
                session.onEndProcessing();
            }
        }
    }

    protected void doInvoke() throws InvocationException {
    }
    
    protected void doInvoke(InvocationContext context) throws InvocationException {
    }

    public boolean isProxiedInvocation() {
        return false;
    }

    public boolean isRelocatable() {
        return false;
    }

    public void relocate(EndPoint endPoint) throws InvocationException {
        throw new UnsupportedOperationException();
    }

    public void sendError(int code, String message) throws InvocationException {
        errored = true;
        this.errorCode = code;
        this.errorMessage = message;
    }

    public void setInvocationProxy(InvocationProxy proxy) {
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public boolean isErrorIfSessionNotAcquired() {
        return errorIfSessionNotAcquired;
    }

    public void setErrorIfSessionNotAcquired(boolean errorIfSessionNotAcquired) {
        this.errorIfSessionNotAcquired = errorIfSessionNotAcquired;
    }

    public boolean isErrored() {
        return errored;
    }
    
    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getExclusiveSessionLockWaitTime() {
        return exclusiveSessionLockWaitTime;
    }

    public boolean isDoNotExecuteOnEndProcessing() {
        return doNotExecuteOnEndProcessing;
    }

    public void setDoNotExecuteOnEndProcessing(boolean doNotExecuteOnEndProcessing) {
        this.doNotExecuteOnEndProcessing = doNotExecuteOnEndProcessing;
    }

    public InvocationContext newContext(Session context) {
        return new BasicInvocationContext(this);
    }

    public boolean isWithExclusiveLock() {
        return withExclusiveLock;
    }

    public void setWithExclusiveLock(boolean withExclusiveLock) {
        this.withExclusiveLock = withExclusiveLock;
    }

    public void setAcquireLockOnInvocationStart(boolean acquireLockOnInvocationStart) {
        this.acquireLockOnInvocationStart = acquireLockOnInvocationStart;
    }

    public boolean isAcquireLockOnInvocationStart() {
        return acquireLockOnInvocationStart;
    }

    public void setReleaseLockOnInvocationEnd(boolean releaseLockOnInvocationEnd) {
        this.releaseLockOnInvocationEnd = releaseLockOnInvocationEnd;
    }

    public boolean isReleaseLockOnInvocationEnd() {
        return releaseLockOnInvocationEnd;
    }

}
