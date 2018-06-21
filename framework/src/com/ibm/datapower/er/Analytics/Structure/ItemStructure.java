/**
 * Copyright 2014-2016 IBM Corp.
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

import java.util.HashMap;
import java.util.Map;

import com.ibm.datapower.er.Analytics.Structure.ItemObject.OBJECT_TYPE;

public class ItemStructure {

	public ItemStructure()
	{
		
	}
	
	public ItemObject getItem(String name)
	{
		ItemObject val = (ItemObject) mItems.get(name);
		return val;
	}
	
	public void addItem(String name, Object obj, OBJECT_TYPE type)
	{
		mItems.remove(name); // override previous entry, remove it if existed
		ItemObject itmObj = new ItemObject(name,obj,type);
		mItems.put(name, itmObj);
	}
	
	
	private Map<String, ItemObject> mItems = new HashMap<String, ItemObject>();
}
