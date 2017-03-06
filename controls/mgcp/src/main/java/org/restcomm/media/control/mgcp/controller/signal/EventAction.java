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
package org.restcomm.media.control.mgcp.controller.signal;

import org.restcomm.media.spi.utils.Text;

/**
 * Action associated with event.
 * 
 * @author kulikov
 */
public abstract class EventAction {
    private Text name;
    
    /**
     * Creates new instance of this action.
     * 
     * @param name the name of this action.
     */
    public EventAction(String name) {
        this.name = new Text(name);
    }
    
    /**
     * Gets the name of this action.
     * 
     * @return name of this action.
     */
    public Text getName() {
        return name;
    }
    
    /**
     * Executor method.
     * 
     * @param signal the signal triggered this action
     * @param event the event triggered this action
     * @param options the event options
     */
    public abstract void perform(Signal signal, Event event, Text options);
}
