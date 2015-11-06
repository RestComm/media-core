/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp.controller;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.server.concurrent.pooling.ConcurrentResourcePool;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpConnectionPool extends ConcurrentResourcePool<MgcpConnection> {

    private static final Logger LOGGER = Logger.getLogger(MgcpConnectionPool.class);

    private final AtomicInteger count = new AtomicInteger(15);
    
    private static final MgcpConnectionPool INSTANCE = new MgcpConnectionPool();
    
    private MgcpConnectionPool() {
        super();
        for (int i = 0; i < 15; i++) {
            super.offer(new MgcpConnection());
        }
    }
    
    public static MgcpConnectionPool getInstance() {
        return INSTANCE;
    }

    @Override
    public MgcpConnection poll() {
        // Attempt to retrieve pooled connection
        MgcpConnection connection = super.poll();

        if (connection == null) {
            // Create new connection in case pool is empty
            connection = new MgcpConnection();
            int newSize = this.count.incrementAndGet();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Allocated new MGCP connection in, pooled=" + newSize + ", free=" + this.size());
            }
        }

        return connection;

    }

    @Override
    public void offer(MgcpConnection resource) {
        super.offer(resource);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Released MGCP connection, pooled=" + this.count.get() + ", free=" + this.size());
        }
    }

}
