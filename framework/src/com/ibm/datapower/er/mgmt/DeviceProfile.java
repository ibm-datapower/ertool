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

package com.ibm.datapower.er.mgmt;

import java.util.Hashtable;


/**
 * This class provides a communications profile that is used to connect
 * the appliance.
 * 
 * @author DataPower Technology, Inc.
 * @author Dana Numkena
 *
 */
public class DeviceProfile {

    Hashtable mHashtable;
    
    // the '/' at the end of the URI is necessary.  Without it, certain requests
    // may not work.
    public static final String sDefaultManagmentURI = "/service/mgmt/current/";

    /**
     * Constructor
     */
    public DeviceProfile() {

        mHashtable = new Hashtable();
                
    }

    /**
     * Gets hostname
     * 
     * @return hostname
     */
    public String getHostname() {
        if (mHashtable.containsKey("Host")) {
            return (String) mHashtable.get("Host");
        }
        return null;
    }

    /**
     * Sets hostname 
     * 
     * @param string hostname
     */
    public void setHostname(String string) {
        mHashtable.put("Host", string.trim());
    }

    /**
     * Gets XML management port
     * 
     * @return XML management port
     */
    public int getPort() {
        if (mHashtable.containsKey("Port")) {
            return Integer.parseInt((String) mHashtable.get("Port"));
        }
        return 0;
    }

    /**
     * Sets XML management port
     * 
     * @param i XML management port
     */
    public void setPort(int i) throws ERMgmtInvalidPortException {
        if(i < -1 || i > 0xffff)
        {
            throw new ERMgmtInvalidPortException("Port " + i + " is out of range");
        }
        mHashtable.put("Port", String.valueOf(i));
    }
    
    /**
     * Gets XML management URI
     * 
     * @return XML management URI
     */
    public String getManagementURI() {    
        return sDefaultManagmentURI;
    }

    /**
     * Gets password
     * 
     * @return password
     */
    public String getPassword() {
        if (mHashtable.containsKey("Password")) {
            return new String(
                Base64.Decode((String) mHashtable.get("Password")));
        }
        return null;
    }

    /**
     * Sets password
     * 
     * @param string password
     */
    public void setPassword(String string) {
        mHashtable.put("Password", Base64.Encode(string.getBytes()));
    }

    /**
     * Gets user name
     * 
     * @return user name
     */
    public String getUser() {
        if (mHashtable.containsKey("User")) {
            return (String) mHashtable.get("User");
        }
        return null;
    }

    /**
     * Sets user name
     * 
     * @param string user name
     */
    public void setUser(String string) {
        mHashtable.put("User", string.trim());
    }
    
    /**
     * Gets the certificate to use
     * 
     * @return certificate as a string
     */
    public String getCertificate() {
        return (String) mHashtable.get("Certificate");
    }

    /**
     * Sets certificate to use
     * 
     * @param certficate the certificate as PEM string
     */
    public void setCertificate(String certificate) {
    	if(certificate != null){
    		mHashtable.put("Certificate", certificate);
    	}
    }
    

    /**
     * Gets allowing untrusted certificate
     * 
     * @return allow untrusted certificate
     */
    public boolean getAllowUntrustedCert() {
        if (mHashtable.containsKey("AllowUntrustedCertificate")) {
            return Boolean.parseBoolean((String) mHashtable.get("AllowUntrustedCertificate"));
        }
        return false;
    }

    /**
     * Sets allowing untrusted certificate
     * 
     * @param allowUntrustedCert allow untrusted certificate
     */
    public void setAllowUntrustedCert(boolean allowUntrustedCert) {
        mHashtable.put("AllowUntrustedCertificate", Boolean.toString(allowUntrustedCert));
    }

}
