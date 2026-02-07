package com.ibm.datapower.er.Analytics.Structure;

import java.io.InputStream;

public class SectionStream {
	public InputStream OriginalStream = null;
	public InputStream EncapsulatedStream = null;
	public SectionStream(InputStream ogStream, InputStream encapsStream) {
		OriginalStream = ogStream;
		EncapsulatedStream = encapsStream;
	}
}
