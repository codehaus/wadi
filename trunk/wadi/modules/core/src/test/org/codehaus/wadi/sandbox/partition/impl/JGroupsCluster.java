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
package org.codehaus.wadi.sandbox.partition.impl;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.partition.Cluster;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MessageListener;
import org.jgroups.View;
import org.jgroups.blocks.RpcDispatcher;

public class JGroupsCluster implements Cluster {

	protected final Log _log = LogFactory.getLog(getClass());

	protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
	protected final String _channelName="WADI";
	protected final long _timeout=30*1000L;

	protected final Channel _channel;
	protected final RpcDispatcher _dispatcher;

	protected Address _address;

	protected final MembershipListener _membershipListener=new MembershipListener() {

		public void viewAccepted(View arg0) {
            if ( _log.isInfoEnabled() ) {

                _log.info("MembershipListener:viewAccepted: " + arg0);
            }
		}

		public void suspect(Address arg0) {
            if ( _log.isInfoEnabled() ) {

                _log.info("MembershipListener:suspect: " + arg0);
            }
		}

		public void block() {
            if ( _log.isInfoEnabled() ) {

                _log.info("MembershipListener:block");
            }
		}

	};

	protected final MessageListener _messageListener=new MessageListener() {

		public void receive(org.jgroups.Message arg0) {
            if ( _log.isInfoEnabled() ) {

                _log.info("MessageListener:receive: " + arg0);
            }
		}

		public byte[] getState() {
            if ( _log.isInfoEnabled() ) {

                _log.info("MessageListener:getState");
            }
			return null;
		}

		public void setState(byte[] arg0) {
            if ( _log.isInfoEnabled() ) {

                _log.info("MessageListener:setState: " + arg0);
            }
		}

	};

	public JGroupsCluster(String nodeName) throws Exception {

		if ( _log.isInfoEnabled() ) {

            _log.info(InetAddress.getLocalHost());
            _log.info(InetAddress.getLocalHost().getAddress());
        }


		_channel=new JChannel();
		_dispatcher=new RpcDispatcher(_channel, _messageListener, _membershipListener, this, true, true);
	}

	public String getNodeName(Object address) {
		return ((Address)address).toString();
	}

	public void start() throws ChannelNotConnectedException, ChannelClosedException {
        if ( _log.isInfoEnabled() ) {

            _log.info("starting...");
        }
		try {
			_channel.connect(_channelName);
			_dispatcher.start();
		} catch (Exception e) {
            if ( _log.isErrorEnabled() ) {

                _log.error("ohoh!", e);
            }
		}

		if (!_channel.getState(null, 5000))
            if ( _log.isInfoEnabled() ) {

                _log.info("cluster state is null - this must be the first node");
        }
		else
			if ( _log.isInfoEnabled() ) {

                _log.info("state received");
                _log.info("..started");
            }

		_address=_channel.getLocalAddress();
	}

	public void stop() {
		_channel.disconnect(); // is this enough ?
		_address=null;
	}


	public static void main(String[] args) throws Exception {
		Cluster cluster=new JGroupsCluster(args[0]);
		cluster.start();
		Thread.sleep(100*1000);
		cluster.stop();
	}

}
