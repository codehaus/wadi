package org.codehaus.wadi.sandbox.gridstate;

import org.jgroups.Address;

public class JGroupsLocation extends AbstractLocation {

	protected Address _address;
	
	public JGroupsLocation(Address address) {
		super();
		_address=address;
	}
	
	public Object getValue() {
		return _address;
	}
	
	public void setValue(Object address) {
		_address=(Address)address;
	}
}
