/*
 * Mobicents Media Gateway
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */
package org.mobicents.media.server.testsuite.general;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Provides repited execution at a reqular time intervals.
 * 
 * @author Oleg Kulikov
 */
public class TimerImpl implements Timer {
	public static final int _DEFAULT_T_PRIORITY = Thread.MAX_PRIORITY;
	private transient final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new TimerThreadFactory());
	private int heartBeat = 20;

	/**
	 * Creates new instance of the timer.
	 */
	public TimerImpl() {
	}

	/**
	 * Gets value of interval between timer ticks.
	 * 
	 * @return the int value in milliseconds.
	 */
	public int getHeartBeat() {
		return heartBeat;
	}

	/**
	 * Modify interval between timer tick
	 * 
	 * @param heartBeat
	 *            the new value of interval in milliseconds.
	 */
	public void setHeartBeat(int heartBeat) {
		this.heartBeat = heartBeat;
	}

	/**
	 * Synchronizes task from this timer.
	 * 
	 * @param task
	 *            the task to be synchronized.
	 * @return the action which can be canceled to unsynchronize previously
	 *         synchronized task
	 */
	public ScheduledFuture synchronize(Runnable task) {
		return timer.scheduleAtFixedRate(task, 0, heartBeat, TimeUnit.MILLISECONDS);
	}

}

