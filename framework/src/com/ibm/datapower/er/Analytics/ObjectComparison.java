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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


public class ObjectComparison {
	
	public static boolean CompareObjectToFile(ArrayList<ConditionsNode> results, String file)
	{
		for(int i=0;i<results.size();i++)
		{
			WriteObjectToBuffer((ConditionsNode)results.get(i));
	
			ObjectInputStream inputStream;
			try {
				inputStream = new ObjectInputStream(new FileInputStream(file + "." + i + ".bin"));
			} catch (Exception ex)
			{
				break;
			}
			ConditionsNode prevBuffer = (ConditionsNode)ReadObjectToBuffer(inputStream);
			
			if ( inputStream != null )
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			ConditionsNode node = results.get(i);
			
			if ( prevBuffer == null || !prevBuffer.toString().equals(node.toString()) )
				return false;
		}
		
		if ( results.size() > 0 )
			return true;
		
		return false;
	}

	public static ByteArrayOutputStream WriteObjectToBuffer(Object obj)
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
		try {
			ObjectOutputStream outStream = new ObjectOutputStream(buffer);
			outStream.writeObject(obj);
			outStream.close(); // should we do this despite writeObject failing? I am not sure, then it will need another try catch
		} catch (IOException e) {

		}

		return buffer;
	}

	public static Object ReadObjectToBuffer(ObjectInputStream inputStream)
	{
		Object obj = null;
		try {
			obj = inputStream.readObject();
			if ( obj instanceof ConditionsNode )
				return obj;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
