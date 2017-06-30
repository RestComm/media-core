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
        
package org.restcomm.media.rtp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.rtp.format.LinearFormat;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.spi.dsp.Processor;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.memory.Frame;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInputTest {

    @Test
    public void testReadWhenJitterBufferFilled() {
        // given
        final long timestamp = 12345L;
        final AudioInput input = mock(AudioInput.class);
        final Processor dsp = mock(Processor.class);
        final Format pcmuFormat = AVProfile.audio.getRTPFormat(0).getFormat();
        final Format linearFormat = LinearFormat.FORMAT;
        final Frame frame = mock(Frame.class);
        final Frame transcodedFrame = mock(Frame.class);
        final JitterBuffer buffer = mock(JitterBuffer.class);
        final Clock clock = new WallClock();
        final PriorityQueueScheduler scheduler = new PriorityQueueScheduler(clock);
        final RtpInput rtpInput = new RtpInput("rtp-input", scheduler, buffer, dsp, input);
        
        when(frame.getFormat()).thenReturn(pcmuFormat);
        when(buffer.read(timestamp)).thenReturn(frame);
        when(dsp.process(frame, pcmuFormat, linearFormat)).thenReturn(transcodedFrame);
        
        // when
        Frame evolved = rtpInput.evolve(timestamp);
        
        // then
        assertNotNull(evolved);
        verify(dsp, times(1)).process(frame, pcmuFormat, linearFormat);
    }

    @Test
    public void testReadWhenJitterBufferEmpty() {
        // given
        final long timestamp = 12345L;
        final AudioInput input = mock(AudioInput.class);
        final Processor dsp = mock(Processor.class);
        final Format pcmuFormat = AVProfile.audio.getRTPFormat(0).getFormat();
        final Format linearFormat = LinearFormat.FORMAT;
        final Frame frame = mock(Frame.class);
        final JitterBuffer buffer = mock(JitterBuffer.class);
        final Clock clock = new WallClock();
        final PriorityQueueScheduler scheduler = new PriorityQueueScheduler(clock);
        final RtpInput rtpInput = new RtpInput("rtp-input", scheduler, buffer, dsp, input);
        
        when(frame.getFormat()).thenReturn(pcmuFormat);
        when(buffer.read(timestamp)).thenReturn(null);
        
        // when
        Frame evolved = rtpInput.evolve(timestamp);
        
        // then
        assertNull(evolved);
        verify(dsp, never()).process(frame, pcmuFormat, linearFormat);
    }

    @Test
    public void testJitterBufferFilledNotification() {
        // given
        final long timestamp = 12345L;
        final AudioInput input = mock(AudioInput.class);
        final Processor dsp = mock(Processor.class);
        final Format pcmuFormat = AVProfile.audio.getRTPFormat(0).getFormat();
        final Frame frame = mock(Frame.class);
        final JitterBuffer buffer = mock(JitterBuffer.class);
        final Clock clock = new WallClock();
        final PriorityQueueScheduler scheduler = new PriorityQueueScheduler(clock);
        final RtpInput rtpInput = spy(new RtpInput("rtp-input", scheduler, buffer, dsp, input));
        
        when(frame.getFormat()).thenReturn(pcmuFormat);
        when(buffer.read(timestamp)).thenReturn(null);
        
        // when
        rtpInput.onJitterBufferEvent(buffer, JitterBufferEvent.BUFFER_FILLED);
        
        // then
        verify(rtpInput).wakeup();
    }
    
}
