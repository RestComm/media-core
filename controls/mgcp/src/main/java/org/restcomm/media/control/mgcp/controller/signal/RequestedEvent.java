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

import org.mobicents.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class RequestedEvent {
    private Text packageName;
    private Text eventName;
    private Text params;
    private Text connectionID;
    
    public void parse(Text descriptor) {
        
    }
    
    public void setPackageName(Text packageName) {
        this.packageName = packageName;
    }
    
    public Text getPackageName() {
        return packageName;
    }

    public void setEventName(Text eventName) {
        this.eventName = eventName;
    }
    
    public Text getEventName() {
        return eventName;
    }
    
    public void setParams(Text params) {
        this.params = params;
    }
    
    public Text getParams() {
        return params;
    }
    
    public Text getConnectionID() {
        return connectionID;
    }
}
