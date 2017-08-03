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
package org.restcomm.media.network.deprecated;

/**
 * Defines relation between audio/video format and RTP payload number as
 * specified by Audio/Video Profile spec.
 * 
 * @author Oleg Kulikov
 */
public enum IPAddressType {

    IPV4(0),IPV6(1),INVALID(2);
    
    private int value;
    
    IPAddressType(int value)
    {
    	this.value=value;
    }
    
    public int getValue()
    {
    	return value;
    }
}
