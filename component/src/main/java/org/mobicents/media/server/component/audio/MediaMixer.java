/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.server.component.audio;

import org.mobicents.media.server.concurrent.ConcurrentMap;

/**
 * Implements compound audio mixer , one of core components of MMS 3.0
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Yulian Oifa
 *
 */
public class MediaMixer {
    
    private final ConcurrentMap<MixerComponent> components;
    
    public MediaMixer() {
        this.components = new ConcurrentMap<MixerComponent>(5);
    }
    
    public void addComponent(MixerComponent component) {
        this.components.putIfAbsent(component.getConnectionId(), component);
    }
    
    public MixerComponent removeComponent(int connectionId) {
        return this.components.remove(connectionId);
    }
    
}
