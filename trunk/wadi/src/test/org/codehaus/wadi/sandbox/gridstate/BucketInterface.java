package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

public interface BucketInterface {

	void init(BucketConfig config);
	
	/**
	 * Put a fresh association into the Map unless the key is already in use.
	 * @param key
	 * @param location
	 * @return
	 */
	boolean putAbsent(Serializable key, Destination location);

	/**
	 * Add an association to the map, overwriting old value if it already existed.
	 * @param key
	 * @param location
	 * @return associations previous value
	 */
	Destination putExists(Serializable key, Destination location);
	
	
	/**
	 * Remove an association from the Map
	 * @param key
	 * @param map
	 * @return association's value
	 */
	Serializable removeReturn(Serializable key, Map map);
	
	/**
	 * Remove an association from the Map (not returning value save [de]serialization)
	 * @param key
	 */
	void removeNoReturn(Serializable key);

	// Serializable executeSync(Object process);
	// void executeASync(Object process);
	
}
