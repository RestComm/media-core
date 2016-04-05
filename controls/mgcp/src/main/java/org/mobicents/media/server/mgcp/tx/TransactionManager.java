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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.mgcp.MgcpProvider;
import org.mobicents.media.server.mgcp.controller.CallManager;
import org.mobicents.media.server.mgcp.controller.naming.NamingTree;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Implements pool of transactions.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class TransactionManager {
    
    private static final Logger log = Logger.getLogger(TransactionManager.class);
    
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private static final int CACHE_SIZE = 5;

    // core elements
    private final PriorityQueueScheduler scheduler;
    protected CallManager callManager;
    protected MgcpProvider provider;
    protected NamingTree namingService;
    
    // transactions
	private final ConcurrentCyclicFIFO<Transaction> transactionPool;
	private final ConcurrentMap<Transaction> activeTransactions;
    
    // cache size in 100ms units    
	private ConcurrentCyclicFIFO<Transaction>[] cache = new ConcurrentCyclicFIFO[CACHE_SIZE];
	private Heartbeat cacheHeartbeat;
	private int cleanIndex = 0;
    
    /**
     * Creates new transaction's pool.
     * 
     * @param scheduler the job scheduler
     * @param size the size of the pool.
     */
    public TransactionManager(PriorityQueueScheduler scheduler, int size) {
        // core elements
        this.scheduler = scheduler;

        // transactions
        this.transactionPool = new ConcurrentCyclicFIFO<Transaction>();
        this.activeTransactions = new ConcurrentMap<Transaction>();

        for (int i = 0; i < size; i++) {
            this.transactionPool.offer(new Transaction(this));
        }

        // cache
        for (int i = 0; i < CACHE_SIZE; i++) {
            this.cache[i] = new ConcurrentCyclicFIFO<Transaction>();
        }
        this.cacheHeartbeat = new Heartbeat();
    }
    
    public void start() {
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
    public PriorityQueueScheduler scheduler() {
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
    	Transaction currTransaction=activeTransactions.get(id);
    	return currTransaction;    	  
    }
    
	/**
	 * Finds a transaction by number (rather than unique transaction ID) in the
	 * active transaction pool.
	 * 
	 * @param transactionNumber
	 *            the number of the transaction to look for
	 * @return The transaction with the desired number. Returns null if none
	 *         matches criteria.
	 * @author hrosa
	 */
	public Transaction findByTransactionNumber(int transactionNumber) {
		Iterator<Transaction> transactions = this.activeTransactions.valuesIterator();
		while (transactions.hasNext()) {
			Transaction transaction = transactions.next();
			if (transaction.getId() == transactionNumber) {
				return transaction;
			}
		}
		return null;
	}
    
    public Transaction allocateNew(int id) {
    	Transaction t=begin(ID_GENERATOR.getAndIncrement());
    	if(t!=null) {
    		t.id=id;
    	}
    	return t;
    }
    
    /**
     * Starts new transaction.
     * 
     * @param id the identifier of new transaction.
     * @return the object which represents transaction.
     */
    private Transaction begin(int id) {
        Transaction t = transactionPool.poll();
        if (t == null) {
        	t=new Transaction(this);
        }
        t.uniqueId = id;
        activeTransactions.put(t.uniqueId,t);        
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
        return ID_GENERATOR.incrementAndGet();
    }
    
    /**
     * Gets the remainder of unused transaction objects.
     * Used for test purpose.
     * 
     * @return the number of available transaction objects.
     */
    protected int remainder() {
    	return transactionPool.size();        
    }
    
    private class Heartbeat extends Task {
    	int queueToClean;
    	
    	public Heartbeat() {
            super();
        }        

        @Override
        public long perform() {
        	queueToClean=(cleanIndex+1)%CACHE_SIZE;
        	Transaction current=cache[queueToClean].poll();
            while(current!=null)
            {
            	activeTransactions.remove(current.uniqueId);
            	current.id = 0;
            	current.uniqueId=0;
            	current.completed=false;
            	transactionPool.offer(current);
            	current=cache[queueToClean].poll();
            }
            
            cleanIndex=(cleanIndex+1)%CACHE_SIZE;
            scheduler.submitHeatbeat(this);
            return 0;
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.HEARTBEAT_QUEUE;
        }
    }
}
