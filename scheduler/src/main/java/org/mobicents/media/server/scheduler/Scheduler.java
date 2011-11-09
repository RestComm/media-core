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

package org.mobicents.media.server.scheduler;

import java.lang.InterruptedException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

/**
 * Implements scheduler with multi-level priority queue.
 *
 * This scheduler implementation follows to uniprocessor model with "super" thread.
 * The "super" thread includes IO bound thread and one or more CPU bound threads
 * with equal priorities.
 *
 * The actual priority is assigned to task instead of process and can be
 * changed dynamically at runtime using the initial priority level, feedback
 * and other parameters.
 *
 *
 * @author kulikov
 */
public class Scheduler  {
	//MANAGEMENT QUEUE SHOULD CONTAIN ONLY TASKS THAT ARE NOT TIME DEPENDENT , FOR
	//EXAMPLE MGCP COMMANDS 
	public static final Integer MANAGEMENT_QUEUE=0;
	public static final Integer UDP_MANAGER_QUEUE=1;
	public static final Integer RX_TASK_QUEUE=2;
	public static final Integer INPUT_QUEUE=3;
	public static final Integer SPLITTER_INPUT_QUEUE=4;
	public static final Integer SPLITTER_OUTPUT_QUEUE=5;
	public static final Integer MIXER_INPUT_QUEUE=6;
	public static final Integer MIXER_MIX_QUEUE=7;
	public static final Integer MIXER_OUTPUT_QUEUE=8;
	public static final Integer OUTPUT_QUEUE=9;
	//TX Task is called from OUTPUT AND NOT ADDED TO SCHEDULER THEREFORE NOT NEEDED HERE
	
	public static final Integer HEARTBEAT_QUEUE=-1;
    //The clock for time measurement
    private Clock clock;

    //priority queue
    protected OrderedTaskQueue[] taskQueues = new OrderedTaskQueue[10];

    protected OrderedTaskQueue heartBeatQueue;
    //CPU bound threads
    private CpuThread cpuThread;
    
    //flag indicating state of the scheduler
    private boolean isActive;

    private Logger logger = Logger.getLogger(Scheduler.class) ;
    
    /**
     * Creates new instance of scheduler.
     */
    public Scheduler() {
    	for(int i=0;i<taskQueues.length;i++)
    		taskQueues[i]=new OrderedTaskQueue();
    	
    	heartBeatQueue=new OrderedTaskQueue();
    	
        cpuThread = new CpuThread(String.format("Scheduler"));
    }

    /**
     * Sets clock.
     *
     * @param clock the clock used for time measurement.
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Gets the clock used by this scheduler.
     *
     * @return the clock object.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Queues task for execution according to its priority.
     *
     * @param task the task to be executed.
     */
    public void submit(Task task,Integer index) {
        task.activate(false);
        taskQueues[index].accept(task);
    }
    
    /**
     * Queues task for execution according to its priority.
     *
     * @param task the task to be executed.
     */
    public void submitHeatbeat(Task task) {
        task.activate(true);
        heartBeatQueue.accept(task);
    }
    
    /**
     * Queues chain of the tasks for execution.
     * 
     * @param taskChanin the chain of the tasks
     */
    public void submit(TaskChain taskChain) {    	
        taskChain.start(MANAGEMENT_QUEUE);
    }    
    
    /**
     * Starts scheduler.
     */
    public void start() {
        if (clock == null) {
            throw new IllegalStateException("Clock is not set");
        }

        this.isActive = true;
        
        logger.info("Starting ");
        
        cpuThread.activate();
        
        logger.info("Started ");
    }

    /**
     * Stops scheduler.
     */
    public void stop() {
        if (!this.isActive) {
            return;
        }

        cpuThread.shutdown();
        try
        {
        	Thread.sleep(40);
        }
        catch(InterruptedException e)
		{                				
		}
        
        for(int i=0;i<taskQueues.length;i++)
        	taskQueues[i].clear();
        
        heartBeatQueue.clear();
    }

    //removed statistics to increase perfomance
    /**
     * Shows the miss rate.
     * 
     * @return the miss rate value;
     */
    public double getMissRate() {
        return 0;
    }

    public long getWorstExecutionTime() {
        return 0;
    }

    public void notifyCompletion()
    {
    	cpuThread.notifyCompletion();
    }
    
    /**
     * Executor thread.
     */
    private class CpuThread extends Thread {        
        private volatile boolean active;
        private int currQueue=0;        
        private AtomicInteger activeTasksCount=new AtomicInteger();
        private long cycleStart=0;
        private ExecutorService eservice;
        private int runIndex=0;
        private Object LOCK=new Object();
        
        public CpuThread(String name) {
            super(name);
            
            int nrOfProcessors = Runtime.getRuntime().availableProcessors();
            eservice = Executors.newFixedThreadPool(2*nrOfProcessors);            
        }
        
        public void activate() {
        	this.active = true;
        	cycleStart = clock.getTime();
        	this.start();
        }
        
        public void notifyCompletion() {
        	int newValue=activeTasksCount.decrementAndGet();
        	if(newValue==0 && this.active)
        		synchronized(LOCK) {
        			LOCK.notify();
        		}        	        	
        }
        
        @Override
        public void run() {        	
        	long cycleDuration;
        	
        	while(true)
        	{
        		while(currQueue<=OUTPUT_QUEUE)
    			{    				    				
    				synchronized(LOCK) {    					
    					if(executeQueue(taskQueues[currQueue]))
    						try {
    							LOCK.wait();
    						}
    						catch(InterruptedException e)  {                                               
    							//lets continue
    						}
    				}
    				
    				currQueue++;
    			}
        		        		
        		cycleDuration=clock.getTime() - cycleStart;
				if(cycleDuration<18000000L)					
					synchronized(LOCK) {						
						if(executeQueue(taskQueues[MANAGEMENT_QUEUE]))
							try  {
								LOCK.wait();
							}
							catch(InterruptedException e)  {                                               
							//lets continue
							}
					}
        	
				runIndex=(runIndex+1)%5;
    			if(runIndex==0)    				    				
    				synchronized(LOCK) {
    					if(executeQueue(heartBeatQueue))
    						try  {
    							LOCK.wait();
    						}
    						catch(InterruptedException e)  {                                               
    							//lets continue
    						}
    				}    				
            
        		//sleep till next cycle
        		cycleDuration=clock.getTime() - cycleStart;
        		if(cycleDuration<20000000L)
        			try  {                                               
        				sleep(20L-cycleDuration/1000000L,(int)((20000000L-cycleDuration)%1000000L));
        			}
                	catch(InterruptedException e)  {                                               
                		//lets continue
                	}
    		
                //new cycle starts , updating cycle start time by 20ms
                cycleStart = cycleStart + 20000000L;
                currQueue=MANAGEMENT_QUEUE;                                               
        	}
        }
        
        private boolean executeQueue(OrderedTaskQueue currQueue)
        {
        	Task t;        	
        	currQueue.changePool();
            int currQueueSize=currQueue.size();
            activeTasksCount.set(currQueueSize);
            t = currQueue.poll();
            //submit all tasks in current queue
            while(t!=null)
            {            	
            	eservice.submit(t);
            	t = currQueue.poll();
            }
            
            return currQueueSize!=0;
        }

        /**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
    }

}
