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

package org.mobicents.media.server.impl;

import org.mobicents.media.Component;

/**
 *
 * @author kulikov
 */
public abstract class BaseComponent implements Component {

    //unique identifier of the component
    private String id = null;
    
    //the name of the component. 
    //name of the component might be same accros many components of same type
    private String name = null;
    
    /**
     * Creates new instance of the component.
     * 
     * @param name the name of component.
     */
    public BaseComponent(String name) {
        this.name = name;
        //generate identifier
        this.id = Long.toHexString(System.nanoTime());
    }

    public String getId() {
        return this.id;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#getName(). 
     */
    public String getName() {
        return name;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#reserStats();
     */
    public void reset() {
    }

    /* (non-Javadoc)
     * @see org.mobicents.media.Component#getInterface(java.lang.Class)
     */
    public <T> T getInterface(Class<T> interfaceType) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Cyclic increment function.
     *
     * @param v the value to be incremented.
     * @return the v + 1 or 0 if max value has been reached.
     */
    protected long inc(long v) {
        return v == Long.MAX_VALUE ? 0 : v+1;
    }
}
