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

package org.restcomm.media;

import org.restcomm.media.spi.Connection;

/**
 * Acts as a factory of any media components.
 * 
 * @author yulian oifa
 */
public interface ComponentFactory {
    /**
     * Constructs new component.
     * 
     * @param componentType - type of component to be created
     * @return new instance of the component.
     */
    public Component newAudioComponent(ComponentType componentType);
    
    /**
     * Frees previously allocated component component.
     * 
     * @param component - component to release
     * @param componentType - type of component to be created
     * 
     */
    public void releaseAudioComponent(Component component,ComponentType componentType);
    
    /**
     * Constructs new connection.
     * 
     * @param isLocal - created connection should be local or remote
     * @return new instance of the connection.
     */
    public Connection newConnection(boolean isLocal);
    
    /**
     * Releases new connection.
     * 
     * @param connection - connection to release
     * @param isLocal - created connection should be local or remote 
     */
    public void releaseConnection(Connection connection,boolean isLocal);
}
