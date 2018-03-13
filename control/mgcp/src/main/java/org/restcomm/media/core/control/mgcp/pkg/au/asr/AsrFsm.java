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

package org.restcomm.media.core.control.mgcp.pkg.au.asr;

import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * @author anikiforov
 */
public interface AsrFsm extends StateMachine<AsrFsm, AsrState, AsrEvent, AsrContext> {

    void enterPlayCollect(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitPlayCollect(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterLoadingPlaylist(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitLoadingPlaylist(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterPrompting(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void onPrompting(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitPrompting(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterPrompted(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterCollecting(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void onCollecting(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void onTextRecognized(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitCollecting(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterWaitingForResponse(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitWaitingForResponse(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterEvaluating(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitEvaluating(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterCanceled(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitCanceled(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterSucceeding(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitSucceeding(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterPlayingSuccess(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void onPlayingSuccess(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitPlayingSuccess(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterSucceeded(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterFailing(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitFailing(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterPlayingFailure(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void onPlayingFailure(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void exitPlayingFailure(AsrState from, AsrState to, AsrEvent event, AsrContext context);

    void enterFailed(AsrState from, AsrState to, AsrEvent event, AsrContext context);

}
