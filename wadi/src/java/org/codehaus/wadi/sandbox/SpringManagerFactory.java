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
package org.codehaus.wadi.sandbox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.impl.Manager;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;

public class SpringManagerFactory {
    
    protected final static Log _log = LogFactory.getLog(SpringManagerFactory.class);
    
    protected final String _descriptor;
    protected final String _bean;
    
    public SpringManagerFactory(String descriptor, String bean) {
        _descriptor=descriptor;
        _bean=bean;
    }

    public Manager create() throws FileNotFoundException {
        return create(_descriptor, _bean);
    }
    
    public static Manager create(String descriptor, String bean) throws FileNotFoundException {
        //ClassLoader cl=SpringManagerFactory.class.getClassLoader();
        ClassLoader cl=Thread.currentThread().getContextClassLoader();
        _log.info("Manager ClassLoader: "+cl);

        //InputStream is=cl.getResourceAsStream(descriptor);
        InputStream is=new FileInputStream(descriptor);
        if (is!=null) {
            DefaultListableBeanFactory dlbf=new DefaultListableBeanFactory();
            PropertyPlaceholderConfigurer cfg=new PropertyPlaceholderConfigurer();
            new XmlBeanDefinitionReader(dlbf).loadBeanDefinitions(new InputStreamResource(is));
            cfg.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
            cfg.postProcessBeanFactory(dlbf);

            Manager manager=(Manager)dlbf.getBean(bean);

            if (manager==null)
                _log.error("could not find WADI Manager bean: "+bean);
            else
                _log.info("loaded bean: "+bean+" from WADI descriptor: "+descriptor);
            
            return manager;
        } else {
            _log.error("could not find WADI descriptor: "+descriptor);
            return null;
        }
    }

}
