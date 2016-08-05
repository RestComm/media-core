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

package org.mobicents.media.server.bootstrap.ioc;

import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.endpoint.provider.MediaGroupProvider;
import org.mobicents.media.control.mgcp.network.MgcpChannel;
import org.mobicents.media.control.mgcp.pkg.MgcpSignalProvider;
import org.mobicents.media.control.mgcp.transaction.MgcpTransactionProvider;
import org.mobicents.media.control.mgcp.transaction.TransactionManager;
import org.mobicents.media.server.bootstrap.ioc.provider.ListeningExecutorServiceProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MediaGroupProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpChannelProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpCommandProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpConnectionProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpControllerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointInstallerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointInstallerProvider.MgcpEndpointInstallerListType;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointManagerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpSignalProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpTransactionManagerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpTransactionProviderProvider;
import org.mobicents.media.server.spi.ServerManager;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MgcpConnectionProvider.class).toProvider(MgcpConnectionProviderProvider.class).in(Singleton.class);
        bind(MgcpEndpointInstallerListType.INSTANCE).toProvider(MgcpEndpointInstallerProvider.class).in(Singleton.class);
        bind(MgcpEndpointManager.class).toProvider(MgcpEndpointManagerProvider.class).in(Singleton.class);
        bind(MgcpCommandProvider.class).toProvider(MgcpCommandProviderProvider.class).in(Singleton.class);
        bind(MgcpTransactionProvider.class).toProvider(MgcpTransactionProviderProvider.class).in(Singleton.class);
        bind(TransactionManager.class).toProvider(MgcpTransactionManagerProvider.class).in(Singleton.class);
        bind(MgcpChannel.class).toProvider(MgcpChannelProvider.class).in(Singleton.class);
        bind(ServerManager.class).toProvider(MgcpControllerProvider.class).in(Singleton.class);
        bind(MgcpSignalProvider.class).toProvider(MgcpSignalProviderProvider.class).in(Singleton.class);
        bind(MediaGroupProvider.class).toProvider(MediaGroupProviderProvider.class).in(Singleton.class);
        bind(ListeningExecutorService.class).toProvider(ListeningExecutorServiceProvider.class).in(Singleton.class);
    }
}
