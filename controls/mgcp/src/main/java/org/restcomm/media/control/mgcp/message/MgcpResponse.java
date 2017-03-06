/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.restcomm.media.spi.utils.Text;

/**
 * MGCP response message.
 * 
 * @author Oifa Yulian
 */
public class MgcpResponse extends MgcpMessage {
    private int responseCode;
    private int txID;
    private Text responseString;
    
    //buffer for parameters
    private ArrayList<Parameter> parameters = new ArrayList<Parameter>(15);
    //the actual number of parameters in this message
    private int count;
    
    private boolean sdpDetected = false;
    private Text sdp;
    
    private Text msg = new Text();
    
    private static final Logger logger = Logger.getLogger(MgcpResponse.class);
    
    public MgcpResponse() {
        for (int i = 0; i < 15; i++) {
            parameters.add(new Parameter());
        }
        count = 0;
    }
    
    public int getResponseCode() {
        return responseCode;
    }
    
    public void setResponseCode(int code) {
        this.responseCode = code;
    }
    
    public int getTxID() {
        return txID;
    }
    
    public void setTxID(int txID) {
        this.txID = txID;
    }
    
    
    public Text getResponseString() {
        return responseString;
    }
    
    public void setResponseString(Text text) {
        this.responseString = text;
    }
    
    /**
     * Gets the requested parameter.
     * 
     * @param key the key identifier of the parameter
     * @return 
     */
    public Parameter getParameter(Text key) {
        for (int i = 0; i < count; i++) {
            if (parameters.get(i).getKey().equals(key)) {
                return parameters.get(i);
            }
        }
        return null;
    }

    /**
     * Assigns parameter.
     * 
     * @param p the parameter.
     */
    public void setParameter(Text key, Text value) {        
        if (key.equals(Parameter.SDP)) {
            sdp = value;
            return;
        }
        
        parameters.get(count).copy(key, value);
        count++;
    }
    
    public void strain(byte[] buff, int offset, int len) {
        msg.strain(buff, offset, len);
        
        //header
        Text header = msg.nextLine();
        Iterator<Text> tokens = header.split(' ').iterator();

        int off = offset;
        int length = header.length();
        
        //command
        Text t = tokens.next();
        
        off += t.length();
        responseCode = t.toInteger();
        
        //txID
        t = tokens.next();
        
        off += t.length();
        txID = t.toInteger();
        
        off += 2;
        
        //endpoint
        responseString = new Text();
        responseString.strain(buff, off, length - off);
        //parameters
        count = 0;
        
        int subItemsCount;        
        while (msg.hasMoreLines()) {
            if (sdpDetected) {
                sdp = new Text();
                msg.copyRemainder(sdp);
                parameters.get(count).copy(Parameter.SDP, sdp);
                count++;
                return;
            }
            
            Text line = msg.nextLine();
            
            if (line.length() == 0) {
                this.sdpDetected = true;
                continue;
            }
            
            subItemsCount=line.divide(':', parameters.get(count).param);
            if(subItemsCount==2)
            {
            	parameters.get(count).trim();            
            	count++;
            }
        }        
    }

    @Override
    public void parse(byte[] buff, int offset, int len) {
        try {
            this.strain(buff, offset, len);
        } catch (Exception e) {
        	logger.error(e);
        }
    }
    
    @Override
    public void write(ByteBuffer buffer) {
        buffer.clear();
        buffer.rewind();
        
        //writting response code
        new Text(responseCode).write(buffer);
        buffer.put((byte)32);
        
        //writting transaction id
        new Text(txID).write(buffer);
        buffer.put((byte)32);
        
        //writting response string
        responseString.write(buffer);
        buffer.put((byte)10);
        
        //writting parameters
        for (int i = 0; i < count; i++) {
            parameters.get(i).write(buffer);
        }
        
        if (sdp != null) {
            buffer.put((byte)10);
            sdp.write(buffer);
        }
        
        buffer.flip();
        buffer.rewind();
    }
    
    public void reset() {
        sdp = null;
    }
    
    /**
     * Clean parameters.
     */
    public void clean() {
        sdp = null;
        sdpDetected = false;
        this.count = 0;
    }
    
    @Override
    public String toString() {
        return this.responseCode + " (" + responseString + ")";
    }
    
}
