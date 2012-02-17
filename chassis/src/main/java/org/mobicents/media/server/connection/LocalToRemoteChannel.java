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

package org.mobicents.media.server.connection;

import org.mobicents.media.CheckPoint;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.utils.Text;
import org.apache.log4j.Logger;

/**
 * Represents the bi-directional transition path for a particular media type stream.
 *
 *
 * @author kulikov
 */
public class LocalToRemoteChannel extends Channel {		
    public LocalToRemoteChannel(BaseConnection connection, Connections connections, MediaType mediaType, Mixer mixer, Splitter splitter) throws Exception {
        super(connection,connections,mediaType,mixer,splitter);                    
    }   
    
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
    	Mode selectedMode=this.mode.get();    	
    	if(selectedMode!=null && selectedMode.getID()==mode)
		{
			//same mode
			return;
		}
    	
    	//setting mode before updating so other threads will see it.
    	Mode newMode=convert(mode);
        this.mode.set(newMode);
        
    	boolean wasNull=false,isNull=false;
    	
        if (selectedMode != null) {  
        	if(mode==ConnectionMode.INACTIVE)
        		//remove from conference if inactive mode
        		connections.removeFromConference(connection);        	
        	selectedMode.deactivate();
        }
        else
        	wasNull=true;                              
        
        if (newMode != null) {
            try {
            	if(wasNull)
                	//if its inactive should not add to conference , if its conference mode itself handles it
                	connections.addToConference(connection);
            	
            	newMode.activate();
            } catch (FormatNotSupportedException e) {
                throw new ModeNotSupportedException(e.getMessage());
            }
        } 
        else
        	isNull=true;
        
        if(!wasNull && !isNull)
    		connections.updateConnectionChannels(connection);
    }
}
