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
import org.mobicents.media.server.scheduler.ConcurrentLinkedList;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Enumeration;

/**
 * Implements pool of transactions.
 * 
 * @author yulian oifa
 */
public class GlobalTransactionManager 
{	
	//one byte block
    private GlobalTransactionManager[] subManager;
    
    //ports block
    private TransactionManager[] managers;    
    
    //MGCP protocol provider
    protected MgcpProvider provider;
       
    //Scheduler
    private Scheduler scheduler;
    
    //Endpoint naming tree
    private NamingTree namingService;
    
    private int poolSize;
    
    private Lock lock=new Lock();
    
    public GlobalTransactionManager(Scheduler scheduler) 
    {
        this.scheduler = scheduler;
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
     * Associates endpoint naming service.
     * 
     * @param namingService the endpoint naming service.
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
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
    public Transaction find(InetSocketAddress source,int id) {    
    	return find(source.getAddress().getAddress(),source.getPort(),0,id);  
    }
    
    private Transaction find(byte[] address,int port,int level,int id) {    
    	if(level==address.length-1)
    	{
    		checkPort(port-1);
    		return managers[port-1].find(id);
    	}
    	
    	checkAddress(address[level]);
    	return subManager[address[level]].find(address, port, level+1, id);    	
    }
    
    public Transaction allocateNew(InetSocketAddress source,int id) {
    	return allocateNew(source.getAddress().getAddress(),source.getPort(),0,id);
    }
    
    private Transaction allocateNew(byte[] address,int port,int level,int id) {
    	if(level==address.length-1)
    	{    		
    		checkPort(port-1);
    		Transaction old=managers[port-1].find(id);
    		if(old==null)
    			return managers[port-1].allocateNew(id);
    		
    		return null;
    	}
    	
    	checkAddress(address[level]);	
    	return subManager[address[level]].allocateNew(address, port, level+1, id);    	
    }
    
    public TransactionManager createTransactionManager() {
    	TransactionManager txManager = new TransactionManager(scheduler, poolSize);
        txManager.setNamingService(namingService);
        txManager.setCallManager(new CallManager());
        txManager.setMgcpProvider(provider);
        txManager.start();
        return txManager;
    }
    
    private void checkPort(int portIndex) {
    	if(managers==null)
		{
    		try
    		{
    			lock.lock();
    		}
    		catch(java.lang.InterruptedException e)
    		{
    			
    		}
			
			if(managers==null)
			{
				managers=new TransactionManager[65536];				
				managers[portIndex]=createTransactionManager();    			
			}
			
			lock.unlock();
		}
		else if(managers[portIndex]==null)
		{
			try
    		{
    			lock.lock();
    		}
    		catch(java.lang.InterruptedException e)
    		{
    			
    		}
			
			if(managers[portIndex]==null)
				managers[portIndex]=createTransactionManager();
			
			lock.unlock();
		}
    }
    
    private void checkAddress(byte index) {
    	int intIndex = ((int)index) & 0xFF;
    	
    	if(subManager==null)
		{
    		try
    		{
    			lock.lock();
    		}
    		catch(java.lang.InterruptedException e)
    		{
    			
    		}
			
			if(subManager==null)
			{
				subManager=new GlobalTransactionManager[256];
				
				subManager[intIndex]=new GlobalTransactionManager(scheduler);
    			subManager[intIndex].setMgcpProvider(provider);
    			subManager[intIndex].setNamingService(namingService);
    			subManager[intIndex].setPoolSize(poolSize);
			}
			
			lock.unlock();
		}
		else if(subManager[intIndex]==null)
		{
			try
    		{
    			lock.lock();
    		}
    		catch(java.lang.InterruptedException e)
    		{
    			
    		}
			
			if(subManager[intIndex]==null)
			{
				subManager[intIndex]=new GlobalTransactionManager(scheduler);
    			subManager[intIndex].setMgcpProvider(provider);
    			subManager[intIndex].setNamingService(namingService);
    			subManager[intIndex].setPoolSize(poolSize);
			}
			
			lock.unlock();
		}
    }
    
    private class Lock {
        protected boolean locked;
        public Lock() {
            locked=false;
        }
        
        public synchronized void lock() throws InterruptedException {
            while (locked) wait();
            locked=true;
        }
        
        public synchronized void unlock() {
            locked=false;
            notify();
        }
    }
}
