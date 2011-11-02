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

package org.mobicents.media.server.ctrl.mgcp.signal;

import javolution.util.FastList;
import org.apache.log4j.Logger;

/**
 * Pool of Request executors.
 * 
 * @author kulikov
 */
public class RequestExecutors {
    
    private FastList<RequestExecutor> pool = new FastList();
    private final MgcpPackages packages = new MgcpPackages();
    
    private Logger logger = Logger.getLogger(RequestExecutors.class);
    /**
     * Creates new instance of this pool and pre-loads it.
     * 
     * @param count the number of executors in the poll loaded at start up.
     */
    public RequestExecutors(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            pool.add(new RequestExecutor(this, packages.loadAll()));
        }
    }
    
    /**
     * Extracts first unused executor from pool.
     * 
     * @return first unused executor.
     */     
    public RequestExecutor poll() throws Exception {
        if (pool.size() > 0) {
            RequestExecutor re = pool.remove(0);
            return re;
        }
        
        logger.warn("Pool is empty, increasing size");
        RequestExecutor re = new RequestExecutor(this, packages.loadAll());
        return re;
    }
    
    /**
     * Recycles used executor.
     * 
     * @param executor the executor to be recycled.
     */
    protected void recycle(RequestExecutor executor) {
        this.pool.add(executor);
    }
    
    /**
     * Returns number of inactive executors in the pool.
     * 
     * @return the number of executors.
     */
    public int remainder() {
        return pool.size();
    }
}
