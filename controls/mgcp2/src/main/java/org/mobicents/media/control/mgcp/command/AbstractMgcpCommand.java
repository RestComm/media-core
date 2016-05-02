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

package org.mobicents.media.control.mgcp.command;

import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * Abstract implementation of MGCP command that forces a rollback operation when {@link MgcpCommand#execute(MgcpRequest)} fails.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpCommand implements MgcpCommand {

    @Override
    public MgcpResponse execute(MgcpRequest request) {
        MgcpResponse response;
        try {
            response = executeRequest(request);
        } catch (MgcpCommandException e) {
            response = rollback(request.getTransactionId(), e.getCode(), e.getMessage());
        }
        return response;
    }

    protected abstract MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException;

    protected abstract MgcpResponse rollback(int transactionId, int code, String message);

}
