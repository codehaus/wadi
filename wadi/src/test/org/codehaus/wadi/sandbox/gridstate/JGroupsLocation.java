package org.codehaus.wadi.sandbox.gridstate;

import org.jgroups.Address;

public class JGroupsLocation extends AbstractLocation {

	protected Address _address;
	
	public JGroupsLocation(Address address) {
		super();
		_address=address;
	}
	
	public Address getAddress() {
		return _address;
	}
	
	public void setAddress(Address address) {
		_address=address;
	}
}
