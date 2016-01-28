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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

public class Base64
{
    // Globals
    public static final int  DECODE = 0;
    public static final int  ENCODE = 1;

    public static final byte ASCII_SPACE  = 32;
    public static final byte ASCII_EQUALS = 61;
    public static final byte ASCII_TAB    = 9;
    public static final byte ASCII_LF     = 10;
    public static final byte ASCII_FF     = 12;
    public static final byte ASCII_CR     = 13;

    // Source MAP used for Base64 encoding.
    private static final char[] NIB2BASE =
    {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 
        'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 
        'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 
        'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', 
        '8', '9', '+', '/'
    };

    // Destination MAP used for Base64 decoding.
    private static final byte[] BASE2NIB =
    {
          -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
          -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
          -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
          -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
          -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
          -1,   -1,   -1, 0x3E,   -1,   -1,   -1, 0x3F,
        0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B,
        0x3C, 0x3D,   -1,   -1,   -1,   -1,   -1,   -1,
          -1, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
        0x17, 0x18, 0x19,   -1,   -1,   -1,   -1,   -1,
          -1, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20,
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30,
        0x31, 0x32, 0x33,   -1,   -1,   -1,   -1,   -1
    };

    // Prevent Class instantiation.
    private Base64() {}

    // Encoding
    public static String Encode (String in)
    {
        return Encode (in.getBytes());
    }

    public static String Encode (byte[] in)
    {
        byte[] outb = new byte[(in.length*4+3)/3+2];
        
        int i = 0, j = 0;
        for (; i < in.length - 2; i += 3, j+= 4)
        {
            Encode (in, i, 3, outb, j);
        }

        if (i < in.length)
        {
            Encode (in, i, in.length-i, outb, j);
            j+=4;
        }
        return new String (outb, 0, j);
    }

    private static void Encode (byte[] inb, int ino, int len, byte[] outb, int outo)
    {
        // shift left by 24 to remove the 1's from java treating the value
        // as negative when casting from byte to int
        int inBuff =  (len > 0 ? ((inb[ino] << 24) >>> 8) : 0)
                    | (len > 1 ? ((inb[ino + 1] << 24) >>> 16) : 0)
                    | (len > 2 ? ((inb[ino + 2] << 24) >>> 24) : 0);

        if (len == 1) {
            outb[outo + 0] = (byte) NIB2BASE[(inBuff >>> 18)];
            outb[outo + 1] = (byte) NIB2BASE[(inBuff >>> 12) & 0x3f];
            outb[outo + 2] = ASCII_EQUALS;
            outb[outo + 3] = ASCII_EQUALS;
        } else if (len == 2) {
            outb[outo + 0] = (byte) NIB2BASE[(inBuff >>> 18)];
            outb[outo + 1] = (byte) NIB2BASE[(inBuff >>> 12) & 0x3f];
            outb[outo + 2] = (byte) NIB2BASE[(inBuff >>> 6) & 0x3f];
            outb[outo + 3] = ASCII_EQUALS;
        } else {
            outb[outo + 0] = (byte) NIB2BASE[(inBuff >>> 18)];
            outb[outo + 1] = (byte) NIB2BASE[(inBuff >>> 12) & 0x3f];
            outb[outo + 2] = (byte) NIB2BASE[(inBuff >>> 6) & 0x3f];
            outb[outo + 3] = (byte) NIB2BASE[(inBuff & 0x3f)];
        }
    }

    // Decoding
    public static byte[] Decode (String in)
    {
        return Decode (in.getBytes());
    }

    public static byte[] Decode (byte[] in)
    {
        byte[] outb = new byte[in.length*3/4];
        byte[] decode = new byte[4];

        int i = 0, j = 0, k = 0;
        while (i < in.length)
        {
            // Skip white space.
            for (k = 0; k < 4 && i < in.length; i++)
            {
                if (!isspace(in[i])) 
                    decode [k++] = in[i];
            }
            
            if (k == 4)
                j += Decode (decode, 0, outb, j);
        }

        byte[] out = new byte[j];
        System.arraycopy (outb, 0, out, 0, j);
        return out;
    }

    private static int Decode (byte[] inb, int ino, byte[] outb, int outo)
    {
        int j = 0;
        byte t1 = BASE2NIB[inb[ino+0] & 0x7F];
        byte t2 = BASE2NIB[inb[ino+1] & 0x7F];
        outb[outo+j++] = (byte) ((t1 << 2) | (t2 >> 4));

        if (inb[ino+2] != ASCII_EQUALS)
        {
            t1 = BASE2NIB[inb[ino+2] & 0x7F];
            outb[outo+j++] = (byte) ((t2 << 4) | (t1 >> 2));

            if (inb[ino+3] != ASCII_EQUALS)
            {
                t2 = BASE2NIB[inb[ino+3] & 0x7F];
                outb[outo+j++] = (byte) ((t1 << 6) | t2);
            }
        }
        return j;
    }

    private static boolean isspace (byte ch)
    {
        return ((ch == ASCII_SPACE)  ||
                (ch == ASCII_TAB)    ||
                (ch == ASCII_LF)     ||
                (ch == ASCII_FF)     ||
                (ch == ASCII_CR));
    }

    public static class InputStream extends FilterInputStream
    {
        private byte[] mInBuffer;
        private byte[] mOutBuffer;
        private int    mBuflen;
        private int    mIndex;
        private int    mType;

        public InputStream (java.io.InputStream in)
        {
            this (in, DECODE);
        }

        public InputStream (java.io.InputStream in, int type)
        {
            super (in);
            mBuflen = 0;
            mIndex  = 0;
            mInBuffer  = new byte[4];
            mOutBuffer = new byte[4];
            mType = type;
        }

        public int read () throws IOException
        {
            if (mIndex >= mBuflen)
            {
                mIndex = 0;
                mBuflen = 0;

                if (mType == DECODE)
                {
                    // Skip white space.
                    for (int k = 0; k < 4;)
                    {
                        int ch = in.read ();
                        if (ch == -1) return -1;
                        
                        if (!isspace ((byte)ch))
                            mInBuffer[k++] = (byte) ch;
                    }
                    
                    mBuflen = Decode (mInBuffer, 0, mOutBuffer, 0);
                }

                if (mType == ENCODE)
                {
                    int k = 0;
                    while (k < 3)
                    {
                        int ch = in.read ();
                        if (ch == -1) break;
                        
                        mInBuffer[k++] = (byte) ch;
                    }

                    if (k > 0)
                    {
                        Encode (mInBuffer, 0, k, mOutBuffer, 0);
                        mBuflen = 4;
                    }
                    else
                        return -1;
                }
            }
            return mOutBuffer[mIndex++] & 0xFF;
        }

        public int read (byte[] buffer, int off, int len) throws IOException
        {
            try
            {
                int l = 0;
                for (; l < len; l++)
                {
                    int ch = read ();
                    if (ch == -1)
                    {
                        if (l == 0) return -1;
                        else break;
                    }
                    buffer[off+l] = (byte)ch;
                }
                return l;
            }
            catch (IOException ioe)
            {
                return -1;
            }
        }

        public int available () throws IOException 
        {
            return (in.available()*3)/4+(mBuflen-mIndex);
        }
    }

    public static class OutputStream extends FilterOutputStream
    {
        private byte[] mInBuffer;
        private byte[] mOutBuffer;
        private int mBuflen;
        private int mType;

        public OutputStream (java.io.OutputStream out)
        {
            this (out, ENCODE);
        }

        public OutputStream (java.io.OutputStream out, int type)
        {
            super (out);
            mBuflen = 0;
            mInBuffer = new byte[4];
            mOutBuffer = new byte[4];
            mType = type;
        }

        public void write (int ch) throws IOException
        {
            mInBuffer[mBuflen++] = (byte)ch;
            if (mType == ENCODE && mBuflen == 3)
            {
                Encode (mInBuffer, 0, mBuflen, mOutBuffer, 0);
                mBuflen = 0;
                out.write (mOutBuffer, 0, 4);
            }

            if (mType == DECODE && mBuflen == 4)
            {
                int rv = Decode (mInBuffer, 0, mOutBuffer, 0);
                mBuflen = 0;
                out.write (mOutBuffer, 0, rv);
            }
        }

        public void write (byte[] buffer) throws IOException 
        {
            write (buffer, 0, buffer.length);
        }

        public void write (byte[] buffer, int off, int len) throws IOException 
        {
            for (int i = 0; i < len; i++)                 
                write (buffer[off+i]);
        }

        public void flush () throws IOException 
        {
            if (mType == ENCODE && mBuflen > 0)
            {
                Encode (mInBuffer, 0, mBuflen, mOutBuffer, 0);
                mBuflen = 0;
                out.write (mOutBuffer, 0, 4);
            }

            if (mType == DECODE && mBuflen == 4)
            {
                int rv = Decode (mInBuffer, 0, mOutBuffer, 0);
                mBuflen = 0;
                out.write (mOutBuffer, 0, rv);
            }
            out.flush ();
        }

        public void close () throws IOException
        {
            flush ();
            out.close ();
        }
    }

    /*
    public static void main (String args[])
    {
        System.out.println ("Hello");

        System.out.println (Base64.Encode ("Hello World"));
        System.out.println (Base64.Encode ("Hello World1"));
        System.out.println (Base64.Encode ("Hello World12"));
        System.out.println (Base64.Encode ("Hello World123"));
        System.out.println (Base64.Encode ("Hello World1234"));

        byte[] l1 = Base64.Decode (Base64.Encode ("Hello World"));
        byte[] l2 = Base64.Decode (Base64.Encode ("Hello World1"));
        byte[] l3 = Base64.Decode (Base64.Encode ("Hello World12"));
        byte[] l4 = Base64.Decode (Base64.Encode ("Hello World123"));
        byte[] l5 = Base64.Decode (Base64.Encode ("Hello World1234"));

        System.out.println (new String (l1));
        System.out.println (new String (l2));
        System.out.println (new String (l3));
        System.out.println (new String (l4));
        System.out.println (new String (l5));

        l1 = Base64.Decode ("S\nGV\nsb\nG8\ngV\n29\nyb\nGQ\n=");
        l2 = Base64.Decode ("S\nG\nV\ns\nb\nG\n8\ng\nV\n2\n9\ny\nb\nG\nQ\nx\n");
        l3 = Base64.Decode ("\nSGV\nsbG\n8gV29\nybGQ\nxMg==\n");
        l4 = Base64.Decode ("\nS\nGVsbG8\ngV29ybGQxM\njM\n=");
        l5 = Base64.Decode ("SGVsbG8gV29ybGQxMj\nM\n0\n");

        System.out.println (new String (l1));
        System.out.println (new String (l2));
        System.out.println (new String (l3));
        System.out.println (new String (l4));
        System.out.println (new String (l5));

        try {

        // input stream (encode)
        FileInputStream inStream = new FileInputStream("Base64.java");
        Base64.InputStream base64in =
            new Base64.InputStream(inStream, Base64.ENCODE);
        
        FileOutputStream outStream = new FileOutputStream("temp_1");
        Base64.OutputStream base64out =
            new Base64.OutputStream(outStream, Base64.DECODE);

        int bytes;
        byte[] buffer = new byte[4096];
        while ((bytes = base64in.read(buffer)) != -1) 
        {
            base64out.write(buffer, 0, bytes);
        }

        base64out.flush();
        base64out.close();
        base64in.close();

        inStream = new FileInputStream("temp_2");
        base64in = new Base64.InputStream(inStream, Base64.DECODE);

        outStream = new FileOutputStream("temp_3");
        base64out = new Base64.OutputStream(outStream, Base64.ENCODE);

        while ((bytes = base64in.read(buffer)) != -1) 
        {
            base64out.write(buffer, 0, bytes);
        }

        base64out.flush();
        base64out.close();
        base64in.close();
        }
        catch (IOException ioe)
        {
            return;
        }
    }
    */
}
