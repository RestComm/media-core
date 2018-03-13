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

package org.restcomm.media.core.control.mgcp.command;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.core.control.mgcp.call.MgcpCallManager;
import org.restcomm.media.core.control.mgcp.command.AuditConnectionCommand;
import org.restcomm.media.core.control.mgcp.command.AuditEndpointCommand;
import org.restcomm.media.core.control.mgcp.command.CreateConnectionCommand;
import org.restcomm.media.core.control.mgcp.command.DeleteConnectionCommand;
import org.restcomm.media.core.control.mgcp.command.MgcpCommand;
import org.restcomm.media.core.control.mgcp.command.MgcpCommandProvider;
import org.restcomm.media.core.control.mgcp.command.ModifyConnectionCommand;
import org.restcomm.media.core.control.mgcp.command.RequestNotificationCommand;
import org.restcomm.media.core.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.core.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.core.control.mgcp.message.MgcpRequestType;
import org.restcomm.media.core.control.mgcp.pkg.MgcpPackageManager;
import org.restcomm.media.core.control.mgcp.pkg.MgcpSignalProvider;
import org.restcomm.media.core.control.mgcp.util.collections.Parameters;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCommandProviderTest {

    @Test
    public void testProvideCrcx() {
        // given
        final int transactionId = 12345;
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);
        final MgcpCallManager callManager = mock(MgcpCallManager.class);
        final MgcpCommandProvider commandProvider = new MgcpCommandProvider(endpointManager, packageManager, signalProvider, callManager);

        // when
        MgcpCommand command = commandProvider.provide(MgcpRequestType.CRCX, transactionId, parameters);

        // then
        Assert.assertTrue(command instanceof CreateConnectionCommand);
    }

    @Test
    public void testProvideMdcx() {
        // given
        final int transactionId = 12345;
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);
        final MgcpCallManager callManager = mock(MgcpCallManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final MgcpCommandProvider commandProvider = new MgcpCommandProvider(endpointManager, packageManager, signalProvider, callManager);

        // when
        MgcpCommand command = commandProvider.provide(MgcpRequestType.MDCX, transactionId, parameters);

        // then
        Assert.assertTrue(command instanceof ModifyConnectionCommand);
    }

    @Test
    public void testProvideDlcx() {
        // given
        final int transactionId = 12345;
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);
        final MgcpCallManager callManager = mock(MgcpCallManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final MgcpCommandProvider commandProvider = new MgcpCommandProvider(endpointManager, packageManager, signalProvider, callManager);

        // when
        MgcpCommand command = commandProvider.provide(MgcpRequestType.DLCX, transactionId, parameters);

        // then
        Assert.assertTrue(command instanceof DeleteConnectionCommand);
    }

    @Test
    public void testProvideAucx() {
        // given
        final int transactionId = 12345;
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);
        final MgcpCallManager callManager = mock(MgcpCallManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final MgcpCommandProvider commandProvider = new MgcpCommandProvider(endpointManager, packageManager, signalProvider, callManager);

        // when
        MgcpCommand command = commandProvider.provide(MgcpRequestType.AUCX, transactionId, parameters);

        // then
        Assert.assertTrue(command instanceof AuditConnectionCommand);
    }

    @Test
    public void testProvideAuep() {
        // given
        final int transactionId = 12345;
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);
        final MgcpCallManager callManager = mock(MgcpCallManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final MgcpCommandProvider commandProvider = new MgcpCommandProvider(endpointManager, packageManager, signalProvider, callManager);

        // when
        MgcpCommand command = commandProvider.provide(MgcpRequestType.AUEP, transactionId, parameters);

        // then
        Assert.assertTrue(command instanceof AuditEndpointCommand);
    }

    @Test
    public void testProvideRqnt() {
        // given
        final int transactionId = 12345;
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);
        final MgcpCallManager callManager = mock(MgcpCallManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final MgcpCommandProvider commandProvider = new MgcpCommandProvider(endpointManager, packageManager, signalProvider, callManager);

        // when
        MgcpCommand command = commandProvider.provide(MgcpRequestType.RQNT, transactionId, parameters);

        // then
        Assert.assertTrue(command instanceof RequestNotificationCommand);
    }
}
