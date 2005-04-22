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

import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.broker.BrokerContext;
import org.activemq.broker.BrokerContainer;
import org.activemq.store.PersistenceAdapter;

public class CustomBrokerContainerFactory extends BrokerContainerFactoryImpl {

  public BrokerContainer createBrokerContainer(String brokerName, BrokerContext context) {
    System.err.println("USING CUSTOM FACTORY");
    PersistenceAdapter persistenceAdapter=getPersistenceAdapter();
    if (persistenceAdapter!=null) {
      return new CustomBrokerContainer(brokerName, persistenceAdapter, context);
    } else {
      return new CustomBrokerContainer(brokerName, context);
    }
  }

}
