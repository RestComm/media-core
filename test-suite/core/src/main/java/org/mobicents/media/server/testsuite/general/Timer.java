/*
 * Mobicents, Communications Middleware
 * 
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party
 * contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 *
 * Boston, MA  02110-1301  USA
 */
package org.mobicents.media.server.testsuite.general;

import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author kulikov
 */
public interface Timer {
    /**
     * Gets value of interval between timer ticks.
     * 
     * @return the int value in milliseconds.
     */
    public int getHeartBeat();

    /**
     * Modify interval between timer tick
     * 
     * @param heartBeat the new value of interval in milliseconds.
     */
    public void setHeartBeat(int heartBeat);
    
    /**
     * Synchronizes task from this timer.
     * 
     * @param task the task to be synchronized.
     * @return the action which can be canceled to unsynchronize previously 
     * synchronized task
     */
    public ScheduledFuture synchronize(Runnable task);
}
