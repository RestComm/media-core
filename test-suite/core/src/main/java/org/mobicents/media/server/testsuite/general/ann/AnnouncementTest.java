/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.testsuite.general.ann;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mobicents.media.server.testsuite.general.AbstractCall;
import org.mobicents.media.server.testsuite.general.AbstractTestCase;

/**
 * 
 * @author baranowb
 */
public class AnnouncementTest extends AbstractTestCase {

	transient Logger logger = Logger.getLogger(AnnouncementTest.class);

	@Override
	public AbstractCall getNewCall() {
		try {
			AbstractCall call = new AnnCall(this, super.callDisplay.getFileURL());
			ScheduledFuture<?> timeoutHandle = super.timeGuard.schedule(new AnnCallTimeOutTask(call), super.callDisplay
					.getCallDuration(), TimeUnit.MILLISECONDS);
			call.setTimeoutHandle(timeoutHandle);
			return call;
		} catch (IOException ex) {
			logger.error(ex);
		}
		return null;
	}

	protected class AnnCallTimeOutTask implements Runnable {
		private AbstractCall call;

		public AnnCallTimeOutTask(AbstractCall call) {
			this.call = call;
		}

		public void run() {
			call.timeOut();
		}

	}
}
