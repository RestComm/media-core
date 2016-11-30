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

package org.mobicents.media.control.mgcp.pkg.au.pc2;

import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface PlayCollectFsm extends StateMachine<PlayCollectFsm, PlayCollectState, PlayCollectEvent, PlayCollectContext> {

    
    void enterPlayCollect(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitPlayCollect(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterLoadingPlaylist(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);
    
    void exitLoadingPlaylist(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void onPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void onReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void onNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void onCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterEvaluating(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitEvaluating(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterCanceled(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitCanceled(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void onPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterSucceeded(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void onPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void exitPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

    void enterFailed(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context);

}