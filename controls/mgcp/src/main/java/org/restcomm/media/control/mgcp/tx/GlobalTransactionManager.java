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

package org.restcomm.media.control.mgcp.tx;

import java.net.InetSocketAddress;

import org.restcomm.media.concurrent.Lock;
import org.restcomm.media.control.mgcp.MgcpProvider;
import org.restcomm.media.control.mgcp.controller.CallManager;
import org.restcomm.media.control.mgcp.controller.naming.NamingTree;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.Scheduler;

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
    private final Scheduler scheduler;
    private final Clock clock;
    
    //Endpoint naming tree
    private NamingTree namingService;
    
    private int poolSize;
    
    private Lock lock=new Lock();
    
    public GlobalTransactionManager(Scheduler scheduler, Clock clock) 
    {
        this.scheduler = scheduler;
        this.clock = clock;
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
        return this.clock.getTime();
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
    	
    	int intIndex = ((int)address[level]) & 0xFF;    				       	
    	checkAddress(intIndex);
    	return subManager[intIndex].find(address, port, level+1, id);    	
    }
    
    public Transaction allocateNew(InetSocketAddress source,int id) {
    	return allocateNew(source.getAddress().getAddress(),source.getPort(),0,id);
    }
    
    private Transaction allocateNew(byte[] address,int port,int level,int id) {
    	if(level==address.length-1)
    	{    		
    		checkPort(port-1);
    		// hrosa - look for transaction by tx number rather than unique id
    		// Fixes issue MEDIA-20
    		Transaction old=managers[port-1].findByTransactionNumber(id);
			if (old == null) {
				return managers[port - 1].allocateNew(id);
			} else {
				return null;
			}
    	}
    	
    	int intIndex = ((int)address[level]) & 0xFF;    				       	
    	checkAddress(intIndex);
    	return subManager[intIndex].allocateNew(address, port, level+1, id);    	
    }
    
    public TransactionManager createTransactionManager() {
    	TransactionManager txManager = new TransactionManager(clock, scheduler, poolSize);
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
    
    private void checkAddress(int intIndex) {
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
				
				subManager[intIndex]=new GlobalTransactionManager(scheduler, clock);
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
				subManager[intIndex]=new GlobalTransactionManager(scheduler, clock);
    			subManager[intIndex].setMgcpProvider(provider);
    			subManager[intIndex].setNamingService(namingService);
    			subManager[intIndex].setPoolSize(poolSize);
			}
			
			lock.unlock();
		}
    }    
}
