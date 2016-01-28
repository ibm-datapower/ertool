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

public class ItemObject {
	public enum OBJECT_TYPE
	{
		OBJECT,
		BOOLEAN,
		STRING,
		INTEGER,
		ELEMENT,
		NODELIST
	};
	
	private String mObjectName = "";
	private Object mObject = null;
	private OBJECT_TYPE mType = OBJECT_TYPE.OBJECT;
	
	public ItemObject(String objName, Object object, OBJECT_TYPE type)
	{
		setObjectName(objName);
		setObject(object);
		setType(type);
	}
	
	public String getObjectName() { return mObjectName; }
	private void setObjectName(String name) { mObjectName = name; }
	
	public Object getObject() { return mObject; }
	private void setObject(Object obj) { mObject = obj; }
	
	public OBJECT_TYPE getType() { return mType; }
	private void setType(OBJECT_TYPE type) { mType = type; }
	
}
