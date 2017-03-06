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

package org.restcomm.media.server.bootstrap.configuration.main;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Test;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;
import org.restcomm.media.control.mgcp.controller.MgcpController;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.server.bootstrap.main.RestCommMediaServer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestCommMediaServerTest {

    private MediaServer mediaServer;

    @After
    public void after() {
        if (mediaServer != null && mediaServer.isRunning()) {
            mediaServer.stop();
        }
    }

    @Test
    public void testMediaServerStart() {
        // given
        PriorityQueueScheduler mediaScheduler = mock(PriorityQueueScheduler.class);
        Scheduler taskScheduler = mock(ServiceScheduler.class);
        UdpManager udpManager = mock(UdpManager.class);
        ServerManager controller = mock(MgcpController.class);
        mediaServer = new RestCommMediaServer(mediaScheduler, taskScheduler, udpManager, controller);

        // when
        mediaServer.start();

        // then
        assertTrue(mediaServer.isRunning());
        verify(mediaScheduler, times(1)).start();
        verify(taskScheduler, times(1)).start();
        verify(udpManager, times(1)).start();
        verify(controller, times(1)).activate();

        // when
        mediaServer.stop();
        assertFalse(mediaServer.isRunning());
        verify(mediaScheduler, times(1)).stop();
        verify(taskScheduler, times(1)).stop();
        verify(udpManager, times(1)).stop();
        verify(controller, times(1)).deactivate();
    }

}
