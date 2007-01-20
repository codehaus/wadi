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
package org.codehaus.wadi.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.web.WebSessionFactory;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SpringManagerFactory {

    protected final static Log _log = LogFactory.getLog(SpringManagerFactory.class);

    protected final InputStream _descriptor;
    protected final String _beanName;
    protected final WebSessionFactory _sessionFactory;
    protected final WebSessionWrapperFactory _sessionWrapperFactory;

    public SpringManagerFactory(InputStream descriptor, String beanName, WebSessionFactory sessionFactory, WebSessionWrapperFactory sessionWrapperFactory) {
        _descriptor=descriptor;
        _beanName=beanName;
        _sessionFactory=sessionFactory;
        _sessionWrapperFactory=sessionWrapperFactory;
    }

    public Manager create() throws FileNotFoundException {
        return create(_descriptor, _beanName);
    }

    public static Manager create(InputStream descriptor, String beanName) throws FileNotFoundException {
    	DefaultListableBeanFactory dlbf=new DefaultListableBeanFactory();
        String wadiPropsName = System.getProperty("wadi.properties");
        FileSystemResource props = null;
        if ((wadiPropsName!=null) && (!"".equals(wadiPropsName.trim()))){
            props = new FileSystemResource(new File(wadiPropsName.trim()));
        }
    	PropertyPlaceholderConfigurer cfg=new PropertyPlaceholderConfigurer();
        if ((props != null) && props.exists())
                cfg.setLocation(props);
        else
            _log.info("properties file "+wadiPropsName+" does not exist");

        _log.info("java.io.tmpdir="+System.getProperty("java.io.tmpdir"));

        XmlBeanDefinitionReader xbdr=new XmlBeanDefinitionReader(dlbf);
        xbdr.setBeanClassLoader(SpringManagerFactory.class.getClassLoader());
        xbdr.loadBeanDefinitions(new InputStreamResource(descriptor));
    	cfg.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
    	cfg.postProcessBeanFactory(dlbf);

    	//dlbf.getBean("exporter");
    	//dlbf.getBean("serverConnector");
    	dlbf.preInstantiateSingletons();
    	Manager manager=(Manager)dlbf.getBean(beanName);

    	if (manager==null)
    		if (_log.isErrorEnabled()) _log.error("could not find WADI Manager bean: "+beanName);
    		else
    			if (_log.isInfoEnabled()) _log.info("loaded bean: "+beanName+" from WADI descriptor: "+descriptor);

    	return manager;
    }

}
