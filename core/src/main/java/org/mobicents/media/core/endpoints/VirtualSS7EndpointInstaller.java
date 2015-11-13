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

package org.mobicents.media.core.endpoints;

import java.lang.reflect.Constructor;

import org.mobicents.media.core.Server;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Endpoint installer is used for automatic creation and instalation of endpoints.
 *
 * It uses three parameters: the name pattern, class name and configuration
 * @author oifa yulian
 */
public class VirtualSS7EndpointInstaller extends VirtualEndpointInstaller {

    private ChannelsManager channelsManager;
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
    public ChannelsManager getChannelsManager() {
        return channelsManager;
    }

    /**
     * Sets the pattern used for generating endpoint name.
     *
     * @param SS7Manager the pattern text.
     */
    public void setChannelsManager(ChannelsManager channelsManager) {
        this.channelsManager = channelsManager;
    }
        
    /**
     * (Non Java-doc.)
     *
     * @throws ResourceUnavailableException
     */
    @Override
    public void install() {
        int index=startChannelID;
        for(int i=0;i<initialSize;i++) {
        	newEndpoint(index++);                    
        }
    }

    @Override
    public void newEndpoint()
    {    	
    }
    
    private void newEndpoint(int index)
    {
    	ClassLoader loader = Server.class.getClassLoader();
        try {
            Constructor<?> constructor = loader.loadClass(getEndpointClass()).getConstructor(String.class,ChannelsManager.class,int.class,boolean.class);
            BaseSS7EndpointImpl endpoint = (BaseSS7EndpointImpl) constructor.newInstance(getNamePattern() + lastEndpointID.getAndIncrement(),channelsManager,index,isALaw);
            server.install(endpoint,this);
        } catch (Exception e) {
            server.logger.error("Couldn't instantiate endpoint", e);
        }                
    }
    
    @Override
    public boolean canExpand() 
    {
    	return false;
    }
    
    @Override
    public void uninstall() {
    }

}
