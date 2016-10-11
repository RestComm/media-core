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

import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.recorder.Recorder;
import org.mobicents.media.server.spi.recorder.RecorderListener;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecordFsmImpl extends AbstractStateMachine<PlayRecordFsm, PlayRecordState, PlayRecordEvent, PlayRecordContext>
        implements PlayRecordFsm {

    // Event Listener
    private final MgcpEventSubject mgcpEventSubject;

    // Media Components
    private final DtmfDetector detector;
    final DtmfDetectorListener detectorListener;

    private final Player player;
    final PlayerListener playerListener;

    private final Recorder recorder;
    final RecorderListener recorderListener;

    // Execution Context
    private final PlayRecordContext context;

    public PlayRecordFsmImpl(MgcpEventSubject mgcpEventSubject, Recorder recorder, RecorderListener recorderListener,
            DtmfDetector detector, DtmfDetectorListener detectorListener, Player player, PlayerListener playerListener,
            PlayRecordContext context) {
        super();
        // Event Listener
        this.mgcpEventSubject = mgcpEventSubject;

        // Media Components
        this.recorder = recorder;
        this.recorderListener = recorderListener;

        this.detector = detector;
        this.detectorListener = detectorListener;

        this.player = player;
        this.playerListener = playerListener;

        // Execution Context
        this.context = context;
    }

}