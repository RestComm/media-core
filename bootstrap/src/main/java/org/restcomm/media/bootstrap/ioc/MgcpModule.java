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

package org.restcomm.media.bootstrap.ioc;

import org.restcomm.media.bootstrap.ioc.provider.ListeningScheduledExecutorServiceProvider;
import org.restcomm.media.bootstrap.ioc.provider.media.MgcpCallManagerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.AsyncMgcpChannelProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.DynamicMgcpPackageManagerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.GlobalMgcpEventProviderProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.GlobalMgcpTransactionManagerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.LocalDataChannelProviderGuiceProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.LocalNetworkGuardProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MediaGroupProviderProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.Mgcp2ControllerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpChannelInboundHandlerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpChannelInitializerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpCommandProviderGuiceProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpConnectionGuiceProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpEndpointInstallerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpEndpointInstallerProvider.MgcpEndpointInstallerListType;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpEndpointManagerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpNetworkManagerProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpSignalProviderProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.MgcpTransactionNumberspaceProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.RtpEventProviderProvider;
import org.restcomm.media.bootstrap.ioc.provider.mgcp.SubMgcpTransactionManagerGuiceProvider;
import org.restcomm.media.control.mgcp.call.MgcpCallManager;
import org.restcomm.media.control.mgcp.command.MgcpCommandProvider;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.local.LocalDataChannelProvider;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.endpoint.provider.MediaGroupProvider;
import org.restcomm.media.control.mgcp.network.netty.AsyncMgcpChannel;
import org.restcomm.media.control.mgcp.network.netty.MgcpChannelInboundHandler;
import org.restcomm.media.control.mgcp.network.netty.MgcpChannelInitializer;
import org.restcomm.media.control.mgcp.network.netty.MgcpNetworkManager;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpPackageManager;
import org.restcomm.media.control.mgcp.pkg.MgcpSignalProvider;
import org.restcomm.media.control.mgcp.pkg.r.RtpEventProvider;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionManager;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionManagerProvider;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionNumberspace;
import org.restcomm.media.network.netty.filter.NetworkGuard;
import org.restcomm.media.spi.ServerManager;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MgcpConnectionProvider.class).toProvider(MgcpConnectionGuiceProvider.class).in(Singleton.class);
        bind(MgcpEndpointInstallerListType.INSTANCE).toProvider(MgcpEndpointInstallerProvider.class).in(Singleton.class);
        bind(MgcpEndpointManager.class).toProvider(MgcpEndpointManagerProvider.class).in(Singleton.class);
        bind(MgcpCommandProvider.class).toProvider(MgcpCommandProviderGuiceProvider.class).in(Singleton.class);
        bind(MgcpTransactionNumberspace.class).toProvider(MgcpTransactionNumberspaceProvider.class).in(Singleton.class);
        bind(MgcpTransactionManagerProvider.class).toProvider(SubMgcpTransactionManagerGuiceProvider.class).in(Singleton.class);
        bind(MgcpTransactionManager.class).toProvider(GlobalMgcpTransactionManagerProvider.class).in(Singleton.class);
        bind(MgcpChannelInboundHandler.class).toProvider(MgcpChannelInboundHandlerProvider.class).in(Singleton.class);
        bind(MgcpChannelInitializer.class).toProvider(MgcpChannelInitializerProvider.class).in(Singleton.class);
        bind(MgcpNetworkManager.class).toProvider(MgcpNetworkManagerProvider.class).in(Singleton.class);
        bind(AsyncMgcpChannel.class).toProvider(AsyncMgcpChannelProvider.class).in(Singleton.class);
        bind(ServerManager.class).toProvider(Mgcp2ControllerProvider.class).in(Singleton.class);
        bind(MgcpPackageManager.class).toProvider(DynamicMgcpPackageManagerProvider.class).in(Singleton.class);
        bind(MgcpCallManager.class).toProvider(MgcpCallManagerProvider.class).in(Singleton.class);
        bind(RtpEventProvider.class).toProvider(RtpEventProviderProvider.class).in(Singleton.class);
        bind(MgcpEventProvider.class).toProvider(GlobalMgcpEventProviderProvider.class).in(Singleton.class);
        bind(MgcpSignalProvider.class).toProvider(MgcpSignalProviderProvider.class).in(Singleton.class);
        bind(MediaGroupProvider.class).toProvider(MediaGroupProviderProvider.class).in(Singleton.class);
        bind(ListeningScheduledExecutorService.class).toProvider(ListeningScheduledExecutorServiceProvider.class).in(Singleton.class);
        bind(ListeningExecutorService.class).to(ListeningScheduledExecutorService.class);
        bind(NetworkGuard.class).annotatedWith(Names.named("mgcpNetworkGuard")).toProvider(LocalNetworkGuardProvider.class).in(Singleton.class);
        bind(LocalDataChannelProvider.class).toProvider(LocalDataChannelProviderGuiceProvider.class).asEagerSingleton();
    }

}
