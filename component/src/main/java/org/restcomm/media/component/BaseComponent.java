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

package org.restcomm.media.component;

import org.restcomm.media.Component;

/**
 *
 * @author kulikov
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public abstract class BaseComponent implements Component {

	private static final long serialVersionUID = 7891529327834578393L;

	//unique identifier of the component
    private final String id;
    
    //the name of the component. 
    //name of the component might be same across many components of same type
    private final String name;
    
    /**
     * Creates new instance of the component.
     * 
     * @param name the name of component.
     */
    public BaseComponent(String name) {
        this.name = name;
        this.id = Long.toHexString(System.nanoTime());
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return name;
    }

}
