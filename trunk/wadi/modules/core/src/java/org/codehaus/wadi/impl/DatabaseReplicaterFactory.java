package org.codehaus.wadi.impl;

/**
 * A DatabaseReplicater holds no per Session state, so all Sessions may use the same Replicater
 *
 */
public class DatabaseReplicaterFactory extends StatelessReplicaterFactory {

	public DatabaseReplicaterFactory(DatabaseStore store, boolean reusingStore) {
		super(new DatabaseReplicater(store, reusingStore));
	}
	
}
