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

package org.restcomm.media.core.rtp.sdp;

import java.text.ParseException;
import java.util.Iterator;

import org.restcomm.media.core.spi.utils.Text;

/**
 * Origin attribute.
 * @author kulikov
 * 
 * @deprecated use new /io/sdp library
 */
public class OriginField {
    private Text name;
    private Text sessionID;
    private Text sessionVersion;
    private Text networkType;
    private Text addressType;
    private Text address;

    protected OriginField() {
    }
    protected OriginField(String name, String sessionID, String sessionVersion, String netType, String addressType, String address) {
        this.name = new Text(name);
        this.sessionID = new Text(sessionID);
        this.sessionVersion = new Text(sessionVersion);
        this.networkType = new Text(netType);
        this.addressType = new Text(addressType);
        this.address = new Text(address);
    }
    /**
     * Reads attribute from text.
     *
     * @param line the text.
     */
    public void strain(Text line) throws ParseException {
        try {
            Iterator<Text> it = line.split('=').iterator();
            it.next();

            Text token = it.next();
            it = token.split(' ').iterator();

            name = it.next();
            name.trim();

            sessionID = it.next();
            sessionID.trim();

            sessionVersion = it.next();
            sessionVersion.trim();

            networkType = it.next();
            networkType.trim();

            addressType = it.next();
            addressType.trim();

            address = it.next();
            address.trim();
        } catch (Exception e) {
            throw new ParseException("Could not parse origin", 0);
        }
    }

    public String getAddress() {
        return address.toString();
    }
    
    public void reset() {
        this.name = null;
        this.sessionID = null;
        this.sessionVersion = null;
        this.networkType = null;
        this.addressType = null;
        this.address = null;
    }

    @Override
    public String toString() {
        return String.format("o=%s %s %s %s %s %s", name, sessionID, sessionVersion,
                networkType, addressType, address);
    }
}


