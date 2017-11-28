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

package org.restcomm.media.control.mgcp.endpoint;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 28/11/2017
 */
class ShutdownNotificationCenterAction extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext> implements MgcpEndpointAction {

    static final ShutdownNotificationCenterAction INSTANCE = new ShutdownNotificationCenterAction();

    private static final Logger log = Logger.getLogger(ShutdownNotificationCenterAction.class);

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, final MgcpEndpointTransitionContext context, final MgcpEndpointFsm stateMachine) {
        final NotificationCenter notificationCenter = stateMachine.getContext().getNotificationCenter();

        notificationCenter.shutdown(new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                if (log.isDebugEnabled()) {
                    log.debug("Shutdown Notification Center for endpoint " + stateMachine.getContext().getEndpointId());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to shutdown Notification Center for endpoint " + stateMachine.getContext().getEndpointId(), t);
            }
        });
    }
}
