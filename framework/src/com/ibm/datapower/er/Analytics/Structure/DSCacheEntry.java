package com.ibm.datapower.er.Analytics.Structure;

import java.util.ArrayList;

import com.ibm.datapower.er.Analytics.DocumentSection;

public class DSCacheEntry {
	// keeping data quickly accessible so we can pull it out and re-use for formula processing
	public String cidName = "";
	public ArrayList<DocumentSection> documentSet = new ArrayList<DocumentSection>();
	public boolean wildcardValue = false;
	public String extension = "";
}
