/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.server.mgcp.tx;

import org.mobicents.media.server.mgcp.MgcpProvider;
import org.mobicents.media.server.mgcp.controller.CallManager;
import org.mobicents.media.server.mgcp.controller.naming.NamingTree;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * Implements pool of transactions.
 * 
 * @author kulikov
 */
public class TransactionManager {
    //transaction identifier generator
    private static int ID = 1;
    
    //pool of transaction objects
    private Transaction[] pool;
    //currently active transactions.
    private Transaction[] active;
    
    //scheduler instance
    private Scheduler scheduler;

    //access to MGCP protocol provider
    protected MgcpProvider provider;
    
    //access to naming service
    protected NamingTree namingService;
    
    protected CallManager callManager;
    /**
     * Creates new transaction's pool.
     * 
     * @param scheduler the job scheduler
     * @param size the size of the pool.
     */
    public TransactionManager(Scheduler scheduler, int size) {
        this.scheduler = scheduler;
        
        pool = new Transaction[size];
        active = new Transaction[size];
        
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Transaction(this);
        }
    }
    
    /**
     * Associates endpoint naming service.
     * 
     * @param namingService the endpoint naming service.
     */
    public void setNamingService(NamingTree namingService) {
        this.namingService = namingService;
    }
    
    /**
     * Gets the access to the scheduler.
     * 
     * @return job scheduler.
     */
    public Scheduler scheduler() {
        return scheduler;
    }
    
    /**
     * Provides access to the wall clock.
     * 
     * @return time measured by wall clock.
     */
    public long getTime() {
        return scheduler.getClock().getTime();
    }
    
    public void setCallManager(CallManager callManager) {
        this.callManager = callManager;
    }
    
    
    /**
     * Assigns MGCP provider.
     * 
     * @param provider mgcp provider instance
     */
    public void setMgcpProvider(MgcpProvider provider) {
        this.provider = provider;
    }
    
    /**
     * Find active transaction with specified identifier.
     * 
     * @param id the transaction identifier.
     * @return transaction object or null if does not exist.
     */
    public synchronized Transaction find(int id) {    	
    	for (int i = 0; i < active.length; i++) {
    		if (active[i] != null && active[i].id == id) {
    			return active[i];
    		}
    	}        
    	return begin(id);    	
    }
    
    /**
     * Starts new transaction.
     * 
     * @param id the identifier of new transaction.
     * @return the object which represents transaction.
     */
    private Transaction begin(int id) {
        Transaction t = null;
        for (int i = 0; i < pool.length; i++) {
            if (pool[i] != null) {
                t = pool[i];
                pool[i] = null;
                break;
            }
        }
        
        if (t == null) {
            return t;
        }
        t.id = id;
        insert(t, active);
        
        return t;
    }

    /**
     * Terminates active transaction.
     * 
     * @param t the transaction to be terminated
     */
    protected synchronized void terminate(Transaction t) {
    	for (int i = 0; i < active.length; i++) {
    		if (active[i] != null && active[i].id == t.id) {
    			active[i] = null;
    			break;
    		}
    	}
                
    	t.id = 0;
    	insert(t, pool);    	
    }
    
    /**
     * Insert transaction into first empty space of the given array.
     * 
     * @param t the transaction to be inserted
     * @param list the array 
     */
    private void insert(Transaction t, Transaction[] list) {
        for (int i = 0; i < list.length; i++) {
            if (list[i] == null) {
                list[i] = t;
                return;
            }
        }
    }
    
    /**
     * Generates unique transaction identifier.
     * 
     * @return unique integer identifier.
     */
    protected int nextID() {
        return ID++;
    }
    
    /**
     * Gets the remainder of unused transaction objects.
     * Used for test purpose.
     * 
     * @return the number of available transaction objects.
     */
    protected int remainder() {
        int count = 0;
        for (int i = 0; i < pool.length; i++) {
            if (pool[i] != null) {
                count++;
            }
        }
        return count;
    }
}
