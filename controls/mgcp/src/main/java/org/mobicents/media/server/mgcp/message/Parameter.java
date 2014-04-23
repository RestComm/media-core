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
import org.mobicents.media.server.utils.Text;

/**
 * MGCP parameter value
 * 
 * @author kulikov
 */
public class Parameter {
    public final static Text CALL_ID = new Text("C");
    public final static Text MODE = new Text("M");
    public final static Text SECOND_ENDPOINT = new Text("Z2");
    public final static Text SDP = new Text("sdp");
    public final static Text CONNECTION_ID = new Text("I");
    public final static Text CONNECTION_ID2 = new Text("I2");
    public final static Text ENDPOINT_ID = new Text("Z");
    public final static Text REQUEST_ID = new Text("X");
    public final static Text REQUESTED_EVENTS = new Text("R");
    public final static Text REQUESTED_SIGNALS = new Text("S");
    public final static Text NOTIFIED_ENTITY= new Text("N");
    public final static Text OBSERVED_EVENT= new Text("O");
    public final static Text CONNECTION_PARAMETERS= new Text("P");
    public final static Text LOCAL_CONNECTION_OPTIONS= new Text("L");
    public final static Text REASON_CODE = new Text("E");
    public final static Text BARER_INFORMATION = new Text("B");
    public final static Text REQUESTED_INFO = new Text("F");
    public final static Text REMOTE_CONNECTION_DESCRIPTION = new Text("RC");
    public final static Text LOCAL_CONNECTION_DESCRIPTION = new Text("LC");
    // hrosa - used by AUCX to query connection availability. Not standard!
    public final static Text CONNECTION_AVAILABILITY = new Text("AV");
    
    private Text key = new Text();
    private Text value = new Text();
    
    protected Text[] param = new Text[]{key, value};
    
    protected Parameter() {
    }
    
    public Text getKey() {
        return key;
    }
    
    public Text getValue() {
        return value;
    }
    
    protected void trim() {
        key.trim();
        value.trim();
    }
    
    protected void copy(Text key, Text value) {
        key.copy(this.key);
        value.copy(this.value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
    
    public int toInteger() {
        return value.toInteger();
    }
    
    public void write(ByteBuffer buffer) {
        key.write(buffer);
        buffer.put((byte)58);
        value.write(buffer);
        buffer.put((byte)10);
    }
}
