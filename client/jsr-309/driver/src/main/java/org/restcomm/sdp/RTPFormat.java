/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.sdp;

import java.util.Collection;
import javax.sdp.Attribute;

/**
 * Defines payload type of the RTP packet. 
 * 
 * @author kulikov
 */
public interface RTPFormat {

    /**
     * Defines the RTP payload type.
     * 
     * @return and integer payload type number.
     */
    public int getPayloadType();
    
    /**
     * Encodes format objects to SDP attributes.
     * 
     * @return a collection of attributes which represents this format object
     */
    public Collection<Attribute> encode();
}
