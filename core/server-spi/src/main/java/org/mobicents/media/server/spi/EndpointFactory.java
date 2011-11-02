/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.mobicents.media.server.spi;

import java.util.Collection;
import org.mobicents.media.server.spi.rtp.RtpManager;

/**
 * Defines interface of endpoint factory.
 * 
 * 
 * @author kulikov
 */
public interface EndpointFactory { 
    
    /**
     * Assigns RTP manager
     * 
     * @param rtpManager the RTP manager instance.
     */
    public void setRtpManager(RtpManager rtpManager);
    
    /**
     * Installs factory.
     */
    public Collection<Endpoint> install() throws ResourceUnavailableException;
    
    /**
     * Uninstalls factory.
     */
    public void uninstall();
}
