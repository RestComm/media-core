/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.endpoint.mixer;

import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MediaGroup;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointContext;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpointContext extends MgcpEndpointContext {

    private final AudioMixer mixer;
    private final OOBMixer oobMixer;

    public MgcpMixerEndpointContext(EndpointIdentifier endpointId, MediaGroup mediaGroup, NotificationCenter notificationCenter, AudioMixer mixer, OOBMixer oobMixer) {
        super(endpointId, mediaGroup, notificationCenter);
        this.mixer = mixer;
        this.oobMixer = oobMixer;
    }

    public AudioMixer getMixer() {
        return mixer;
    }

    public OOBMixer getOobMixer() {
        return oobMixer;
    }

}
