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

package org.restcomm.media.bootstrap.ioc.provider.mgcp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.provider.MediaGroupProvider;
import org.restcomm.media.control.mgcp.endpoint.provider.MgcpEndpointProvider;
import org.restcomm.media.control.mgcp.endpoint.provider.MgcpMixerEndpointProvider;
import org.restcomm.media.control.mgcp.endpoint.provider.MgcpSplitterEndpointProvider;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.MgcpControllerConfiguration;
import org.restcomm.media.core.configuration.MgcpEndpointConfiguration;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpEndpointInstallerProvider implements Provider<List<MgcpEndpointProvider<? extends MgcpEndpoint>>> {

    private final MediaServerConfiguration configuration;
    private final PriorityQueueScheduler mediaScheduler;
    private final MgcpConnectionProvider connectionProvider;
    private final MediaGroupProvider mediaGroupProvider;

    @Inject
    public MgcpEndpointInstallerProvider(MediaServerConfiguration configuration, PriorityQueueScheduler mediaScheduler, MgcpConnectionProvider connectionProvider, MediaGroupProvider mediaGroupProvider) {
        this.configuration = configuration;
        this.mediaScheduler = mediaScheduler;
        this.connectionProvider = connectionProvider;
        this.mediaGroupProvider = mediaGroupProvider;
    }

    @Override
    public List<MgcpEndpointProvider<? extends MgcpEndpoint>> get() {
        final MgcpControllerConfiguration controller = this.configuration.getControllerConfiguration();
        final Iterator<MgcpEndpointConfiguration> iterator = controller.getEndpoints();
        final List<MgcpEndpointProvider<? extends MgcpEndpoint>> providers = new ArrayList<>(controller.countEndpoints());
        final String domain = this.configuration.getControllerConfiguration().getAddress() + ":" + this.configuration.getControllerConfiguration().getPort();

        while (iterator.hasNext()) {
            final MgcpEndpointConfiguration endpoint = iterator.next();
            final MgcpEndpointProvider<? extends MgcpEndpoint> provider;
            final String namespace = endpoint.getName();

            switch (endpoint.getRelayType()) {
                case MIXER:
                    provider = new MgcpMixerEndpointProvider(namespace, domain, this.mediaScheduler, this.mediaGroupProvider);
                    break;

                case SPLITTER:
                    provider = new MgcpSplitterEndpointProvider(namespace, domain, this.mediaScheduler);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown relay type " + endpoint.getRelayType());
            }
            providers.add(provider);
        }
        return providers;
    }

    public static final class MgcpEndpointInstallerListType extends TypeLiteral<List<MgcpEndpointProvider<? extends MgcpEndpoint>>> {

        public static final MgcpEndpointInstallerListType INSTANCE = new MgcpEndpointInstallerListType();

        private MgcpEndpointInstallerListType() {
            super();
        }

    }

}
