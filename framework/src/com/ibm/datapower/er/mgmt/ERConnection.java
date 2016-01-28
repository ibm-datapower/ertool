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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * This class provides methods to send and receive SOAP messages 
 * to and from the appliance.
 * 
 * @author DataPower Technology, Inc.
 * @author Dana Numkena
 * 
 */
public class ERConnection {

    private class WaitForResponse implements Runnable {

        private HttpsURLConnection mConnection = null;
        private long mTimeout = 0;
        private ERMgmtException mError = null;
        private boolean mTimedOut = false;
        private boolean mResponded = false;
        
        /**
         * Constructor
         * 
         * @param con HttpsURLConnection that will wait for the response
         * 
         * @param timeout timeout in seconds
         * 
         */
        public WaitForResponse(HttpsURLConnection connection, int timeout) {
            mConnection = connection;
            mTimeout = timeout * 1000;
        }
        
        /**
         * Wait with timeout.
         * 
         * This will be the server that will start a thread that will
         * wait for a response.  The server will provide the timeout 
         * feature.
         * 
         * @throws ERMgmtException
         */
        public void waitForResponse() throws ERMgmtException {
            
            TraceHelper.trace(
                "Waiting for response from '" + mConnection.getURL() + "'");

            // create separate thread that will make the connection
            Thread connect_thread = new Thread(this);

            //    If this is the last thread running, allow exit.
            connect_thread.setDaemon(true);

            // initiate the connection
            connect_thread.start();

            
        try {
            // wait for the connection thread to connect or when time has elapsed
            connect_thread.join(mTimeout);
                
        } catch (java.lang.InterruptedException ie) {
                
            // interrupt connect thread
            connect_thread.interrupt();
        }

            // if connect thread had an exception
            if (mError != null) {
                // throw the connect thread exception
                throw mError;
            }

            // if connect was not made
            if (mResponded == false) {
                
                // indicate a time out
                mTimedOut = true;
                
                TraceHelper.trace("Response timeout: " + mConnection.getURL());
                
                throw new ERMgmtNoHttpResponseException("Response timeout");
                
                // note: connect thread will timeout on its own with OS default
                
            } 
            // else connection was made
            else {
                TraceHelper.trace("Response received '" + mConnection.getURL() + "'");
            }
        }
        
        /**
         * This thread will actually do the waiting.
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            
        try {
                // make the connection
                this.mConnection.getResponseCode();                
            } catch (UnknownHostException e) {
                this.mError = new ERMgmtUnknownHostException("Unknown/Invalid Host : " + e.getMessage());
            } catch (ConnectException e) {
                this.mError = new ERMgmtConnectException("unable to connect: " + e.getMessage());
            } catch (NoRouteToHostException e) {
                this.mError = new ERMgmtNoRouteToHostException("no route to host: " + e.getMessage());
            } catch (SSLHandshakeException e){
                Throwable nextE = e.getCause();
                ERTrustManagerCertificateException mye;
                while((nextE != null) && !(nextE instanceof ERTrustManagerCertificateException)){            
                    nextE = nextE.getCause();             
                }                
                if(nextE == null){
                    this.mError = new ERMgmtException("Connection Exception: " + e.toString()); 
                } else{
                    mye = (ERTrustManagerCertificateException) nextE;
                    this.mError = new ERMgmtCertificateException(nextE.getMessage(), mye.getCertificate());    
                }
            } catch (Exception e) {        
                this.mError = new ERMgmtException("connection Exception: " + e.toString());                
            } 
            // if time elapsed
            if (mTimedOut) {
                // no exception and just return
                mError = null;
                return;
            }

            // indicate connection was made
            this.mResponded = true;
            
            return;
        }
    }
    
    /**
     * This class adds a configurable timeout feature
     * for URLConnection.connect.  
     * 
     * @author Dana Numkena
     *
     */
    private class ConnectWithTimeout implements Runnable {

        private URLConnection mURLConnection = null;
        private long mTimeout = 0;
        private ERMgmtException mError = null;
        private boolean mTimedOut = false;
        private boolean mConnected = false;

        /**
         * Constructor
         * 
         * @param con URLConnection that will make the connect
         * 
         * @param timeout timeout in seconds
         * 
         */
        public ConnectWithTimeout(URLConnection con, int timeout) {
            mURLConnection = con;
            mTimeout = timeout * 1000;
        }

        /**
         * Connect with timeout.
         * 
         * This will be the server that will start a thread that will
         * make the connection.  The server will provide the timeout 
         * feature.
         * 
         * @throws ERMgmtException
         */
        public void connect() throws ERMgmtException {
            
            TraceHelper.trace(
                "Attempting to connect to '" + mURLConnection.getURL() + "'");

            // create separate thread that will make the connection
            Thread connect_thread = new Thread(this);

            //    If this is the last thread running, allow exit.
            connect_thread.setDaemon(true);

            // initiate the connection
            connect_thread.start();

            
        try {
            // wait for the connection thread to connect or when time has elapsed
            connect_thread.join(mTimeout);
                
        } catch (java.lang.InterruptedException ie) {               
            // interrupt connect thread
            connect_thread.interrupt();
        }

            // if connect thread had an exception
            if (mError != null) {
                throw mError;
            }

            // if connect was not made
            if (mConnected == false) {
                
                // indicate a time out
                mTimedOut = true;
                
                TraceHelper.trace("Connection timeout: " + mURLConnection.getURL());
                
                throw new ERMgmtConnectException("connect timeout");
                
                // note: connect thread will timeout on its own with OS default
                
            } 
            // else connection was made
            else {
                TraceHelper.trace("Connected to '" + mURLConnection.getURL() + "'");
            }
        }

        /** 
         * This thread actually make the connection
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            
        try {
                // make the connection
                this.mURLConnection.connect();
                
            } catch (UnknownHostException e) {
                this.mError = new ERMgmtUnknownHostException("Unknown/Invalid Host : " + e.getMessage());
            } catch (ConnectException e) {
                this.mError = new ERMgmtConnectException("unable to connect: " + e.getMessage());
            } catch (NoRouteToHostException e) {
                this.mError = new ERMgmtNoRouteToHostException("no route to host: " + e.getMessage());
            } catch (SSLHandshakeException e){
                Throwable nextE = e.getCause();
                ERTrustManagerCertificateException mye;
                while((nextE != null) && !(nextE instanceof ERTrustManagerCertificateException)){            
                    nextE = nextE.getCause();             
                }                
                if(nextE == null){
                    this.mError = new ERMgmtException("Connection Exception: " + e.toString()); 
                } else{
                    mye = (ERTrustManagerCertificateException) nextE;
                    this.mError = new ERMgmtCertificateException(nextE.getMessage(), mye.getCertificate());    
                }
            }catch (Throwable e) {            
                this.mError = new ERMgmtIOException("connection Exception: " + e.toString());               
            } 
            
            
            // if time elapsed
            if (mTimedOut) {
                // no exception and just return
                mError = null;
                return;
            }

            // indicate connection was made
            this.mConnected = true;
            
            return;
        }

    }
    // the cached custom ssl factory
    private static SSLSocketFactory mCachedCustomSSLSocketFactory = null;

    // device profile
    private DeviceProfile mProfile;

    // device url
    private URL mURL;
    

    // replacment of host name verifier to deal with datapower generic certificate
    private static final HostnameVerifier mNullVerifier = new HostnameVerifier() {
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }
    };

    /**
     * Creates the user name and password combination in base64 format.
     * 
     * @return String base64 encoded user and password
     */
    private String getBase64UserPassword() {
        String login = mProfile.getUser() + ":" + mProfile.getPassword();
        //TraceHelper.trace("getBase64UserPassword: Basic " + Base64.Encode(login.getBytes()));
        return "Basic " + Base64.Encode(login.getBytes());
    }

    /**
     * Constructor
     * 
     * @param profile communication settings to connect to appliance
     *            
     * @throws ERMgmtException
     */
    public ERConnection(DeviceProfile profile) throws ERMgmtException {
        
        mProfile = profile;
        
        try {
            mURL = new URL("https://" + mProfile.getHostname() + ":"
                    + mProfile.getPort() + mProfile.getManagementURI());
            TraceHelper.trace("Specified URL: https://" + mProfile.getHostname() + ":"
                               + mProfile.getPort() + mProfile.getManagementURI());
        } catch (MalformedURLException e) {
            throw new ERMgmtException("Connection https://" + mProfile.getHostname() + ":"
                    + mProfile.getPort() + mProfile.getManagementURI() + " MalformedURLException :" + e.getMessage());           
        }        
        
    }
    /**
     * checks the certificate in the mProfile
     * 
     * @throws ERMgmtException
     */

    public void checkCert() throws ERMgmtException {
        URLConnection connection = null;
        HttpsURLConnection httpsConnection = null;
        if (mURL == null) {
            throw new ERMgmtIOException("Non-initialized connection");
        }
        try {
             connection = mURL.openConnection();
             // we know that we used https to connect
             httpsConnection = (HttpsURLConnection) connection;
             httpsConnection.setHostnameVerifier(mNullVerifier);

             SSLSocketFactory customSSLSocketFactory = getCustomSSLSocketFactory();
             httpsConnection.setSSLSocketFactory(customSSLSocketFactory);
             // protect connection attempt with timeout wrapper
             ConnectWithTimeout timeoutWrapper = new ConnectWithTimeout(httpsConnection, 1250); 
             timeoutWrapper.connect();
             // wait for response timeout
             WaitForResponse wfr = new WaitForResponse(httpsConnection, 1200);
             wfr.waitForResponse();

        } catch(IOException e) {        
             throw new ERMgmtIOException("loading cert exception: ", e);            
        } catch (ERMgmtException e) {
            throw e;             
        } finally {
            //reset the cached sslsocketfactory to null in case a new certificate was added
            mCachedCustomSSLSocketFactory = null;
            if (httpsConnection != null){
                httpsConnection.disconnect();
            }
            
        }
                    
     }
    
    /**
     * Sends SOAP request 
     * 
     * @param request request message
     * 
     * @throws ERMgmtException
     */    
    public void send(SoapManagementRequest request) throws ERMgmtException {
                    
            if (mURL == null) {
                throw new ERMgmtIOException("Non-initialized connection");
            }

            URLConnection connection = null;
        try {            
            connection = mURL.openConnection();
        } catch (Throwable e) {
            throw new ERMgmtIOException("open connection exception: " , e);
        }

            // we know that we used https to connect
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            // replace hostname verifier with our own
            httpsConnection.setHostnameVerifier(mNullVerifier);
            
        try {
            // POST request
            httpsConnection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new ERMgmtIOException("protocol exception: " , e);
        } catch (Throwable e) {
            throw new ERMgmtIOException("set request method exception: ", e);
        }

            // Authentication
            httpsConnection.setRequestProperty("Authorization",
                    getBase64UserPassword());

            // expecting request/response
            httpsConnection.setDoInput(true);
            httpsConnection.setDoOutput(true);

            /*
             * In case the device still has the default SSL certificate for the SOMA
             * web service, it is signed using a "DataPower CA" which is not one we
             * trust yet (doesn't exist in the cacerts file). We will need to set up
             * the DataPower CA as a trusted CA to the JVM. Without requiring the
             * DataPower CA to be installed in the usual cacerts file, we can do
             * that dynamically by creating a custom SSLSocketFactory that includes
             * a TrustManager that includes the DataPower CA. Then we tell the
             * HttpsURLConnection object to use that custom SSLSocketFactory. That
             * way the DataPower CA will be trusted only in this application, not
             * all applications launched from this JRE.
             */
        
            SSLSocketFactory customSSLSocketFactory;
        try {
            customSSLSocketFactory = getCustomSSLSocketFactory();
            httpsConnection.setSSLSocketFactory(customSSLSocketFactory);
        } catch(Throwable e) {
            e.printStackTrace();
            throw new ERMgmtIOException("loading cert exception: ", e);
            
        }
        
            // protect connection attempt with timeout wrapper
            ConnectWithTimeout timeoutWrapper = new ConnectWithTimeout(
                                                    httpsConnection, 
                                                    1200); 

            // attempt to connect - protected by timeout
            timeoutWrapper.connect();
        
            SoapEnvelope soapEnv = null;
        try {
            // get the output stream for the connection
            OutputStream out = httpsConnection.getOutputStream();
            
            // get a writer object for the output stream
            OutputStreamWriter writer = new OutputStreamWriter(out);

            // enclose SOMA request message inside SOAP envelope
            soapEnv = new SoapEnvelope(request);
            
            // trace entire request
            TraceHelper tr = new TraceHelper();
            soapEnv.write(tr);
            tr.flush();
            
            // send SOAP message to connection            
            soapEnv.write(writer);

            // send request 
            TraceHelper.trace("Sending SOAP request");
            writer.flush();
            
            // close connection
            writer.close();
        } catch (Throwable e) {
            throw new ERMgmtIOException("sending request exception: " , e);
        }
          
        try {
            // wait for response timeout
            WaitForResponse wfr = new WaitForResponse(httpsConnection, 1200);
            wfr.waitForResponse();
        } catch (ERMgmtNoHttpResponseException e) {
            throw new ERMgmtNoHttpResponseException("No HTTP response.  Check IP Address.", e);
        }
        
        
        try {        
            // trace HTTP headers
            TraceHelper.trace("**** Response Headers:");  
            TraceHelper.trace("response code   : " + httpsConnection.getResponseCode());
            TraceHelper.trace("response message: " + httpsConnection.getResponseMessage());            
            int index = 0;
            String sHeader;
            String sHeaderKey;
            do {
                sHeader    = httpsConnection.getHeaderField(index);
                sHeaderKey = httpsConnection.getHeaderFieldKey(index);
            
                if(sHeader != null || sHeaderKey != null) {
                    TraceHelper.trace(sHeaderKey + " = " + sHeader);
                }
                index++;
            } while (sHeader != null || sHeaderKey != null); 
        

            
        } catch (NullPointerException e) {
            throw new ERMgmtNoHttpResponseException("No HTTP response.  Check IP Address.", e);
        } catch (ConnectException e) {
            throw new ERMgmtConnectException("HTTP Response Timeout.", e);
        } catch (UnknownHostException e) {
            throw new ERMgmtUnknownHostException("parsing HTTP response Unknown/Invalid host: ", e);
        } catch (IllegalArgumentException e) {
            throw new ERMgmtUnknownHostException("parsing HTTP response Unknown/Invalid host: " , e);
        } catch (Throwable e) {
            throw new ERMgmtIOException("parsing HTTP response exception: " , e);
        }
        
        try {
            // check HTTP response code
            int ret = httpsConnection.getResponseCode();
            if (ret != HttpsURLConnection.HTTP_OK) {
                String mesg = httpsConnection.getResponseMessage();
                if (mesg == null) {
                    mesg = "Unknown HTTP error code(" + ret + ")";
                }

                httpsConnection.disconnect();
                TraceHelper.trace("HTTP error response(" + ret + "): " + mesg);
                throw new ERMgmtHttpNotOKException("HTTP error (" + ret + "): " + mesg);
            }
        
            // get input stream from https connection
            InputStream in = httpsConnection.getInputStream();
        
            // store response in temporary storage
            request.storeResponse(in);

            // close input stream from https connection and disconnect the connection
            in.close();
            httpsConnection.disconnect();
        } catch (ERMgmtHttpNotOKException e) {
            throw new ERMgmtHttpNotOKException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new ERMgmtIOException("processing response: ", e);
        }

            
            InputStream stream;
        try {
            // get a file input stream from response that is in temporary storage 
            stream = new FileInputStream(request.getResponse());
        } catch (Throwable e) {
            throw new ERMgmtIOException("call file not found: ", e);
        }
        
            // convert byte stream to character stream
            InputStreamReader input_stream_reader = new InputStreamReader(stream);
        
            // use buffered io to read character stream
            BufferedReader reader = new BufferedReader(input_stream_reader);

            TraceHelper.trace("Parsing SOAP response");
            soapEnv.parse(reader);

        try {
            // close buffered response stream 
            reader.close();
        } catch(Throwable e) {
            throw new ERMgmtIOException("unable to close response stream: " , e);
        }
                
    }

    /**
     * Create the custom SSLSocketFactory only once, then cache it for reuse.
     * This is so we don't need to create a new SSLSocketFactory for each SOMA
     * call to the device.
     * 
     * @return SSLSocketFactory containing DP cert
     * 
     * @throws General Security Exception
     */
    private SSLSocketFactory getCustomSSLSocketFactory() throws ERMgmtException {
        if (mCachedCustomSSLSocketFactory == null) {
             mCachedCustomSSLSocketFactory = createCustomSSLSocketFactory();
        }
        return (mCachedCustomSSLSocketFactory);
    }
    /**
     * Create the custom SSLSocketFactory to be cached.
     * If a custom certificate has been set, a TrustManager with that certificate is used
     * 
     * @return SSLSocketFactory containing either the Default Certificates and the DP certificate, or an SSLSocketFactory containing only a custom certificate
     * 
     * @throws GeneralSecurityException
     */
    private SSLSocketFactory createCustomSSLSocketFactory() throws ERMgmtException {
        String certificate = mProfile.getCertificate();
        SSLSocketFactory sslSocketFactory = null;
        try{
            TrustManager[] tm = null;
            ERTrustManager myTrustManager = new ERTrustManager();
            tm = new TrustManager[] { myTrustManager };
        
            if(certificate != null){            
                myTrustManager.addCert(certificate);        
            }
        
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, tm, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e){
            throw new ERMgmtException("Could not create the custom socket facotry ", e);
        }

        
        return sslSocketFactory;    
    }
}
