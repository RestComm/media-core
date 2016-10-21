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

package org.mobicents.media.control.mgcp.pkg.au.pr;

import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.junit.Test;
import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.recorder.Recorder;
import org.mobicents.media.server.spi.recorder.RecorderListener;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecordFsmTest {

    @Test
    public void testPromptCollect() {
        // given
        final HashMap<String, String> parameters = new HashMap<>(5);
        final MgcpEventSubject mgcpEventSubject = mock(MgcpEventSubject.class);
        final Recorder recorder = mock(Recorder.class);
        final RecorderListener recorderListener = mock(RecorderListener.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final DtmfDetectorListener detectorListener = mock(DtmfDetectorListener.class);
        final Player player = mock(Player.class);
        final PlayerListener playerListener = mock(PlayerListener.class);
        final PlayRecordContext context = new PlayRecordContext(parameters);
        final PlayRecordFsm fsm = PlayRecordFsmBuilder.INSTANCE.build(mgcpEventSubject, recorder, recorderListener, detector, detectorListener, player, playerListener, context);

        // when
        fsm.start();
        fsm.fire(PlayRecordEvent.NEXT_TRACK, context);
        fsm.fire(PlayRecordEvent.NEXT_TRACK, context);
        fsm.fire(PlayRecordEvent.NEXT_TRACK, context);
        fsm.fire(PlayRecordEvent.NEXT_TRACK, context);
        fsm.fire(PlayRecordEvent.END_COLLECT, context);
        fsm.fire(PlayRecordEvent.PROMPT_END, context);
    }

    @Test
    public void testPromptWhenTimeout() {
        // given
        final HashMap<String, String> parameters = new HashMap<>(5);
        final MgcpEventSubject mgcpEventSubject = mock(MgcpEventSubject.class);
        final Recorder recorder = mock(Recorder.class);
        final RecorderListener recorderListener = mock(RecorderListener.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final DtmfDetectorListener detectorListener = mock(DtmfDetectorListener.class);
        final Player player = mock(Player.class);
        final PlayerListener playerListener = mock(PlayerListener.class);
        final PlayRecordContext context = new PlayRecordContext(parameters);
        final PlayRecordFsm fsm = PlayRecordFsmBuilder.INSTANCE.build(mgcpEventSubject, recorder, recorderListener, detector, detectorListener, player, playerListener, context);
        
        // when
        fsm.start();
        fsm.fire(PlayRecordEvent.PROMPT_END, context);
        // fsm.fire(PlayRecordEvent.END_COLLECT, context);
        fsm.fire(PlayRecordEvent.TIMEOUT, context);
    }

}
