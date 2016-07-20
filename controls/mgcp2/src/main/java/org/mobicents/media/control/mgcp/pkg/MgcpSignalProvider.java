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

import org.mobicents.media.control.mgcp.pkg.au.AudioPackage;
import org.mobicents.media.control.mgcp.pkg.au.AudioSignalType;
import org.mobicents.media.control.mgcp.pkg.au.PlayAnnouncement;
import org.mobicents.media.control.mgcp.pkg.au.PlayCollect;
import org.mobicents.media.control.mgcp.pkg.au.PlayRecord;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;
import org.mobicents.media.control.mgcp.pkg.exception.UnsupportedMgcpSignalException;

/**
 * Provides MGCP signals by package.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSignalProvider {

    /**
     * Provides an MGCP Signal to be executed.
     * 
     * @param pkg The package name.
     * @param signal The signal name.
     * @return The MGCP signal.
     * @throws UnrecognizedMgcpPackageException When package name is unrecognized.
     * @throws UnsupportedMgcpSignalException When package does not support the specified signal.
     */
    public static MgcpSignal provide(String pkg, String signal)
            throws UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        switch (pkg) {
            case AudioPackage.PACKAGE_NAME:
                return provideAudioSignal(signal);

            default:
                throw new UnrecognizedMgcpPackageException("Unrecognized package " + pkg);
        }
    }

    private static MgcpSignal provideAudioSignal(String signal) throws UnsupportedMgcpSignalException {
        // Validate signal type
        AudioSignalType signalType = AudioSignalType.fromSymbol(signal);

        if (signalType == null) {
            throw new UnsupportedMgcpSignalException("Package " + AudioPackage.PACKAGE_NAME + " does not support signal " + signal);
        }

        switch (signalType) {
            case PLAY_ANNOUNCEMENT:
                return new PlayAnnouncement();

            case PLAY_COLLECT:
                return new PlayCollect();

            case PLAY_RECORD:
                return new PlayRecord();

            default:
                throw new IllegalArgumentException("Unsupported audio signal: " + signal);
        }
    }

}
