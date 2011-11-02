/**
 * Start time:12:14:30 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.media.server.testsuite.general;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Start time:12:14:30 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 */
public class TimerThreadFactory implements ThreadFactory {
	public static final int _DEFAULT_T_PRIORITY = Thread.MAX_PRIORITY;
	public static final AtomicLong sequence = new AtomicLong(0);
	private ThreadGroup factoryThreadGroup = new ThreadGroup("MMSToolClockThreadGroup[" + sequence.incrementAndGet() + "]");

	public Thread newThread(Runnable r) {
		Thread t = new Thread(this.factoryThreadGroup, r);
		t.setPriority(_DEFAULT_T_PRIORITY);
		// ??
		//t.start();
		return t;
	}
}
