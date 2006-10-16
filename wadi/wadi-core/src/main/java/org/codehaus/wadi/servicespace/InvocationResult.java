package org.codehaus.wadi.servicespace;

import java.io.Serializable;

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
