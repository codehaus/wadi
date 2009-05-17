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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.LazyLoader;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.reflect.FastClass;

import org.codehaus.wadi.core.reflect.ClassIndexer;
import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.servicespace.InvocationInfo;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.ServiceInvocationException;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;

/**
 * 
 * @version $Revision: $
 */
public class CGLIBServiceProxyFactory implements ServiceProxyFactory {

    private final ServiceName targetServiceName;
    private final Class[] interfaces;
    private final ServiceInvoker invoker;
    private final Class proxyType;
    private final FastClass fastClass;
    private final InvocationMetaData metaData = new InvocationMetaData();
    private final Map<Class, ClassIndexer> classToClassIndexer;

    public CGLIBServiceProxyFactory(ServiceName targetServiceName,
            ClassIndexerRegistry indexerRegistry,
            Class[] interfaces,
            ServiceInvoker invoker) {
        if (null == targetServiceName) {
            throw new IllegalArgumentException("targetServiceName is required");
        } else if (null == interfaces || isNotInterfaces(interfaces)) {
            throw new IllegalArgumentException("interfaces is required");
        } else if (null == invoker) {
            throw new IllegalArgumentException("invoker is required");
        }
        this.targetServiceName = targetServiceName;
        this.interfaces = interfaces;
        this.invoker = invoker;
        
        classToClassIndexer = new HashMap<Class, ClassIndexer>();
        for (int i = 0; i < interfaces.length; i++) {
            Class interfaceClazz = interfaces[i];
            ClassIndexer classIndexer = indexerRegistry.index(interfaceClazz);
            classToClassIndexer.put(interfaceClazz, classIndexer);
        }
        
        proxyType = createProxyType(interfaces);
        fastClass = FastClass.create(proxyType);
    }

    public ServiceName getTargetServiceName() {
        return targetServiceName;
    }

    public Class[] getInterfaces() {
        return interfaces;
    }
    
    public InvocationMetaData getInvocationMetaData() {
        return metaData;
    }

    public ServiceProxy getProxy() {
        CGLIBServiceProxy serviceProxy = new CGLIBServiceProxy(new InvocationMetaData(metaData), invoker);
        Callback callback = new ProxyMethodInterceptor(serviceProxy);
        try {
            Factory proxy = (Factory) fastClass.newInstance();
            proxy.setCallbacks(new Callback[] {callback, serviceProxy});
            return (ServiceProxy) proxy;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
    
    protected Class createProxyType(Class[] interfaces) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Object.class);
        Class[] newInterfaces = new Class[interfaces.length + 1];
        System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
        newInterfaces[newInterfaces.length - 1] = ServiceProxy.class;
        enhancer.setInterfaces(newInterfaces);
        enhancer.setCallbackFilter(new CallbackFilter() {
            public int accept(Method method) {
                if (method.getDeclaringClass() == ServiceProxy.class) {
                    return 1;
                } else if (method.getDeclaringClass() == Object.class) {
                    return 1;
                }
                return 0;
            }
        });
        enhancer.setCallbackTypes(new Class[] {MethodInterceptor.class, LazyLoader.class});
        return enhancer.createClass();
    }

    private static boolean isNotInterfaces(Class[] interfaces) {
        for (int i = 0; i < interfaces.length; i++) {
            if (!interfaces[i].isInterface()) {
                throw new IllegalArgumentException("[" + interfaces[i] + "] is not an interface");
            }
        }
        return false;
    }

    private class CGLIBServiceProxy implements ServiceProxy, LazyLoader  {
        private final InvocationMetaData metaData;
        private final ServiceInvoker serviceInvoker;

        public CGLIBServiceProxy(InvocationMetaData metaData, ServiceInvoker serviceInvoker) {
            this.metaData = metaData;
            this.serviceInvoker = serviceInvoker;
        }

        public Class[] getInterfaces() {
            return interfaces;
        }

        public InvocationMetaData getInvocationMetaData() {
            return metaData;
        }

        public ServiceName getTargetServiceName() {
            return targetServiceName;
        }

        public ServiceInvoker getServiceInvoker() {
            return serviceInvoker;
        }

        public Object loadObject() throws Exception {
            return this;
        }
        
    }
    
    private class ProxyMethodInterceptor implements MethodInterceptor {
        private final ServiceProxy serviceProxy;
        private final ServiceInvoker serviceInvoker;
        
        public ProxyMethodInterceptor(ServiceProxy serviceProxy) {
            this.serviceProxy = serviceProxy;
            serviceInvoker = serviceProxy.getServiceInvoker();
        }

        public Object intercept(Object object, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            Class targetClass = method.getDeclaringClass();
            ClassIndexer classIndexer = classToClassIndexer.get(targetClass);
            if (null == classIndexer) {
                throw new AssertionError("Cannot find ClassIndexer for [" + method + "]");
            }
            int memberUpdaterIndex = classIndexer.getIndex(method);

            InvocationMetaData invocationMetaData = serviceProxy.getInvocationMetaData();
            InvocationInfo invocationInfo = new InvocationInfo(targetClass, memberUpdaterIndex, args, invocationMetaData);
            InvocationResult result = serviceInvoker.invoke(invocationInfo);
            if (invocationMetaData.isOneWay()) {
                return null;
            }
            if (result.isSuccess()) {
                return result.getResult();
            } else {
                Throwable throwable = result.getThrowable();
                Class throwableType = throwable.getClass();
                Class[] exceptionTypes = method.getExceptionTypes();
                for (int i = 0; i < exceptionTypes.length; i++) {
                    Class exceptionType = exceptionTypes[i];
                    if (exceptionType.isAssignableFrom(throwableType)) {
                        throw throwable;
                    }
                }
                throw new ServiceInvocationException(throwable);
            }
        }
        
    }

}
