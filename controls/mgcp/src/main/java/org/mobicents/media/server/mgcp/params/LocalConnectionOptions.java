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
package org.mobicents.media.server.mgcp.params;

import java.util.Collection;
import org.mobicents.media.server.utils.Text;

/**
 * Represents local connection options parameter
 * 
 * @author kulikov
 */
public class LocalConnectionOptions {
    public final static Text CODECS = new Text("a");
    public final static Text BANDWIDTH = new Text("b");
    public final static Text PACKETIZATION_PERIOD = new Text("p");
    public final static Text TYPE_OF_SERVICE = new Text("t");
    public final static Text ECHO_CANCELATION = new Text("e");
    public final static Text GAIN_CONTROL = new Text("gc");
    public final static Text SILENCE_SUPPRESSION = new Text("s");
    public final static Text RESOURCE_RESERVATION = new Text("r");
    public final static Text ENCRYPTION_KEY = new Text("k");
    public final static Text DTMF_CLAMP = new Text("x-dc");
    public final static Text IS_LOCAL = new Text("x-lo");
    
    public final static Text TRUE = new Text("true");
    
    private Text codecs = new Text(),
            gain = new Text(),
            bandwidth = new Text(),
            packetizationPeriod = new Text(),
            typeOfService = new Text(),
            echoCancelation = new Text(),
            silenceSuppression = new Text(),
            resourceReservation = new Text(),
            encryptionKey = new Text(),
            dtmfclamp=new Text();
    
    private Text keyword = new Text();
    private Text value = new Text();
    private Text[] option = new Text[] {keyword, value};
    
    private boolean isValid = false;
    private boolean isLocal = false;
    
    /**
     * Modifies the value of this parameter.
     * 
     * @param text the text view of this parameter
     */
    public void setValue(Text text) {
    	this.isLocal=false;
        if (text == null) {
            this.isValid = false;
            return;
        }
        
        this.isValid = true;        
        
        Collection<Text> tokens = text.split(',');
        for (Text token: tokens) {
            token.divide(':', option);
            
            if (keyword.equals(CODECS)) {
                value.copy(this.codecs);
            } else if (keyword.equals(BANDWIDTH)) {
                value.copy(this.bandwidth);
            } else if (keyword.equals(PACKETIZATION_PERIOD)) {
                value.copy(this.packetizationPeriod);
            } else if (keyword.equals(TYPE_OF_SERVICE)) {
                value.copy(this.typeOfService);
            } else if (keyword.equals(ECHO_CANCELATION)) {
                value.copy(this.echoCancelation);
            } else if (keyword.equals(GAIN_CONTROL)) {
                value.copy(this.gain);
            } else if (keyword.equals(SILENCE_SUPPRESSION)) {
                value.copy(this.silenceSuppression);
            } else if (keyword.equals(RESOURCE_RESERVATION)) {
                value.copy(this.resourceReservation);
            } else if (keyword.equals(ENCRYPTION_KEY)) {
                value.copy(this.encryptionKey);
            } else if (keyword.equals(DTMF_CLAMP)) {
            	value.copy(this.dtmfclamp);
            } else if (keyword.equals(IS_LOCAL)) {
            	isLocal=true;
            }
        }
    }
    
    public int getGain() {
        return this.isValid ? gain.toInteger() : 0;
    }
    
    public boolean getIsLocal() {
    	return this.isLocal;
    }
    
    public boolean getDtmfClamp() {
    	if(!this.isValid)
    		return false;
    	
    	if(this.dtmfclamp.equals(TRUE))
    		return true;
    	
    	return false;
    }
}
