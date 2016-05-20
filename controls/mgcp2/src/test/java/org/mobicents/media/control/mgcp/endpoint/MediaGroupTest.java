/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.endpoint;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioInput;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaGroupTest {

    @Test
    public void testAddRemoveInbandInputResource() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        final Component component = mock(AudioPlayerImpl.class); // XXX Forced to use concrete class here!

        // when - add component
        mediaGroup.addComponent(component);

        // then
        verify(inbandComponent, times(1)).addInput(any(AudioInput.class));

        // when - retrieve component
        Component registered = mediaGroup.getComponent(ComponentType.PLAYER);
        Component unregistered = mediaGroup.getComponent(ComponentType.RECORDER);

        // then
        assertNotNull(registered);
        assertNull(unregistered);
        assertTrue(registered instanceof AudioPlayerImpl);

        // when - remove component
        Component removed = mediaGroup.removeComponent(ComponentType.PLAYER);

        // then
        assertEquals(component, removed);
        assertNull(mediaGroup.removeComponent(ComponentType.PLAYER));
        verify(inbandComponent, times(1)).remove(any(AudioInput.class));
    }

}
