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

package com.ibm.datapower.er.mgmt;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This exception occurs when a connection to the appliance fails because of a Certificate not trusted problem. 
 * Thrown by ERTrustManager when an untrusted certificate is found.
 * Contains the untrusted X509Certificate as well as a message indicating the issuers and the subject
 * 
 * @author Alex Teyssandier
 *
 */

public class ERTrustManagerCertificateException extends CertificateException {
    //the X509Certificate associated with the exception
    X509Certificate certificate = null;
    
    /**
     * Constructor for a Certificate Exception with a null certificate and a given message
     * @param message      the message of the exception. Should be the Issuer and Subject Information to present to the user
     * 
     */
    ERTrustManagerCertificateException(String message) {
        super(message);
    }
    
    /**
     * Constructor for a Certificate Exception with a given Certificate and Message
     * 
     * @param message The message to set the exception. Should be the Issuer and Subject Information to present to the user 
     * @param mycert   The X509Certificate that caused the exception.
     * 
     */
    ERTrustManagerCertificateException(String message, X509Certificate mycert) {
        this(message);
        certificate = mycert;
    }
    
    /**
     * Used to set the X509Certificate certificate that caused the exception to happen
     * 
     * @param mycert    The certificate that caused the exception
     */
    public void setCertificate(X509Certificate mycert){        
        certificate = mycert;
    }
    
    /**
     * Used to retrieve the certificate as an X509Certificate Object.
     * 
     * @return The X509Certificate
     */
    public X509Certificate getCertificate(){
        return certificate;
    }
    
    /**
     * Used to retrieve the certificate as a PEM formatted string as a PEM formatted string can be used to generate X509Certificates
     * 
     * @return the certificate in PEM format. 
     * @throws ERMgmtException when there was an error encoding the Certificate as a string 
     */
    public String getCertificateAsString() throws ERMgmtException{
        StringBuffer sb = new StringBuffer(2252);
        
        try{
            sb.append("-----BEGIN CERTIFICATE-----\n");
            sb.append(Base64.Encode(certificate.getEncoded()));
            sb.append("\n-----END CERTIFICATE-----\n");
        } catch (CertificateEncodingException e){
            throw new ERMgmtException("Error Encoding Certificate as String: "+e.toString());
        }
        
        return sb.toString();
    }  
}    
