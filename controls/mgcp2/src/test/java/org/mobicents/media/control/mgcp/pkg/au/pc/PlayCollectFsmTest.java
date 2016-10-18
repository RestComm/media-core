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

package org.mobicents.media.control.mgcp.pkg.au.pc;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.AfterClass;
import org.junit.Test;
import org.mobicents.media.control.mgcp.pkg.MgcpEventObserver;
import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectFsmTest {

    private static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);

    @AfterClass
    public static void cleanup() {
        threadPool.shutdown();
    }

    @Test
    public void testPlayCollectWithEndInputKey() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "5");
        parameters.put("mn", "1");
        parameters.put("ip", "prompt1.wav");
        parameters.put("eik", "#");

        final Player player = mock(Player.class);
        final PlayerListener playerListener = mock(PlayerListener.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final DtmfDetectorListener detectorListener = mock(DtmfDetectorListener.class); 
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollectContext context = new PlayCollectContext(detector, detectorListener, parameters);
        final MgcpEventSubject eventSubject = mock(MgcpEventSubject.class);
        final PlayCollectFsm fsm = PlayCollectFsmBuilder.INSTANCE.build(detector, detectorListener, player, playerListener, eventSubject, executor, context);

        // when
        fsm.start(context);
        fsm.fire(PlayCollectEvent.PROMPT, context);
        fsm.fire(PlayCollectEvent.END_PROMPT, context);
        fsm.fire(DtmfToneEvent.DTMF_1, context);
        fsm.fire(DtmfToneEvent.DTMF_HASH, context);
    }

}
