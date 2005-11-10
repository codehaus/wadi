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
package org.codehaus.wadi.sandbox.gridstate.jgroups;

import java.io.Serializable;
import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

public class JGroupsObjectMessage implements ObjectMessage, Serializable {
	
	public JGroupsObjectMessage() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void setObject(Serializable arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public Serializable getObject() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public String getJMSMessageID() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public void setJMSMessageID(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public long getJMSTimestamp() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public void setJMSTimestamp(long arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setJMSCorrelationID(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public String getJMSCorrelationID() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public Destination getJMSReplyTo() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public void setJMSReplyTo(Destination arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public Destination getJMSDestination() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public void setJMSDestination(Destination arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public int getJMSDeliveryMode() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public void setJMSDeliveryMode(int arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public boolean getJMSRedelivered() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}
	
	public void setJMSRedelivered(boolean arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public String getJMSType() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public void setJMSType(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public long getJMSExpiration() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public void setJMSExpiration(long arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public int getJMSPriority() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public void setJMSPriority(int arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void clearProperties() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public boolean propertyExists(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}
	
	public boolean getBooleanProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}
	
	public byte getByteProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public short getShortProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public int getIntProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public long getLongProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public float getFloatProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public double getDoubleProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return 0;
	}
	
	public String getStringProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public Object getObjectProperty(String arg0) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public Enumeration getPropertyNames() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}
	
	public void setBooleanProperty(String arg0, boolean arg1)
	throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setByteProperty(String arg0, byte arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setShortProperty(String arg0, short arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setIntProperty(String arg0, int arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setLongProperty(String arg0, long arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setFloatProperty(String arg0, float arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setDoubleProperty(String arg0, double arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setStringProperty(String arg0, String arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void setObjectProperty(String arg0, Object arg1) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void acknowledge() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void clearBody() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}
	
}
