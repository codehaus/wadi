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
package org.codehaus.wadi.jetty5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.codehaus.wadi.Securable;
import org.mortbay.http.HttpListener;

/**
 * A Jetty Listener, which defines a type of Connection on which we may set a flag to indicate whether
 * it should be considered secure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SocketListener extends org.mortbay.http.SocketListener {

	public static class HttpConnection extends org.mortbay.http.HttpConnection implements Securable {
		
		public HttpConnection(HttpListener listener, InetAddress remoteAddr, InputStream in, OutputStream out, Object connection) {
			super(listener, remoteAddr, in, out, connection);
		}
		
		protected boolean _secure;
		public boolean getSecure(){return _secure;}
		public void setSecure(boolean secure){_secure=secure;}
	}
	
	protected org.mortbay.http.HttpConnection createConnection(Socket socket) throws IOException {
		HttpConnection connection=new HttpConnection(this, socket.getInetAddress(), socket.getInputStream(), socket.getOutputStream(), socket);
		return connection;
	}
	
	public boolean isIntegral(org.mortbay.http.HttpConnection connection){return ((HttpConnection)connection).getSecure();}
	public boolean isConfidential(org.mortbay.http.HttpConnection connection){return ((HttpConnection)connection).getSecure();}
}
