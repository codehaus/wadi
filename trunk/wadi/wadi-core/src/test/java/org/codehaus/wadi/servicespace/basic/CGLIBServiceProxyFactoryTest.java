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
package org.codehaus.wadi.servicespace.basic;

import java.io.IOException;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.servicespace.InvocationInfo;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceProxy;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: $
 */
public class CGLIBServiceProxyFactoryTest extends RMockTestCase {

    private ServiceName name;
    private Class[] interfaces;
    private CGLIBServiceProxyFactory proxyFactory;
    private ServiceInvoker serviceInvoker;

    protected void setUp() throws Exception {
        name = new ServiceName("name");
        serviceInvoker = (ServiceInvoker) mock(ServiceInvoker.class);
        interfaces = new Class[] {ServiceInterface1.class, ServiceInterface2.class};
    }
    
    public void testProxyInterfaces() {
        startVerification();
        
        proxyFactory = new CGLIBServiceProxyFactory(name, interfaces, serviceInvoker);
        Object proxy = proxyFactory.getProxy();
        assertTrue(proxy instanceof ServiceInterface1);
        assertTrue(proxy instanceof ServiceInterface2);
        assertTrue(proxy instanceof ServiceProxy);
    }
    
    public void testServiceProxy() {
        startVerification();
        
        proxyFactory = new CGLIBServiceProxyFactory(name, interfaces, serviceInvoker);
        ServiceProxy serviceProxy = (ServiceProxy) proxyFactory.getProxy();
        assertSame(interfaces, serviceProxy.getInterfaces());
        assertSame(name, serviceProxy.getTargetServiceName());
    }

    public void testProxyInvocation() {
        InvocationResultCombiner resultCombiner = (InvocationResultCombiner) mock(InvocationResultCombiner.class);
        VMPeer peer1 = new VMPeer("peer1");
        VMPeer peer2 = new VMPeer("peer2");
        
        proxyFactory = new CGLIBServiceProxyFactory(name, interfaces, serviceInvoker);
        final InvocationMetaData invocationMetaData = proxyFactory.getInvocationMetaData();
        invocationMetaData.setOneWay(true);
        invocationMetaData.setInvocationResultCombiner(resultCombiner);
        invocationMetaData.setTargets(new Peer[] {peer1, peer2});
        invocationMetaData.setTimeout(3000);
        
        serviceInvoker.invoke(null);
        modify().args(new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                InvocationInfo invocationInfo = (InvocationInfo) object;
                assertEquals("sayHello", invocationInfo.getMethodName());
                assertEquals(1, invocationInfo.getParams().length);
                assertEquals("Hello", invocationInfo.getParams()[0]);
                assertEquals(1, invocationInfo.getParamTypes().length);
                assertEquals(String.class, invocationInfo.getParamTypes()[0]);
                InvocationMetaData metaData = invocationInfo.getMetaData();
                assertEquals(invocationMetaData.isOneWay(), metaData.isOneWay());
                assertSame(invocationMetaData.getInvocationResultCombiner(), metaData.getInvocationResultCombiner());
                assertEquals(invocationMetaData.getTargets(), metaData.getTargets());
                for (int i = 0; i < metaData.getTargets().length; i++) {
                    assertSame(invocationMetaData.getTargets()[i], metaData.getTargets()[i]);
                }
                assertEquals(invocationMetaData.getTimeout(), metaData.getTimeout());
                return true;
            }
            
        });
        
        startVerification();
        
        Object proxy = proxyFactory.getProxy();
        
        ServiceInterface1 interface1 = (ServiceInterface1) proxy;
        interface1.sayHello("Hello");
    }

    public void testOneWayInvocation() {
        proxyFactory = new CGLIBServiceProxyFactory(name, interfaces, serviceInvoker);
        InvocationMetaData invocationMetaData = proxyFactory.getInvocationMetaData();
        invocationMetaData.setOneWay(true);
        
        serviceInvoker.invoke(null);
        modify().args(is.NOT_NULL);
        modify().returnValue(new InvocationResult("returnedValue"));
        
        startVerification();
        
        Object proxy = proxyFactory.getProxy();
        
        ServiceInterface1 interface1 = (ServiceInterface1) proxy;
        String returnedValue = interface1.sayHello("Hello");
        assertNull(returnedValue);
    }

    public void testRequestReplyInvocation() {
        proxyFactory = new CGLIBServiceProxyFactory(name, interfaces, serviceInvoker);
        
        serviceInvoker.invoke(null);
        modify().args(is.NOT_NULL);
        InvocationResult invocationResult = new InvocationResult("returnedValue");
        modify().returnValue(invocationResult);
        
        startVerification();
        
        Object proxy = proxyFactory.getProxy();
        
        ServiceInterface1 interface1 = (ServiceInterface1) proxy;
        String returnedValue = interface1.sayHello("Hello");
        assertEquals(invocationResult.getResult(), returnedValue);
    }


    public void testRequestReplyExceptionInvocation() {
        proxyFactory = new CGLIBServiceProxyFactory(name, interfaces, serviceInvoker);
        
        serviceInvoker.invoke(null);
        modify().args(is.NOT_NULL);
        Exception exception = new Exception();
        InvocationResult invocationResult = new InvocationResult(exception);
        modify().returnValue(invocationResult);
        
        startVerification();
        
        Object proxy = proxyFactory.getProxy();
        
        ServiceInterface1 interface1 = (ServiceInterface1) proxy;
        try {
            interface1.sayHello("Hello");
            fail();
        } catch (Exception e) {
            assertSame(invocationResult.getThrowable(), e);
        }
    }

    public interface ServiceInterface1 {
        String sayHello(String arg);
    }
    
    public interface ServiceInterface2 {
        String sayBye();
    }

}
