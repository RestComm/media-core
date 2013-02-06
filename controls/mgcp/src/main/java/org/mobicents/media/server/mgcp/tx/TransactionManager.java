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
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Enumeration;

/**
 * Implements pool of transactions.
 * 
 * @author yulian oifa
 */
public class TransactionManager {
    //transaction identifier generator
    private static java.util.concurrent.atomic.AtomicInteger ID = new AtomicInteger(1);
    
    //pool of transaction objects
    private  ConcurrentCyclicFIFO<Transaction> pool=new ConcurrentCyclicFIFO();
    
    //cache size in 100ms units    
    private static final int cacheSize=5;
    private ConcurrentCyclicFIFO<Transaction>[] cache=new ConcurrentCyclicFIFO[cacheSize];
    private int cleanIndex=0;
    
    //currently active transactions.
    private ConcurrentHashMap<Integer,Transaction> active;
    
    //scheduler instance
    private Scheduler scheduler;

    //access to MGCP protocol provider
    protected MgcpProvider provider;
    
    //access to naming service
    protected NamingTree namingService;
    
    //call manager
    protected CallManager callManager;
    
    //cache heartbeat
    private Heartbeat cacheHeartbeat;
    
    /**
     * Creates new transaction's pool.
     * 
     * @param scheduler the job scheduler
     * @param size the size of the pool.
     */
    public TransactionManager(Scheduler scheduler, int size) {
        this.scheduler = scheduler;
        
        active = new ConcurrentHashMap(size);
        
        for (int i = 0; i < size; i++) {
        	pool.offer(new Transaction(this));            
        }      
        
        for(int i=0;i<cache.length;i++)
        	cache[i]=new ConcurrentCyclicFIFO<Transaction>();
        
        cacheHeartbeat = new Heartbeat();        
    }
    
    public void start()
    {
    	scheduler.submitHeatbeat(cacheHeartbeat);
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
    public Transaction find(int id) {    
    	Transaction currTransaction=active.get(id);
    	return currTransaction;    	  
    }
    
    public Transaction allocateNew(int id)
    {
    	Transaction t=begin(ID.getAndIncrement());
    	if(t!=null)
    		t.id=id;
    	
    	return t;
    }
    
    /**
     * Starts new transaction.
     * 
     * @param id the identifier of new transaction.
     * @return the object which represents transaction.
     */
    private Transaction begin(int id) {
        Transaction t = pool.poll();
        
        if (t == null)
        	t=new Transaction(this);
        
        t.uniqueId = id;
        active.put(t.uniqueId,t);        
        
        return t;
    }

    /**
     * Terminates active transaction.
     * 
     * @param t the transaction to be terminated
     */
    protected void terminate(Transaction t) {
    	cache[cleanIndex].offer(t);
    }   
    
    /**
     * Generates unique transaction identifier.
     * 
     * @return unique integer identifier.
     */
    protected int nextID() {
        return ID.incrementAndGet();
    }
    
    /**
     * Gets the remainder of unused transaction objects.
     * Used for test purpose.
     * 
     * @return the number of available transaction objects.
     */
    protected int remainder() {
    	return pool.size();        
    }
    
    private class Heartbeat extends Task {
    	int queueToClean;
    	
    	public Heartbeat() {
            super();
        }        

        @Override
        public long perform() {
        	queueToClean=(cleanIndex+1)%cacheSize;
        	Transaction current=cache[queueToClean].poll();
            while(current!=null)
            {
            	active.remove(current.uniqueId);
            	current.id = 0;
            	current.uniqueId=0;
            	current.completed=false;
            	pool.offer(current);
            	current=cache[queueToClean].poll();
            }
            
            cleanIndex=(cleanIndex+1)%cacheSize;
            scheduler.submitHeatbeat(this);
            return 0;
        }
    
        public int getQueueNumber() {
            return scheduler.HEARTBEAT_QUEUE;
        }
    }
}
