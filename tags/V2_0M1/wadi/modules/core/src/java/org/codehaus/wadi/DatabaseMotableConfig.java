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

import java.sql.Connection;

import javax.sql.DataSource;

public interface DatabaseMotableConfig extends StoreMotableConfig {

	String getLabel();
    DataSource getDataSource();
    String getTable();
    
    boolean getReusingStore();
    
    void insert(Connection connection, Motable motable, Object body) throws Exception;
    void delete(Connection connection, Motable motable); // TODO - why no Exception ?
    void update(Connection connection, Motable motable) throws Exception;
	void loadHeader(Connection connection, Motable motable); // TODO - why no Exception ?
	Object loadBody(Connection connection, Motable motable) throws Exception;
    
}