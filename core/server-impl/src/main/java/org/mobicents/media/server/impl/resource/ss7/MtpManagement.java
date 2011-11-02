package org.mobicents.media.server.impl.resource.ss7;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.Server;
import org.mobicents.media.server.impl.clock.LocalTask;
import org.mobicents.media.server.spi.clock.Task;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MtpManagement implements Task {

	private static final Logger logger = Logger.getLogger(MtpManagement.class);

	private ShellExecutor shellExecutor = null;
	private M3UALayer4 m3UALayer4 = null;

	private LocalTask task = null;
	private boolean isActive = false;

	public MtpManagement() {

	}

	public ShellExecutor getShellExecutor() {
		return shellExecutor;
	}

	public void setShellExecutor(ShellExecutor shellExecutor) {
		this.shellExecutor = shellExecutor;
	}

	public M3UALayer4 getM3UALayer4() {
		return m3UALayer4;
	}

	public void setM3UALayer4(M3UALayer4 layer4) {
		m3UALayer4 = layer4;
	}

	/**
	 * Life Cycle methods
	 */
	public void create() {

	}

	public void start() throws Exception {
	    Server.scheduler.start();
		task = Server.scheduler.execute(this);
	}

	public void stop() {
		task.cancel();
	}

	public void destroy() {

	}

	/**
	 * Task methods implementation
	 */
	public void cancel() {
		this.isActive = false;
	}

	public boolean isActive() {
		return this.isActive;
	}

	public int perform() {

		try {
			this.shellExecutor.perform();
			this.m3UALayer4.perform();
			// Management
		} catch (IOException e) {
			logger.error("IOException ", e);
		}

		return 1;
	}

}
