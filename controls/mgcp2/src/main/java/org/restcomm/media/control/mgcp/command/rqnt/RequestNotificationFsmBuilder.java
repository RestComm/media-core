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

package org.restcomm.media.control.mgcp.command.rqnt;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

import static org.restcomm.media.control.mgcp.command.rqnt.RequestNotificationEvent.*;
import static org.restcomm.media.control.mgcp.command.rqnt.RequestNotificationState.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 27/11/2017
 */
public class RequestNotificationFsmBuilder {

    public static final RequestNotificationFsmBuilder INSTANCE = new RequestNotificationFsmBuilder();

    private final StateMachineBuilder<RequestNotificationFsm, RequestNotificationState, RequestNotificationEvent, RequestNotificationContext> builder;


    public RequestNotificationFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<RequestNotificationFsm, RequestNotificationState, RequestNotificationEvent, RequestNotificationContext>create(RequestNotificationFsmImpl.class, RequestNotificationState.class, RequestNotificationEvent.class, RequestNotificationContext.class);

        this.builder.onEntry(VALIDATING_PARAMETERS).perform(ValidateParametersAction.INSTANCE);
        this.builder.externalTransition().from(VALIDATING_PARAMETERS).to(EXECUTING).on(VALIDATED_PARAMETERS);
        this.builder.externalTransition().from(VALIDATING_PARAMETERS).toFinal(FAILED).on(FAILURE);

        this.builder.onEntry(EXECUTING).perform(ExecuteAction.INSTANCE);
        this.builder.externalTransition().from(EXECUTING).toFinal(SUCCEEDED).on(EXECUTED);
        this.builder.externalTransition().from(EXECUTING).toFinal(FAILED).on(FAILURE);

        this.builder.onEntry(FAILED).perform(RespondAction.INSTANCE);
        this.builder.onEntry(SUCCEEDED).perform(RespondAction.INSTANCE);
    }

    public RequestNotificationFsm build() {
        return this.builder.newStateMachine(RequestNotificationState.VALIDATING_PARAMETERS);
    }

}
