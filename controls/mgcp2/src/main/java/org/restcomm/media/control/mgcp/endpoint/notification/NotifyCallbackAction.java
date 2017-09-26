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

import com.google.common.util.concurrent.FutureCallback;

/**
 * Notifies the callback about failed or successful completion of a task.
 * 
 * Input parameters:
 * <ul>
 * <li>ERROR (optional)</li>
 * <li>CALLBACK</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class NotifyCallbackAction extends NotificationCenterAction {

    static final NotifyCallbackAction INSTANCE = new NotifyCallbackAction();

    NotifyCallbackAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final Throwable error = context.get(NotificationCenterTransitionParameter.ERROR, Throwable.class);
        final FutureCallback<?> callback = context.get(NotificationCenterTransitionParameter.CALLBACK, FutureCallback.class);

        if (error == null) {
            callback.onFailure(error);
        } else {
            callback.onSuccess(null);
        }
    }

}
