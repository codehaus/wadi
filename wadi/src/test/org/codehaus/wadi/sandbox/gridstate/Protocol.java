package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;

public interface Protocol {

	// called on PO...
	Serializable get(Serializable key);

	// called on PO...
	Serializable put(Serializable key, Serializable value, boolean overwrite, boolean returnOldValue);

	Serializable remove(Serializable key, boolean returnOldValue);
	
}