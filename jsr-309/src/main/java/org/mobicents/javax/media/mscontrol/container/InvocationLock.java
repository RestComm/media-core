/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.javax.media.mscontrol.container;

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
