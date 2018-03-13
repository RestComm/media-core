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

package org.restcomm.media.core.control.mgcp.pkg.au.pr;

import org.restcomm.media.core.control.mgcp.pkg.au.Playlist;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class LoadPlaylistAction extends AnonymousAction<PlayRecordFsm, PlayRecordState, PlayRecordEvent, PlayRecordContext> {

    static final LoadPlaylistAction INSTANCE = new LoadPlaylistAction();

    public LoadPlaylistAction() {
        super();
    }

    @Override
    public void execute(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context,
            PlayRecordFsm stateMachine) {
        // Initial event. Play initial prompt, if any.
        if(event == null) {
            final Playlist prompt = context.getInitialPrompt();
            if (prompt.isEmpty()) {
                stateMachine.fire(PlayRecordEvent.NO_PROMPT, context);
            } else {
                stateMachine.fire(PlayRecordEvent.PROMPT, context);
            }
        } else {
            switch (event) {
                case REINPUT:
                    stateMachine.fire(PlayRecordEvent.NO_PROMPT, context);
                    break;

                case RESTART:
                    final Playlist prompt = context.getInitialPrompt();
                    if (prompt.isEmpty()) {
                        stateMachine.fire(PlayRecordEvent.NO_PROMPT, context);
                    } else {
                        stateMachine.fire(PlayRecordEvent.PROMPT, context);
                    }
                    break;

                case NO_SPEECH:
                    final Playlist noSpeechReprompt = context.getNoSpeechReprompt();
                    if (noSpeechReprompt.isEmpty()) {
                        stateMachine.fire(PlayRecordEvent.NO_PROMPT, context);
                    } else {
                        stateMachine.fire(PlayRecordEvent.NO_SPEECH, context);
                    }
                    break;

                default:
                    final Playlist reprompt = context.getReprompt();
                    if (reprompt.isEmpty()) {
                        stateMachine.fire(PlayRecordEvent.NO_PROMPT, context);
                    } else {
                        stateMachine.fire(PlayRecordEvent.REPROMPT, context);
                    }
                    break;
            }
        }
    }

}
