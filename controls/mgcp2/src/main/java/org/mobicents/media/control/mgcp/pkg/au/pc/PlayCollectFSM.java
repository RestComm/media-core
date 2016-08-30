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

import org.squirrelframework.foundation.fsm.StateMachine;
import org.squirrelframework.foundation.fsm.TransitionType;
import org.squirrelframework.foundation.fsm.annotation.State;
import org.squirrelframework.foundation.fsm.annotation.States;
import org.squirrelframework.foundation.fsm.annotation.Transit;
import org.squirrelframework.foundation.fsm.annotation.Transitions;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
//@States({ 
//    @State(name = "ready", initialState = true),
//    @State(name = "collecting", entryCallMethod = "enterCollecting", exitCallMethod = "exitCollecting"),
//    @State(name = "succeeded", entryCallMethod = "enterSucceeded", isFinal = true),
//    @State(name = "failed", entryCallMethod = "enterFailed", isFinal = true),
//    @State(name = "canceled", entryCallMethod = "enterCanceled", isFinal = true)
//})
//@Transitions({ 
//    @Transit(from = "ready", to = "collecting", on = "execute"), 
//    @Transit(from = "collecting", to = "collecting", on = "dtmf_tone", type=TransitionType.INTERNAL, callMethod = "onCollecting"),
//    @Transit(from = "collecting", to = "succeeded", on = "endInputKey"),
//    @Transit(from = "collecting", to = "failed", on = "endInputKey"),
//})
public interface PlayCollectFSM extends StateMachine<PlayCollectFSM, PlayCollectState, Object, PlayCollectContext> {

    void enterCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context);

    void onCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context);

    void exitCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context);

    void enterCanceled(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context);

    void enterSucceeded(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context);

    void enterFailed(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context);

}