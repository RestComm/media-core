/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.network.api;

import com.google.common.util.concurrent.FutureCallback;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * Provides and multiplexes a group of {@link SynchronousNetworkChannel} in asynchronous fashion.
 *
 * @param <C> The type of channel produced by the Network Manager.
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public interface AsynchronousNetworkManager<C extends Channel> {

    /**
     * Opens and registers a new Network Channel.
     *
     * @param callback The callback that is invoked when the channel is open.
     */
    void openChannel(FutureCallback<C> callback);

    /**
     * Opens and registers a new Network Channel.
     *
     * @param callback           The callback that is invoked when the channel is open.
     * @param channelInitializer The channel initializer.
     */
    void openChannel(FutureCallback<C> callback, ChannelInitializer<C> channelInitializer);

    /**
     * Asynchronously shuts down the Network Manager, closing any registered channels.
     *
     * @param callback The callback that is invoked when the manager is shut down.
     */
    void close(FutureCallback<Void> callback);

}
