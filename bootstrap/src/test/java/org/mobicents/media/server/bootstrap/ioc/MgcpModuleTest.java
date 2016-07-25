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

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.message.MgcpMessageSubject;
import org.mobicents.media.control.mgcp.pkg.MgcpSignalProvider;
import org.mobicents.media.control.mgcp.transaction.MgcpTransactionProvider;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.MgcpEndpointManagerProvider;
import org.mobicents.media.server.spi.ServerManager;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpModuleTest {

    @Test
    public void testMgcpConnectionProviderBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        // when
        MgcpConnectionProvider obj = injector.getInstance(MgcpConnectionProvider.class);

        // then
        Assert.assertNotNull(obj);
    }

    @Test
    public void testMgcpEndpointManagerProviderBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        // when
        MgcpEndpointManagerProvider obj = injector.getInstance(MgcpEndpointManagerProvider.class);

        // then
        Assert.assertNotNull(obj);
    }

    @Test
    public void testMgcpCommandProviderBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        // when
        MgcpCommandProvider obj = injector.getInstance(MgcpCommandProvider.class);

        // then
        Assert.assertNotNull(obj);
    }

    @Test
    public void testMgcpTransactionProviderBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        // when
        MgcpTransactionProvider obj = injector.getInstance(MgcpTransactionProvider.class);

        // then
        Assert.assertNotNull(obj);
    }

    @Test
    public void testMgcpMessageSubjectBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        // when
        MgcpMessageSubject obj = injector.getInstance(MgcpMessageSubject.class);

        // then
        Assert.assertNotNull(obj);
    }

    @Test
    public void testMgcpSignalProviderBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        // when
        MgcpSignalProvider obj = injector.getInstance(MgcpSignalProvider.class);

        // then
        Assert.assertNotNull(obj);
    }

    @Test
    public void testServerManagerBinding() {
        // given
        final MediaServerConfiguration config = new MediaServerConfiguration();
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));
        
        // when
        ServerManager obj = injector.getInstance(ServerManager.class);
        
        // then
        Assert.assertNotNull(obj);
    }

}
