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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol.container;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.JoinEvent;

/**
 * Locks method calls till join event.
 * 
 * @author kulikov
 */
public class InvocationLock {
    private Semaphore semaphore = new Semaphore(0);
    private long timestamp;
    
    public void lock(long timeout) throws MsControlException {
        //clean last event 
        timestamp = System.currentTimeMillis();
        
        //try to wait 
        try {
            semaphore.tryAcquire(1, timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new MsControlException("Interrupted");
        }
        
        long duration = System.currentTimeMillis() - timestamp;
        //no new even was receive during timeout period
        if (duration >= timeout) {
            throw new MsControlException("No response from server during " + timeout + " ms");
        }
    }

    public void release() {
        semaphore.release();
    }
    
}
