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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Test;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioInput;
import org.mobicents.media.server.component.audio.AudioOutput;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaGroupTest {

    @Test
    public void testAddRemovePlayer() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component = mock(AudioPlayerImpl.class);

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

    @Test
    public void testAddRemoveRecorder() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component = mock(AudioRecorderImpl.class);

        // when - add component
        mediaGroup.addComponent(component);

        // then
        verify(inbandComponent, times(1)).addOutput(any(AudioOutput.class));

        // when - retrieve component
        Component registered = mediaGroup.getComponent(ComponentType.RECORDER);
        Component unregistered = mediaGroup.getComponent(ComponentType.PLAYER);

        // then
        assertNotNull(registered);
        assertNull(unregistered);
        assertTrue(registered instanceof AudioRecorderImpl);

        // when - remove component
        Component removed = mediaGroup.removeComponent(ComponentType.RECORDER);

        // then
        assertEquals(component, removed);
        assertNull(mediaGroup.removeComponent(ComponentType.RECORDER));
        verify(inbandComponent, times(1)).remove(any(AudioOutput.class));
    }

    @Test
    public void testAddRemoveDtmfDetector() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component = mock(DetectorImpl.class);

        // when - add component
        mediaGroup.addComponent(component);

        // then
        verify(inbandComponent, times(1)).addOutput(any(AudioOutput.class));
        verify(outbandComponent, times(1)).addOutput(any(OOBOutput.class));

        // when - retrieve component
        Component registered = mediaGroup.getComponent(ComponentType.DTMF_DETECTOR);
        Component unregistered = mediaGroup.getComponent(ComponentType.PLAYER);

        // then
        assertNotNull(registered);
        assertNull(unregistered);
        assertTrue(registered instanceof DetectorImpl);

        // when - remove component
        Component removed = mediaGroup.removeComponent(ComponentType.DTMF_DETECTOR);

        // then
        assertEquals(component, removed);
        assertNull(mediaGroup.removeComponent(ComponentType.DTMF_DETECTOR));
        verify(inbandComponent, times(1)).remove(any(AudioOutput.class));
        verify(outbandComponent, times(1)).remove(any(OOBOutput.class));
    }

    @Test
    public void testAddRemoveDtmfGenerator() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component = mock(GeneratorImpl.class);

        // when - add component
        mediaGroup.addComponent(component);

        // then
        verify(inbandComponent, times(1)).addInput(any(AudioInput.class));
        verify(outbandComponent, times(1)).addInput(any(OOBInput.class));

        // when - retrieve component
        Component registered = mediaGroup.getComponent(ComponentType.DTMF_GENERATOR);
        Component unregistered = mediaGroup.getComponent(ComponentType.PLAYER);

        // then
        assertNotNull(registered);
        assertNull(unregistered);
        assertTrue(registered instanceof GeneratorImpl);

        // when - remove component
        Component removed = mediaGroup.removeComponent(ComponentType.DTMF_GENERATOR);

        // then
        assertEquals(component, removed);
        assertNull(mediaGroup.removeComponent(ComponentType.DTMF_GENERATOR));
        verify(inbandComponent, times(1)).remove(any(AudioInput.class));
        verify(outbandComponent, times(1)).remove(any(OOBInput.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDuplicateComponent() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component1 = mock(AudioPlayerImpl.class);
        final Component component2 = mock(AudioPlayerImpl.class);

        // when - add component
        mediaGroup.addComponent(component1);

        // then
        verify(inbandComponent, times(1)).addInput(any(AudioInput.class));

        // when - add duplicate component type
        mediaGroup.addComponent(component2);
    }

    @Test
    public void testRemoveComponents() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component1 = mock(AudioPlayerImpl.class);
        final Component component2 = mock(AudioRecorderImpl.class);
        final Component component3 = mock(DetectorImpl.class);
        final Component component4 = mock(GeneratorImpl.class);

        // when - add components
        mediaGroup.addComponent(component1);
        mediaGroup.addComponent(component2);
        mediaGroup.addComponent(component3);
        mediaGroup.addComponent(component4);

        // then
        assertEquals(component1, mediaGroup.getComponent(ComponentType.PLAYER));
        assertEquals(component2, mediaGroup.getComponent(ComponentType.RECORDER));
        assertEquals(component3, mediaGroup.getComponent(ComponentType.DTMF_DETECTOR));
        assertEquals(component4, mediaGroup.getComponent(ComponentType.DTMF_GENERATOR));

        // when - remove components
        List<Component> removed = mediaGroup.removeComponents();

        // then
        assertEquals(4, removed.size());
        assertTrue(removed.contains(component1));
        assertTrue(removed.contains(component2));
        assertTrue(removed.contains(component3));
        assertTrue(removed.contains(component4));
        assertNull(mediaGroup.getComponent(ComponentType.PLAYER));
        assertNull(mediaGroup.getComponent(ComponentType.RECORDER));
        assertNull(mediaGroup.getComponent(ComponentType.DTMF_DETECTOR));
        assertNull(mediaGroup.getComponent(ComponentType.DTMF_GENERATOR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedComponent() {
        // given
        final AudioComponent inbandComponent = mock(AudioComponent.class);
        final OOBComponent outbandComponent = mock(OOBComponent.class);
        final MediaGroup mediaGroup = new MediaGroup(inbandComponent, outbandComponent);
        // XXX Forced to use concrete class here!
        final Component component = mock(Sine.class);

        // when - add component
        mediaGroup.addComponent(component);
    }

}
