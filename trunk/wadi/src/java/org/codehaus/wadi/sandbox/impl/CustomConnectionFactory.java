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
package org.codehaus.wadi.sandbox.impl;

import javax.jms.JMSException;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.XmlConfigHelper;
import org.activemq.broker.BrokerContainerFactory;

public class CustomConnectionFactory extends ActiveMQConnectionFactory {

  public CustomConnectionFactory(String brokerURL) {
    super(brokerURL);
    super.setBrokerContainerFactory(new CustomBrokerContainerFactory());
    System.err.println("USING CUSTOM CONNECTION");
  }

  public BrokerContainerFactory getBrokerContainerFactory() throws JMSException {
    System.err.println("CUSTOM BROKERCONTAINER OVERRIDEN");
    return super.getBrokerContainerFactory();
  }

  public void setBrokerContainerFactory(BrokerContainerFactory brokerContainerFactory) {
    System.err.println("CUSTOM BROKERCONTAINER OVERRIDEN");
    super.setBrokerContainerFactory(brokerContainerFactory);
  }

  protected BrokerContainerFactory createBrokerContainerFactory() throws JMSException {
    System.err.println("CUSTOM BROKERCONTAINER OVERRIDEN");
    String brokerXmlConfig=getBrokerXmlConfig();
    if (brokerXmlConfig!=null) {
      return XmlConfigHelper.createBrokerContainerFactory(brokerXmlConfig);
    }
    return new CustomBrokerContainerFactory();
  }
}
