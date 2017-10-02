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

package org.restcomm.media.control.mgcp.endpoint.notification;

import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.*;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.*;

import java.util.Arrays;
import java.util.List;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class NotificationCenterFsmBuilder {

    public static final NotificationCenterFsmBuilder INSTANCE = new NotificationCenterFsmBuilder();

    private final StateMachineBuilder<NotificationCenterFsm, NotificationCenterState, NotificationCenterEvent, NotificationCenterTransitionContext> builder;

    private NotificationCenterFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<NotificationCenterFsm, NotificationCenterState, NotificationCenterEvent, NotificationCenterTransitionContext>create(NotificationCenterFsmImpl.class, NotificationCenterState.class, NotificationCenterEvent.class, NotificationCenterTransitionContext.class, NotificationCenterContext.class);

        final List<NotificationCenterAction> rqntActionsIdle = Arrays.asList(FilterRequestedSignalsAction.INSTANCE, RequestNotificationAction.INSTANCE, ExecutePendingSignalsAction.INSTANCE, NotifyCallbackAction.INSTANCE);
        final List<NotificationCenterAction> rqntActionsActive = Arrays.asList(FilterRequestedSignalsAction.INSTANCE, QuarantineSignalsAction.INSTANCE, UnregisterSignalsAction.INSTANCE, CancelUnrequestedTimeoutSignalsAction.INSTANCE, RequestNotificationAction.INSTANCE, ExecutePendingSignalsAction.INSTANCE, NotifyCallbackAction.INSTANCE);
        final List<NotificationCenterAction> deactivationActions = Arrays.asList(PersistDeactivationCallbackAction.INSTANCE, CancelAllSignalsAction.INSTANCE);

        this.builder.onEntry(IDLE);
        this.builder.internalTransition().within(IDLE).on(NOTIFICATION_REQUEST).when(NoRequestedSignalsCondition.INSTANCE).perform(rqntActionsIdle);
        this.builder.transition().from(IDLE).to(ACTIVE).on(NOTIFICATION_REQUEST).when(HasRequestedSignalsCondition.INSTANCE).perform(rqntActionsIdle);
        this.builder.transition().from(IDLE).toFinal(DEACTIVATED).on(STOP);
        
        this.builder.onEntry(ACTIVE);
        this.builder.transition().from(ACTIVE).to(IDLE).on(NOTIFICATION_REQUEST).when(NoRequestedSignalsCondition.INSTANCE).perform(rqntActionsActive);
        this.builder.transition().from(ACTIVE).to(IDLE).on(ALL_SIGNALS_COMPLETED);
        this.builder.internalTransition().within(ACTIVE).on(NOTIFICATION_REQUEST).when(HasRequestedSignalsCondition.INSTANCE).perform(rqntActionsActive);
        this.builder.internalTransition().within(ACTIVE).on(SIGNAL_EXECUTED).when(IsActiveBriefSignalCondition.INSTANCE).perform(ExecuteNextBriefSignalAction.INSTANCE);
        this.builder.internalTransition().within(ACTIVE).on(SIGNAL_EXECUTED).when(IsActiveTimeoutSignalCondition.INSTANCE).perform(EvaluateSignalResultAction.INSTANCE);
        this.builder.transition().from(ACTIVE).to(DEACTIVATING).on(STOP);

        this.builder.onEntry(DEACTIVATING).perform(deactivationActions);
        this.builder.internalTransition().within(DEACTIVATING).on(SIGNAL_CANCELLED).when(IsActiveTimeoutSignalCondition.INSTANCE).perform(RemoveTimeoutSignalAction.INSTANCE);
        this.builder.internalTransition().within(DEACTIVATING).on(SIGNAL_EXECUTED).when(IsActiveTimeoutSignalCondition.INSTANCE).perform(RemoveTimeoutSignalAction.INSTANCE);
        this.builder.internalTransition().within(DEACTIVATING).on(SIGNAL_EXECUTED).when(IsActiveBriefSignalCondition.INSTANCE).perform(RemoveActiveBriefSignalAction.INSTANCE);
        this.builder.transition().from(DEACTIVATING).toFinal(DEACTIVATED).on(ALL_SIGNALS_COMPLETED);

        this.builder.onEntry(DEACTIVATED).perform(NotifyDeactivationCallbackAction.INSTANCE);
    }

    public NotificationCenterFsm build(NotificationCenterContext context) {
        return this.builder.newStateMachine(IDLE, context);
    }

}
