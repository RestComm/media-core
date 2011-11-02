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

    /** the amount of tasks missed their deadline */
    private volatile long missCount;

    /** the number of total tasks executed */
    private volatile long taskCount;

    /** The allowed time jitter */
    private long tolerance = 3000000L;

    //The most worst execution time detected
    private long wet;

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
        
        cpuThread.start();
        
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

    /**
     * Shows the miss rate.
     * 
     * @return the miss rate value;
     */
    public double getMissRate() {
        return taskCount > 0 ? (double)missCount/(double)taskCount : 0D;
    }

    public long getWorstExecutionTime() {
        return wet;
    }

    /**
     * Executor thread.
     */
    private class CpuThread extends Thread {
        private Task t;
        private volatile boolean active;

        public CpuThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            this.active = true;
            Integer queueIndex=0;
            long cycleStart = clock.getTime();
            int runIndex=0;
            long duration;
            long cycleDuration;
            
            while (active) {
                //load task with highest priority and execute it.
                t = taskQueues[queueIndex].poll();
                
                //if task has been canceled take another one
                if (t == null) {
                	//cycle completed
                	if(queueIndex==taskQueues.length-1)
                	{                		                		
                		//run here tasks from management pool if have time
                		cycleDuration=clock.getTime() - cycleStart;
                		
                		if(cycleDuration<20000000L)
                		{
                			taskQueues[MANAGEMENT_QUEUE].changePool();
                			t=taskQueues[MANAGEMENT_QUEUE].poll();
                		}
                		
                		while(cycleDuration<20000000L && t!=null)
                		{
                			try {
                                //update miss rate countor
                                long now = clock.getTime();

                                //increment task countor
                                taskCount++;

                                //execute task
                                t.run();

                                //determine worst execution time
                                duration = clock.getTime() - now;
                                if (duration > wet) {
                                    wet = duration;
                                }
                            } catch (Exception e) {
                            }
                            
                            cycleDuration=clock.getTime() - cycleStart;
                            t=taskQueues[MANAGEMENT_QUEUE].poll();
                		}                		
                		
                		//if still have time should sleep
                		if(cycleDuration<20000000L)         
                			try
                			{                				
                				this.sleep(20L-cycleDuration/1000000L,(int)((20000000L-cycleDuration)%1000000L));
                			}
                			catch(InterruptedException e)
                			{                				
                				//lets continue
                			}
                		
                		//new cycle started
                		cycleStart = cycleStart + 20000000L;
                		runIndex=(runIndex+1)%5;
                		
                		//new cycle started , run heartbeat queues if needed
                		if(runIndex==0)
                		{
                			heartBeatQueue.changePool();
                			t=heartBeatQueue.poll();
                			
                			while(t!=null)
                    		{
                    			try {
                                    //update miss rate countor
                                    long now = clock.getTime();

                                    //increment task countor
                                    taskCount++;

                                    //execute task
                                    t.run();

                                    //determine worst execution time
                                    duration = clock.getTime() - now;
                                    if (duration > wet) {
                                        wet = duration;
                                    }
                                } catch (Exception e) {
                                }
                                
                                t=heartBeatQueue.poll();
                    		} 
                		}
                	}
                	
                	queueIndex=(queueIndex+1)%taskQueues.length;
                	taskQueues[queueIndex].changePool();
                    continue;
                }

                try {
                    //update miss rate countor
                    long now = clock.getTime();

                    //increment task countor
                    taskCount++;

                    //execute task
                    t.run();

                    //determine worst execution time
                    duration = clock.getTime() - now;
                    if (duration > wet) {
                        wet = duration;
                    }
                } catch (Exception e) {
                }
            }            
        }

        /**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
    }

}
