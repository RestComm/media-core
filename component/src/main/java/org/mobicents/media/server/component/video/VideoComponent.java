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

package org.mobicents.media.server.component.video;

import org.mobicents.media.server.component.InbandComponent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class VideoComponent extends InbandComponent {

    public VideoComponent(int componentId) {
        super(componentId);
    }

    @Override
    public int[] retrieveData() {
        if (isReadable()) {
            int[] data = null;
            // TODO iterate over active inputs and retrieve a frame from each
            // TODO get the data from the frame and store it locally
            // TODO return the data
            return data;
        } else {
            return EMPTY_DATA;
        }
    }

    public void offerData(int[] data) {
        if (isWritable()) {
            // TODO allocate a new frame
            // TODO place the offered data into the frame
            // TODO offer the frame to all active outputs
        }
    }

}
