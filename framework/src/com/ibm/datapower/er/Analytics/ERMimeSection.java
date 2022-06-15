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

package com.ibm.datapower.er.Analytics;

import java.io.InputStream;

public class ERMimeSection {
	public InputStream mInput = null;
	public int mPhase = 0;
	public int mIterator = 0;
	public String mCidName = "";
	
	public ERMimeSection(InputStream input, int phase) {
		mInput = input;
		mPhase = phase;
	}
	
	public ERMimeSection(InputStream input, int phase, int itr, String cidName) {
		mInput = input;
		mPhase = phase;
		mIterator = itr;
		mCidName = cidName;
	}
}
