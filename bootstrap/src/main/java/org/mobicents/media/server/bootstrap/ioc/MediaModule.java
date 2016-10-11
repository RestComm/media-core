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

import org.mobicents.media.server.bootstrap.ioc.provider.DspProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.media.AudioPlayerProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.media.ChannelsManagerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.media.DtmfDetectorProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.media.MediaChannelProviderProvider;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.channels.MediaChannelProvider;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorProvider;
import org.mobicents.media.server.spi.player.PlayerProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DspFactory.class).toProvider(DspProvider.class).in(Singleton.class);
        bind(ChannelsManager.class).toProvider(ChannelsManagerProvider.class).in(Singleton.class);
        bind(MediaChannelProvider.class).toProvider(MediaChannelProviderProvider.class).in(Singleton.class);
        bind(PlayerProvider.class).toProvider(AudioPlayerProviderProvider.class).in(Singleton.class);
        bind(DtmfDetectorProvider.class).toProvider(DtmfDetectorProviderProvider.class).in(Singleton.class);
    }

}
