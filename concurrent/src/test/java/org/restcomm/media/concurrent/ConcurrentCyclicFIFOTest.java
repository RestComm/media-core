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

package org.restcomm.media.concurrent;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.media.concurrent.ConcurrentCyclicFIFO;

public class ConcurrentCyclicFIFOTest 
{
	private ConcurrentCyclicFIFO<Integer> queue=new ConcurrentCyclicFIFO<Integer>();
	private LinkedBlockingQueue<Integer> cqueue=new LinkedBlockingQueue<Integer>();
	
	private static final int step=10000;
	private static final int size=50;
	
	private AtomicInteger counter=new AtomicInteger(0);
	
	private Sender[] senders=new Sender[size];
	private Receiver[] receivers=new Receiver[size];
	private ConcurrentSender[] csenders=new ConcurrentSender[size];
	private ConcurrentReceiver[] creceivers=new ConcurrentReceiver[size];
	
	private Semaphore semaphore=new Semaphore(1-size);
	
	public ConcurrentCyclicFIFOTest()
	{		
	}
	
	@BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test @Ignore
    public void testSpeed() {
    	long endTime,startTime;
    	long totalGCTime=0,totalTime=0;
    	
    	for(int j=0;j<50;j++)
    	{
    		for(int i=0;i<size;i++)
    		{
    			senders[i]=new Sender(step*i);
    			csenders[i]=new ConcurrentSender(step*i);
    			receivers[i]=new Receiver();
    			creceivers[i]=new ConcurrentReceiver();
    		}
    		
    		semaphore=new Semaphore(1-size);
    		counter.set(0);
    		startTime=System.currentTimeMillis();
    	
    		for(int i=0;i<size;i++)
    		{
    			csenders[i].start();
    			creceivers[i].start();
    		}
    	
    		try
    		{
    			semaphore.acquire();
    		}
    		catch(Exception ex)
    		{
    		
    		}
    	
    		System.gc();
    		endTime=System.currentTimeMillis();
    		totalGCTime+=(endTime-startTime);
    		
    		semaphore=new Semaphore(1-size);
    		counter.set(0);
    	
    		startTime=System.currentTimeMillis();
    	
    		for(int i=0;i<size;i++)
    		{
    			senders[i].start();
    			receivers[i].start();
    		}
    	
    		try
    		{
    			semaphore.acquire();
    		}
    		catch(Exception ex)
    		{
    		
    		}
    	
    		System.gc();
    		endTime=System.currentTimeMillis();
    		totalTime+=(endTime-startTime);    		    	
    	}
    	
    	assertTrue(totalGCTime>totalTime);
    	
    	System.out.println("FIFO time:" + totalTime);
    	System.out.println("Concurrent time:" + totalGCTime);
    }
	private class Sender extends Thread
	{
		int start=0;
		
		public Sender(int start)
		{
			this.start=start;
		}
		
		public void run()
		{
			for(int i=0;i<step;i++)
				queue.offer(new Integer(start+i));
		}
	}
	
	private class ConcurrentSender extends Thread
	{
		int start=0;
		
		public ConcurrentSender(int start)
		{
			this.start=start;
		}
		
		public void run()
		{
			for(int i=0;i<step;i++)
				cqueue.offer(new Integer(start+i));
		}
	}
	
	private class Receiver extends Thread
	{
		public void run()
		{
			Integer result;
			
			while(counter.getAndIncrement()<size*step)
			{
				result=null;
				while(result==null)
					try
					{
						result=queue.take();
					}
					catch(Exception ex)
					{
						
					}
			}
			
			semaphore.release();
		}
	}
	
	private class ConcurrentReceiver extends Thread
	{				
		public void run()
		{
			Integer result;
			
			while(counter.getAndIncrement()<size*step)
			{
				result=null;
				while(result==null)
					try
					{
						result=cqueue.take();
					}
					catch(Exception ex)
					{
						
					}
			}
			
			semaphore.release();
		}
	}
}