/*
 * Mobicents, Communications Middleware
 * 
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party
 * contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 *
 * Boston, MA  02110-1301  USA
 */
package org.mobicents.sdp;

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
