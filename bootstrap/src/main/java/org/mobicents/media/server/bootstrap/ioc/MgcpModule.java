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
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpCommandProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpConnectionProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpControllerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointInstallerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointInstallerProvider.MgcpEndpointInstallerListType;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointManagerProvider;
import org.mobicents.media.server.spi.ServerManager;

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
        bind(ServerManager.class).toProvider(MgcpControllerProvider.class).in(Singleton.class);
    }
}
