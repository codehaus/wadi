package org.codehaus.wadi.servicespace;

import java.util.Collection;


public interface InvocationResultCombiner {
    InvocationResult combine(Collection invocationResults);
}
