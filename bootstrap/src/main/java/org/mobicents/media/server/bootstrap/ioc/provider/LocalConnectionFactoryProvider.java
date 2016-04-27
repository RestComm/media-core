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

import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.mgcp.connection.LocalConnectionFactory;
import org.mobicents.media.server.mgcp.connection.LocalConnectionImpl;
import org.mobicents.media.server.spi.pooling.PooledObjectFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class LocalConnectionFactoryProvider implements Provider<LocalConnectionFactory> {

    private final ChannelsManager connectionFactory;

    @Inject
    public LocalConnectionFactoryProvider(ChannelsManager connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public LocalConnectionFactory get() {
        return new LocalConnectionFactory(this.connectionFactory);
    }

    public static final class LocalConnectionFactoryType extends TypeLiteral<PooledObjectFactory<LocalConnectionImpl>> {

        public static final LocalConnectionFactoryType INSTANCE = new LocalConnectionFactoryType();

        private LocalConnectionFactoryType() {
            super();
        }

    }

}
