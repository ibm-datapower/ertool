package com.ibm.datapower.er.Analytics;

import java.io.InputStream;

public class ERMimeSection {
	public InputStream mInput = null;
	public int mPhase = 0;

	public ERMimeSection(InputStream input, int phase) {
		mInput = input;
		mPhase = phase;
	}
}
