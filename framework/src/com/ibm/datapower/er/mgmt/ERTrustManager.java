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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ibm.jsse2.IBMJSSEProvider2;




/**
 * A TrustManager with the ability to add certificates. 
 * Does not do any Client Certificate checking, Only Server certificate checking
 * Will throw a ERTrustManagerCertificateException that contains the untrusted X509Certificate and a message with information about the Certificate
 * 
 * @author Alex Teyssandier
 *
 */
public class ERTrustManager implements X509TrustManager {
      private X509TrustManager mTrustManager;      
      private TrustManagerFactory mTrustManagerFactory = null;
      private ArrayList mCertList = new ArrayList();
      // allow untrusted certs to not throw an exception
      private boolean mAllowUntrusted = false;
      
      // the following "final" values are defined in the IBM JSSE Reference Guide.
      // They assume use of the IBM implementation of JSSE and not the Sun implementation.
      // if using JSSE2 (WAS 6.1 and later) we prefer IbmPKIX over IbmX509
      // because IbmX509 in JSSE2 gives an error, but not in JSSE1
      final String mTrustManagerFactoryAlgorithm = "PKIX"; //java1.5 and java1.6 use PKIX 
      final String mTrustManagerFactoryBackupAlgorithm = "IbmPKIX"; //java 1.4.2 uses IBMPKIX instead of PKIX when running JSSE2
      final String mTrustManagerFactoryBackupAlgorithm2 = "IbmX509"; //java 1.4.2 running JSSE will use IbmX509, but using JSSE causes a KeyStoreException at KeyStoreManager.init(). This is avoided by line 775 
      
      //The Default 9004 certificate
      private static final String DATAPOWER_CA_CERT_PEM_9004 = "-----BEGIN CERTIFICATE-----\n"
          + "MIIClTCCAX2gAwIBAgIBAzANBgkqhkiG9w0BAQUFADBKMQswCQYDVQQGEwJVUzEj\n"
          + "MCEGA1UEChMaRGF0YVBvd2VyIFRlY2hub2xvZ3ksIEluYy4xFjAUBgNVBAsTDVNT\n"
          + "TCBTZXJ2ZXIgQ0EwHhcNMDYwNDI1MjAxMzI1WhcNMTMwNjA2MjAxMzI1WjAlMSMw\n"
          + "IQYDVQQDExpEYXRhUG93ZXIgVGVjaG5vbG9neSwgSW5jLjCBnzANBgkqhkiG9w0B\n"
          + "AQEFAAOBjQAwgYkCgYEAtR0eyxvhuPTaz1FFW3nALLeVr/ovzAQvqdhtSsmCedmm\n"
          + "x0i5kUy7R4eFZ9knfF7YLc0bUyyBiDkSU95ouoety8ZuEAmwPLuFW0DOTgLtyKq8\n"
          + "Zm3RS906kBcFayGgMDyYseV8PQqgi/BbpCczwsucgo5xYtVpAMa3No/7UAjEWncC\n"
          + "AwEAAaMvMC0wCQYDVR0TBAIwADALBgNVHQ8EBAMCBeAwEwYDVR0lBAwwCgYIKwYB\n"
          + "BQUHAwEwDQYJKoZIhvcNAQEFBQADggEBAM7cigpF4M4TxjbmDQjiTTwY0eNBLHio\n"
          + "sMehNb1PAuqmfGnU+yInmGmeOIpM7iWbIALy6QMoQovWcVeklRCwEIaOr1f6lzXW\n"
          + "K7u/diuKphr4mtnZh3x1QOcngVzWstCHtxiHbTpmhlTbp/t1MMTzrdzBCQkxRHxD\n"
          + "le5Eo+IrBCueRu4zPcNtFVcEsqN6iq2Y4+lyJoljAZ/cZsPJKPQUawnRLcNTwjzo\n"
          + "tcRDC+2SSbejMJCJpr45DcNlCPuqsS+1avbDAo9ATeFzWwRsIbI0wVcR8r6oRSwh\n"
          + "Sw4qVuHKrgLM/+oRvIuYIoeRGslo9aR8l1yQJ7RitIiHBLsFe+U8aqk=\n"
          + "-----END CERTIFICATE-----\n";
    
      //The Default 9005 certificate
      private static final String DATAPOWER_CA_CERT_PEM_9005 = "-----BEGIN CERTIFICATE-----\n"
      + "MIIDdTCCAl2gAwIBAgIBATANBgkqhkiG9w0BAQUFADBMMQswCQYDVQQGEwJVUzEM\n"
      + "MAoGA1UEChMDSUJNMR0wGwYDVQQLExRXZWJTcGhlcmUgQXBwbGlhbmNlczEQMA4G\n"
      + "A1UEAxMHUm9vdCBDQTAgFw0wOTEwMjcyMDU1NTNaGA8yMDM4MDEwMTAwMDAwMFow\n"
      + "TDELMAkGA1UEBhMCVVMxDDAKBgNVBAoTA0lCTTEdMBsGA1UECxMUV2ViU3BoZXJl\n"
      + "IEFwcGxpYW5jZXMxEDAOBgNVBAMTB1Jvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUA\n"
      + "A4IBDwAwggEKAoIBAQC8/v9eoz54J76rWEkYfMlMmeFpA/tXbpV4ZL3K+3pN1vqa\n"
      + "B6cc1U/UosnK/hurO462Undk9A1Nk8MiLKH+rAKGqtku64vo7W8thFxxFbXsB0PR\n"
      + "lwvhkNljuCFBJqf7hkMjpiG3GtFwHpeyt+3gplI+9QOdJdvYZF98R4dsjfQ9QwpJ\n"
      + "F22/Nu77w5Kq+7bWIwAaTYsYeYDcm9Ng6yPTwBlS37mIqlqfxE1IZ2Jf9p3OcMpL\n"
      + "5ezvZUhVbkKCtcHybKbvn9m19Nbpw0/214o+UV7MOXenLDqwLdLI7vtyg5E0VxuJ\n"
      + "e1GakqEMH9/f5yLVrtlQAPnSm3dZ+YIXdVPOyjvrAgMBAAGjYDBeMA8GA1UdEwEB\n"
      + "/wQFMAMBAf8wHQYDVR0OBBYEFOE2HBFDaVDeL26jnyFvVXdvqT/ZMB8GA1UdIwQY\n"
      + "MBaAFOE2HBFDaVDeL26jnyFvVXdvqT/ZMAsGA1UdDwQEAwIBBjANBgkqhkiG9w0B\n"
      + "AQUFAAOCAQEAXzkYaNdfp8/qMd19SauIP3w+ci4JUcS5HVXvifxQrVPV4x7OAKod\n"
      + "EjPQ4V5TjWwzSRGHoESEW+OGdZfiqKLcNyWpBS2IUn/Uc6/OUYCm1iEImftOgqOI\n"
      + "q9CXKjAPsfbD5xYvMieOx6+ObQuGKN8R+PXAsZHusjFnU5bZJk+kALjrh6kZ6M+X\n"
      + "QX6wQUGFlkLhepKcqVo0joxLrha6IxMsHmdj397XJCK6D8YJJRW9ILrp8ZI3+njR\n"
      + "o72WCi/1T4L6KX89GFBhPHZ0x8rGkkF/N8Oz++LFNRzbwmIWe1d8lZFU3niur1K1\n"
      + "jMTjcbfrwspvIJTASxZ1LkVQL0csmqaPvg==\n"
          + "-----END CERTIFICATE-----\n";


      /**
       * The default constructor for the custom TrustManager, uses loadDefaults() to load the default trust managers and insert the 9004 and 9005 certificates 
       */
      public ERTrustManager(boolean allowUntrustedCert) throws ERMgmtException{
          //loads the default certificates into the trustManager;    
          loadDefaults();
          mAllowUntrusted = allowUntrustedCert;
          //add the DataPower Certificates
          addCert(DATAPOWER_CA_CERT_PEM_9005);
          addCert(DATAPOWER_CA_CERT_PEM_9004);
          
      }
      
      /**
       * Adds the certificate to the default certificates in the trust manager
       * first adds the certificate to the mCertList then reinits thetrustManagerFactory and reloads the TrustManager 
       *  
       * @param alias string with the lias of the certificate.
       * @param certificate String containing the PEM formatted certificate
       */
      public void addCert(String certificate) throws ERMgmtException{
          try{
              CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
              InputStream certificateInputStream = new ByteArrayInputStream(certificate.getBytes());
              X509Certificate toadd = (X509Certificate) certificateFactory.generateCertificate(certificateInputStream);
              certificateInputStream.close();
              mCertList.add(toadd);                            
              loadTM();          
          } catch (Exception e){              
              throw new ERMgmtException("Trust Manager Error: ",  e);
          } 
          

      }
      /**
       * Initializes the trustManagerFactory
       * Generates the trustManager using the List of certificates mCertList    
       */
       private void loadTM() throws ERMgmtException{
           try{
                  KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
               //random number generate to generated aliases for the new keystore
               Random generator = new Random();
               
               ts.load(null, null);
               
               if(mCertList.size() > 0){
                   for(int i=0; i<mCertList.size(); i++){
                       int randomInt = generator.nextInt();
                       while(ts.containsAlias("certificate"+randomInt)){
                           randomInt = generator.nextInt();
                       }
                       ts.setCertificateEntry("certificate"+randomInt, (X509Certificate) mCertList.get(i));
                   }                   
                   mTrustManagerFactory.init(ts); 
               } else {                   
                   mTrustManagerFactory.init((KeyStore) null);
               }
                         
               TrustManager[] tm = mTrustManagerFactory.getTrustManagers();
               for(int i=0; i<tm.length; i++){
                   if (tm[i] instanceof X509TrustManager) {
                       mTrustManager = (X509TrustManager)tm[i];                       
                       break;
                    }
               }
               
           } catch (Exception e){ 
               throw new ERMgmtException("Trust Manager Error", e);
           }
 
       }
      /**
       * Generates the trustManagerFactory with the correct Algorithm and default KeyStore.
       * Loads the default TrustManager from the new trustManagerFactory
       * Adds all the certificates from the TrustManagerFactory to the certificate list 
       * 
       */
      private void loadDefaults() throws ERMgmtException{
//        

          try {
              //first try PKIX, works on 1.5 and 1.6
              mTrustManagerFactory = TrustManagerFactory.getInstance(mTrustManagerFactoryAlgorithm);                 
          } catch (NoSuchAlgorithmException nae1) {
              try{
                  //trustManagerFactory should be using IBMJSSE2 in java1.4.2, or an exception will happen upon init
                  Security.insertProviderAt(new IBMJSSEProvider2(), 1);
                  //PKIX is called IbmPKIX on 1.4.2. running IBMJSSE2
                  mTrustManagerFactory = TrustManagerFactory.getInstance(mTrustManagerFactoryBackupAlgorithm);
              }
              catch (NoSuchAlgorithmException nae2){
                  //then try Ibm509 for 1.4.2 running JSSE
                  try  {
                      mTrustManagerFactory = TrustManagerFactory.getInstance(mTrustManagerFactoryBackupAlgorithm2);  
                  }
                  catch (NoSuchAlgorithmException nae3){
                      throw new ERMgmtException("Trust Manager Error: " , nae3);
                  }                  
              }
          }
          loadTM();              
          X509Certificate[] defaultChain = mTrustManager.getAcceptedIssuers();
          for(int i=0; i<defaultChain.length; i++){
              mCertList.add(defaultChain[i]);
          }
      }

      
      /**
       * Return an array of certificate authority certificates which are trusted for authenticating peers.
       * 
       * @return the array of X509Certificate that are trusted for authentication  
       */
      public X509Certificate[] getAcceptedIssuers () {
            X509Certificate[] issuers = mTrustManager.getAcceptedIssuers();
            return issuers;
      }
      /**
       * Given the partial or complete certificate 
       * chain provided by the peer, 
       * build a certificate path to a trusted root and 
       * return if it can be validated and 
       * is trusted for client SSL authentication based on the authentication type.
       * 
       * @param chain     the peer certificate chain
       * @param authType  the authentication type based on the client certificate
       * 
       *  @throws CertificateException if the certificate chain is not trusted by this TrustManager. Is never thrown in this implementation
       * 
       * ERTrustManager implementation, since it is only for checking server certificates 
       * does not check fortrusted Client certificate, and simply allows all certificates  
       */
      public void checkClientTrusted ( X509Certificate[] chain, String authType ) throws CertificateException {
          //This method should be coded when TrustManager is fully implemented             
          try{
              mTrustManager.checkServerTrusted(chain, authType);  
          } catch (CertificateException e) {
              //Do nothing for an all trusting TrustManager
          }
          
      }
      /**
       * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and return if it can be validated and is trusted for server SSL authentication based on the authentication type.
       * 
       * @param chain    the peer certificate chain
       * @param authType the key exchange algorithm used 
       */
      public void checkServerTrusted ( X509Certificate[] chain, String authType ) throws CertificateException {          
          //String to hold the issuer of first member in the chain
          String issuer = "";
          //String to hold the subject of the first member in the chain
          String subject = "";
          //Calendar to get the valid on and expires on date
          Calendar cal=Calendar.getInstance();
          //Date and String to hold the date the certificate was issued on
          Date issuedOn = null;
          String issuedOnString = new String("");
          //Date and String to hold the date the certificate is valid until
          Date expiresOn = null;
          String expiresOnString = new String("");
          //BigInteger to hold the serial number of the certificate
          BigInteger serial = null;
          //the highest certificate in the chain (the root)
          X509Certificate highestCert =  null;
          
          try {
              highestCert = chain[0];
              issuer = highestCert.getIssuerX500Principal().toString();
              subject = highestCert.getSubjectX500Principal().toString();
              serial = highestCert.getSerialNumber();
              
              issuedOn = highestCert.getNotBefore();
              cal.setTime(issuedOn);
              issuedOnString = new String((cal.get(Calendar.MONTH)+1) + "/"+cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.YEAR));
              expiresOn = highestCert.getNotAfter();
              cal.setTime(expiresOn);
              expiresOnString = new String( (cal.get(Calendar.MONTH)+1) + "/"+cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.YEAR));
              
              mTrustManager.checkServerTrusted(chain, authType);
          } catch (CertificateException cx) {    
        	  if ( !mAllowUntrusted )
        	  {
        		  ERTrustManagerCertificateException erce = new ERTrustManagerCertificateException("\nUntrusted Certificate Found: "+
                                                                                                "\nIssued by: "+ issuer + 
                                                                                                "\nIssued to: " + subject + 
                                                                                                "\nIssued on: " + issuedOnString + 
                                                                                                "\nExpires on: " + expiresOnString + 
                                                                                                "\nSerial: " + serial); 
        		  erce.setCertificate(highestCert);
              
            	  throw erce;
        	  }
          }

      }

}
