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

package org.mobicents.media.server.mgcp.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import org.mobicents.media.server.utils.Text;

/**
 * Implements MGCP request message.
 * 
 * @author kulikov
 */
public class MgcpRequest extends MgcpMessage {
    //mgcp command
    private Text command = new Text();
    //endpoint 
    private Text endpointID = new Text();
    
    //transaction identifier
    private int txID;
    
    //buffer for parameters
    private ArrayList<Parameter> parameters = new ArrayList(15);
    
    //the actual number of parameters in this message
    private int count;
    
    private final static byte[] affix = "MGCP 1.0\n".getBytes();
    
    private boolean sdpDetected = false;
    private Text sdp = new Text();
    
    private Text msg = new Text();
    
    public MgcpRequest() {
        for (int i = 0; i < 15; i++) {
            parameters.add(new Parameter());
        }
        count = 0;
    }
    
    /**
     * Get the command carried by the message.
     * @return 
     */
    public Text getCommand() {
        return command;
    }
    
    /**
     * Assign command for this message.
     * 
     * @param command the command identifier.
     */
    public void setCommand(Text command) {
        this.command = command;
    }
    
    /**
     * Get endpoint parameter of the request.
     * 
     * @return the fully qualified name of the endpoint.
     */
    public Text getEndpoint() {
        return endpointID;
    }
    
    /**
     * Assign endpoint identifier.
     * 
     * @param endpointID the fully qualified name of the endpoint.
     */
    public void setEndpoint(Text endpointID) {
        this.endpointID = endpointID;
    }
    
    /**
     * Gets the transaction identifier.
     * 
     * @return the integer transaction number.
     */
    public int getTxID() {
        return txID;
    }
    
    /**
     * Assigns transaction identifier.
     * 
     * @param txID transaction identifier.
     */
    public void setTxID(int txID) {
        this.txID = txID;
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
        parameters.get(count).copy(key, value);
        count++;
    }
    
    public void strain(byte[] buff, int offset, int len) {
        this.sdpDetected = false;
        
        msg.strain(buff, offset, len);
        
        //header
        Text header = msg.nextLine();
        Iterator<Text> tokens = header.split(' ').iterator();
        
        //command
        command = tokens.next();
        
        //txID
        txID = tokens.next().toInteger();
        
        //endpoint
        endpointID = tokens.next();
        
        //parameters
        count = 0;
        
        while (msg.hasMoreLines()) {
            if (sdpDetected) {
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
            
            line.divide(':', parameters.get(count).param);
            parameters.get(count).trim();
            
            count++;
        }
        
    }

    @Override
    protected void parse(byte[] buff, int offset, int len) {
        this.strain(buff, offset, len);
    }
    
    @Override
    public void write(ByteBuffer buffer) {
        //prepare buffer
        buffer.clear();
        buffer.rewind();
        
        //write command
        command.write(buffer);
        buffer.put((byte)32);
        
        //write transaction identifier
        new Text(txID).write(buffer);
        buffer.put((byte)32);
        
        //write endpoint identifier
        endpointID.write(buffer);
        buffer.put((byte)32);
        
        //write 'MGCP 1.0'
        buffer.put(affix);
        
        //append parameters
        for (int i = 0; i < count; i++) {
            parameters.get(i).write(buffer);
        }
        
        buffer.flip();
        buffer.rewind();
    }
    
    /**
     * Clean parameters.
     */
    public void clean() {
        this.count = 0;
        this.sdpDetected = false;
    }
    
    @Override
    public String toString() {
        return this.command.toString() +" " + this.endpointID;
    }
}
