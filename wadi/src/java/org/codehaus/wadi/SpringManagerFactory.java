/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.impl.jetty.JettySessionWrapperFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;

public class SpringManagerFactory {

    protected final static Log _log = LogFactory.getLog(SpringManagerFactory.class);

    protected final String _descriptor;
    protected final String _bean;
    protected final String _sessionFactoryClass;
    protected final String _sessionWrapperFactoryClass;
    protected final String _sessionManagerClass;

    public SpringManagerFactory(String descriptor, String bean, String sessionFactoryClass, String sessionWrapperFactoryClass, String sessionManagerClass) {
        _descriptor=descriptor;
        _bean=bean;
        _sessionFactoryClass=sessionFactoryClass;
        _sessionWrapperFactoryClass=sessionWrapperFactoryClass;
        _sessionManagerClass=sessionManagerClass;
    }

    public StandardManager create() throws FileNotFoundException {
        return create(_descriptor, _bean, _sessionFactoryClass, _sessionWrapperFactoryClass, _sessionManagerClass);
    }

    
    public static class SessionManagerFactory {
    	
    	protected Class _clazz;
    	
    	public SessionManagerFactory(Class clazz) {
    		_clazz=clazz;
    	}

        public Object create(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, Streamer streamer, boolean accessOnLoad, String clusterUri, String clusterName, String nodeName, HttpProxy httpProxy, InetSocketAddress httpAddress, int numBuckets) throws Exception {
    		Class[] meta=new Class[] {
    				SessionPool.class,
    				AttributesFactory.class,
    				ValuePool.class,
    				SessionWrapperFactory.class,
    				SessionIdFactory.class,
    				Contextualiser.class,
    				Map.class,
    				Router.class,
    				Streamer.class,
    				Boolean.TYPE,
    				String.class,
    				String.class,
    				String.class,
    				HttpProxy.class,
    				InetSocketAddress.class,
    				Integer.TYPE
    		};
    		Object[] data=new Object[] {
    				sessionPool,
    				attributesFactory,
    				valuePool,
    				sessionWrapperFactory,
    				sessionIdFactory,
    				contextualiser,
    				sessionMap,
    				router,
    				streamer,
    				accessOnLoad?Boolean.TRUE:Boolean.FALSE,
    				clusterUri,
    				clusterName,
    				nodeName,
    				httpProxy,
    				httpAddress,
    				new Integer(numBuckets)
    		};
    		return _clazz.getConstructor(meta).newInstance(data);
    	}
    	
    }
    
    public static StandardManager create(String descriptor, String bean, String sessionFactoryClass, String sessionWrapperFactoryClass, String sessionManagerClass) throws FileNotFoundException {

    	//ClassLoader cl=SpringManagerFactory.class.getClassLoader();
        ClassLoader cl=Thread.currentThread().getContextClassLoader();
        if (_log.isTraceEnabled()) _log.trace("Manager ClassLoader: "+cl);

        //InputStream is=cl.getResourceAsStream(descriptor);
        InputStream is=new FileInputStream(descriptor);
        if (is!=null) {
            DefaultListableBeanFactory dlbf=new DefaultListableBeanFactory();
            PropertyPlaceholderConfigurer cfg=new PropertyPlaceholderConfigurer();
            new XmlBeanDefinitionReader(dlbf).loadBeanDefinitions(new InputStreamResource(is));
            cfg.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
            cfg.postProcessBeanFactory(dlbf);

            try {
            	dlbf.registerSingleton("SessionFactory", Class.forName(sessionFactoryClass, true, cl).newInstance());
            	dlbf.registerSingleton("SessionWrapperFactory", Class.forName(sessionWrapperFactoryClass, true, cl).newInstance());
            	dlbf.registerSingleton("SessionManagerFactory", new SessionManagerFactory(Class.forName(sessionManagerClass, true, cl)));
            } catch (Exception e) {
            	_log.error("problem initialising component factories", e);
            }
            
    	    //dlbf.getBean("exporter");
    	    //dlbf.getBean("serverConnector");
    	    dlbf.preInstantiateSingletons();
            StandardManager manager=(StandardManager)dlbf.getBean(bean);

            if (manager==null)
                if (_log.isErrorEnabled()) _log.error("could not find WADI Manager bean: "+bean);
            else
	      if (_log.isInfoEnabled()) _log.info("loaded bean: "+bean+" from WADI descriptor: "+descriptor);

            return manager;
        } else {
            if (_log.isErrorEnabled())_log.error("could not find WADI descriptor: "+descriptor);
            return null;
        }
    }

}
