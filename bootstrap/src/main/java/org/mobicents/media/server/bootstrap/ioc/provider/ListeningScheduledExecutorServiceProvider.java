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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ListeningScheduledExecutorServiceProvider implements Provider<ListeningScheduledExecutorService> {

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    public ListeningScheduledExecutorServiceProvider() {
        super();
    }

    @Override
    public ListeningScheduledExecutorService get() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("mgcp-%d").build();
        // ThreadPoolExecutor executor = new ThreadPoolExecutor(POOL_SIZE, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new
        // SynchronousQueue<Runnable>(), threadFactory);
        // executor.allowCoreThreadTimeOut(false);
        // Executors.newScheduledThreadPool(POOL_SIZE, threadFactory);
        // return MoreExecutors.listeningDecorator(executor);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(POOL_SIZE, threadFactory);
        return MoreExecutors.listeningDecorator(executor);
    }

}
