package org.codehaus.wadi.sandbox.gridstate;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Locker;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.impl.AbstractSharedContextualiser;

public class GridStateContextualiser extends AbstractSharedContextualiser {

	public GridStateContextualiser(Contextualiser next, Locker locker) {
		super(next, locker, false);
		// TODO Auto-generated constructor stub
	}

	public Emoter getEmoter() {
		// TODO Auto-generated method stub
		return null;
	}

	public Immoter getImmoter() {
		// TODO Auto-generated method stub
		return null;
	}

	public Motable get(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public void load(Emoter emoter, Immoter immoter) {
		// TODO Auto-generated method stub

	}

}
