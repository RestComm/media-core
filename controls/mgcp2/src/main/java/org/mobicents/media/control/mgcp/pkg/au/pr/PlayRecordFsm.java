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

import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface PlayRecordFsm extends StateMachine<PlayRecordFsm, PlayRecordState, PlayRecordEvent, PlayRecordContext> {

    void enterActive(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void exitActive(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterLoadingPlaylist(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterPrompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void onPrompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void exitPrompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterPrompted(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterCollecting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void onCollecting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void exitCollecting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterCollected(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterRecording(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void onRecording(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void exitRecording(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

    void enterRecorded(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context);

}
