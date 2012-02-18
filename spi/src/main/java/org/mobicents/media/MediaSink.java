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

package org.mobicents.media;

import org.mobicents.media.server.spi.io.Pipe;


/**
 * Implements the media consumer.
 * 
 * @author Oleg Kulikov
 * @author baranowb
 */
public interface MediaSink extends Component {

    /**
     * Starts media processing.
     */
    public void start();
    
    /**
     * Terminates media processing.
     */
    public void stop();
    
    
    /**
     * Joins this media sink with media source.
     * The concrete media sink can allow to join with multiple sources
     * 
     * @param source the media source to join with.
     */
    public void connect(Pipe pipe);
    
    /**
     * Breaks connection with media source.
     * The concrete media sink can allow to join with multiple sources so
     * this method requires the explicit source for disconnection.
     * 
     * @param source the source to disconnect from.
     */
    public void disconnect(Pipe pipe);
    
    /**
     * Gets the state of the component.
     * 
     * @return  true if component is connected to other component.
     */
    public boolean isConnected();
    
    /**
     * Gets true if component is able to receive media.
     * 
     * @return true if component is able to receive media.
     */
    public boolean isStarted();
    
    /**
     * Shows the number of packets received by this medis sink since last start.
     * 
     * @return the number of packets.
     */
    public long getPacketsReceived();
    
    /**
     * Shows the number of bytes received by this sink since last start;
     * 
     * @return the number of bytes.
     */
    public long getBytesReceived();
    
	
}
