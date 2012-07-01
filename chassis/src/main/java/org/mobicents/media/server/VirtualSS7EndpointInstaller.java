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

package org.mobicents.media.server;

import java.lang.reflect.Constructor;

import org.mobicents.media.server.io.ss7.SS7Manager;
import org.mobicents.media.server.impl.naming.EndpointNameGenerator;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Endpoint installer is used for automatic creation and instalation of endpoints.
 *
 * It uses three parameters: the name pattern, class name and configuration
 * @author oifa yulian
 */
public class VirtualSS7EndpointInstaller extends VirtualEndpointInstaller {

    private SS7Manager ss7Manager;
    private int startChannelID=1;
    private boolean isALaw=true;
    
    /**
     * Creates new endpoint installer.
     */
    public VirtualSS7EndpointInstaller() {        
    }

    /**
    * Gets the pattern used for generating endpoint name.
    *
    * @return text pattern
    */
   public boolean getIsALaw() {
       return isALaw;
   }

   /**
    * Sets the pattern used for generating endpoint name.
    *
    * @param namePattern the pattern text.
    */
   public void setIsALaw(boolean isALaw) {
       this.isALaw = isALaw;
   }
   
    /**
     * Gets the pattern used for generating endpoint name.
     *
     * @return text pattern
     */
    public int getStartChannelID() {
        return startChannelID;
    }

    /**
     * Sets the pattern used for generating endpoint name.
     *
     * @param namePattern the pattern text.
     */
    public void setStartChannelID(int startChannelID) {
        this.startChannelID = startChannelID;
    }
    
    /**
     * Gets the SS7Manager used for generating endpoint name.
     *
     * @return SS7Manager
     */
    public SS7Manager getSS7Manager() {
        return ss7Manager;
    }

    /**
     * Sets the pattern used for generating endpoint name.
     *
     * @param SS7Manager the pattern text.
     */
    public void setSS7Manager(SS7Manager ss7Manager) {
        this.ss7Manager = ss7Manager;
    }
        
    /**
     * (Non Java-doc.)
     *
     * @throws ResourceUnavailableException
     */
    @Override
    public void install() {
    	ClassLoader loader = Server.class.getClassLoader();
        nameParser.setPattern(getNamePattern());
        int index=startChannelID;
        while (nameParser.hasMore()) {
            String name = nameParser.next();
            try {            	
                Constructor constructor = loader.loadClass(this.getEndpointClass()).getConstructor(String.class,SS7Manager.class,int.class,boolean.class);
                BaseSS7EndpointImpl endpoint = (BaseSS7EndpointImpl) constructor.newInstance(name,ss7Manager,index++,isALaw);
                endpoint.setDspFactory(dspFactory);
                endpoint.setLocalConnections(getLocalConnections());
                endpoint.setRtpConnections(getRtpConnections());
                server.install(endpoint);
            } catch (Exception e) {
                server.logger.error("Couldn't instantiate endpoint", e);
            }
        }
    }

    public void uninstall() {
    }

}
