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

import java.io.Serializable;

/**
 * <i>Component</i> is an Object that is responsible for any media data processing.
 * 
 * Examples of components are the audio player, recoder, DTMF detector, etc. The <code>Component</code> is a supper class for
 * all media processing components.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public interface Component extends Serializable {

    /**
     * Gets the unique identifier of this component.
     * 
     * @return
     */
    public String getId();

    /**
     * Gets the name of the component. The component of same type can share same name.
     * 
     * @return name of this component;
     */
    public String getName();

    /**
     * Resets component to its original state. This methods cleans transmission statistics and any assigned formats
     */
    public void reset();

    /**
     * Activates component
     * 
     */
    public void activate();

    /**
     * Deactivates component
     * 
     */
    public void deactivate();

}
