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

package org.mobicents.media.server.bootstrap.ioc.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.core.configuration.MgcpEndpointConfiguration;
import org.mobicents.media.server.mgcp.endpoint.factory.VirtualEndpointInstaller;
import org.mobicents.media.server.spi.EndpointInstaller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EndpointInstallerListProvider implements Provider<List<EndpointInstaller>> {

    private final MediaServerConfiguration config;

    @Inject
    public EndpointInstallerListProvider(MediaServerConfiguration config) {
        this.config = config;
    }

    @Override
    public List<EndpointInstaller> get() {
        List<EndpointInstaller> list = new ArrayList<>(this.config.getControllerConfiguration().countEndpoints());
        Iterator<MgcpEndpointConfiguration> endpoints = this.config.getControllerConfiguration().getEndpoints();
        while (endpoints.hasNext()) {
            MgcpEndpointConfiguration endpoint = endpoints.next();
            VirtualEndpointInstaller installer = new VirtualEndpointInstaller();
            installer.setEndpointClass(endpoint.getClassName());
            installer.setNamePattern(endpoint.getName());
            installer.setInitialSize(endpoint.getPoolSize());
            list.add(installer);
        }
        return Collections.unmodifiableList(list);
    }

    public static final class EndpointInstallerListType extends TypeLiteral<List<EndpointInstaller>> {

        public static final EndpointInstallerListType INSTANCE = new EndpointInstallerListType();

        private EndpointInstallerListType() {
            super();
        }

    }

}
