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

package org.restcomm.media.rtp.sdp;

import java.text.ParseException;
import java.util.Iterator;

import org.restcomm.media.spi.utils.Text;

/**
 * Connection attribute.
 * 
 * @author kulikov
 * 
 * @deprecated use new /io/sdp library
 */
@Deprecated
public class ConnectionField {

    private Text networkType;
    private Text addressType;
    private Text address;

    /**
     * Reads value from specified text line.
     *
     * @param line the text view of the attribute
     * @throws ParseException
     */
    public void strain(Text line) throws ParseException {
        try {
            Iterator<Text> it = line.split('=').iterator();
            it.next();

            Text token = it.next();
            it = token.split(' ').iterator();

            networkType = it.next();
            networkType.trim();

            addressType = it.next();
            addressType.trim();

            address = it.next();
            address.trim();
        } catch (Exception e) {
            throw new ParseException("Could not parse connection attribute", 0);
        }
    }

    /**
     * Gets the network type value
     * 
     * @return the network type.
     */
    public String getNetworkType() {
        return networkType.toString();
    }

    /**
     * Gets the address type.
     *
     * @return the value of address type
     */
    public String getAddressType() {
        return addressType.toString();
    }

    /**
     * Gets the address value.
     *
     * @return
     */
    public String getAddress() {
        return address.toString();
    }
    
    public void reset() {
        this.networkType = null;
        this.addressType = null;
        this.address = null;
    }

}
