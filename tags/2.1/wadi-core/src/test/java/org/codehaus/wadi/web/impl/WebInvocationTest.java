package org.codehaus.wadi.web.impl;

import javax.servlet.FilterChain;

import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.web.HttpInvocationContext;

import com.agical.rmock.extension.junit.RMockTestCase;

public class WebInvocationTest extends RMockTestCase {

    private FilterChain chain;
    private Session session;
    private WebInvocation invocation;
    private HttpInvocationContext invocationContext;

    @Override
    protected void setUp() throws Exception {
        chain = (FilterChain) mock(FilterChain.class);
        session = (Session) mock(Session.class);
        invocationContext = (HttpInvocationContext) mock(HttpInvocationContext.class);
        
        invocation = new WebInvocation();
        invocation.init(null, null, chain);
        invocation.setSession(session);
    }
    
    public void testExecuteOnEndProcessing() throws Exception {
        chain.doFilter(null, null);
        session.onEndProcessing();
        
        chain.doFilter(invocationContext, null);
        session.onEndProcessing();
        startVerification();
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    
    public void testDoNotExecuteOnEndProcessing() throws Exception {
        chain.doFilter(null, null);
        chain.doFilter(invocationContext, null);
        startVerification();
        
        invocation.setDoNotExecuteOnEndProcessing(true);
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    
    
}
