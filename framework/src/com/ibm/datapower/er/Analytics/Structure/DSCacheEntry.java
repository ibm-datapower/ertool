/**
 * Copyright 2014-2020 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

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
