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

package org.mobicents.media.control.mgcp.pkg;

import java.util.Map;

import org.mobicents.media.control.mgcp.endpoint.MediaGroup;
import org.mobicents.media.control.mgcp.pkg.au.AudioPackage;
import org.mobicents.media.control.mgcp.pkg.au.AudioSignalType;
import org.mobicents.media.control.mgcp.pkg.au.PlayAnnouncement;
import org.mobicents.media.control.mgcp.pkg.au.PlayRecord;
import org.mobicents.media.control.mgcp.pkg.au.pc.PlayCollect;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;
import org.mobicents.media.control.mgcp.pkg.exception.UnsupportedMgcpSignalException;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Provides MGCP signals by package.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSignalProvider {

    private final ListeningScheduledExecutorService executor;

    public MgcpSignalProvider(ListeningScheduledExecutorService executor) {
        super();
        this.executor = executor;
    }

    /**
     * Provides an MGCP Signal to be executed.
     * 
     * @param pkg The package name.
     * @param signal The signal name.
     * @param parameters The parameters that configure the signal
     * @param mediaGroup The media group that holds media components required for signal execution.
     * @return The MGCP signal.
     * @throws UnrecognizedMgcpPackageException When package name is unrecognized.
     * @throws UnsupportedMgcpSignalException When package does not support the specified signal.
     */
    public MgcpSignal provide(String pkg, String signal, Map<String, String> parameters, MediaGroup mediaGroup)
            throws UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        switch (pkg) {
            case AudioPackage.PACKAGE_NAME:
                return provideAudioSignal(signal, parameters, mediaGroup, this.executor);

            default:
                throw new UnrecognizedMgcpPackageException("Unrecognized package " + pkg);
        }
    }

    private MgcpSignal provideAudioSignal(String signal, Map<String, String> parameters, MediaGroup mediaGroup, ListeningScheduledExecutorService executor)
            throws UnsupportedMgcpSignalException {
        // Validate signal type
        AudioSignalType signalType = AudioSignalType.fromSymbol(signal);

        if (signalType == null) {
            throw new UnsupportedMgcpSignalException(
                    "Package " + AudioPackage.PACKAGE_NAME + " does not support signal " + signal);
        }

        switch (signalType) {
            case PLAY_ANNOUNCEMENT:
                return new PlayAnnouncement(mediaGroup.getPlayer(), parameters);

            case PLAY_COLLECT:
                return new PlayCollect(mediaGroup.getPlayer(), mediaGroup.getDetector(), parameters, executor);

            case PLAY_RECORD:
                // TODO provide player and recorder
                return new PlayRecord();

            default:
                throw new IllegalArgumentException("Unsupported audio signal: " + signal);
        }
    }

}
