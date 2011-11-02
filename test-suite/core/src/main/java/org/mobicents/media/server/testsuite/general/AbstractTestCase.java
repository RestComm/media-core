/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.testsuite.general;

import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Notify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sdp.Attribute;
import javax.sdp.SdpFactory;

import org.mobicents.media.server.testsuite.general.file.FileUtils;
import org.mobicents.media.server.testsuite.general.rtp.RtpPacket;
import org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory;
import org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactoryImpl;
import org.mobicents.media.server.testsuite.gui.ext.CallStateTableModel;
import org.mobicents.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.mgcp.stack.JainMgcpStackProviderImpl;

/**
 * 
 * @author baranowb
 */
public abstract class AbstractTestCase implements JainMgcpExtendedListener,
		Runnable, Serializable {

	protected transient Logger logger = Logger.getLogger(this.getClass()
			.getName());
	private TestState testState = TestState.Stoped;
	public transient final static String _CASE_FILE = "testcase.bin";
	public transient final static String _COLLECTIVE_RTP_FILE = "rtp.txt";
	public static final String _LINE_SEPARATOR;
	static {
		String lineSeparator = System.getProperty("line.separator");
		_LINE_SEPARATOR = lineSeparator;
	}

	// Dump section
	private transient File _RTP_TXT_DUMP_FILE;
	private transient FileOutputStream _RTP_TXT_DUMP_FOS;
	private transient OutputStreamWriter _RTP_TXT_DUMP_OSW;

	// private transient FileInputStream _RTP_TXT_DUMP_FIS;
	// private transient InputStreamReader _RTP_TXT_DUMP_ISR;

	public static final int _TURN_OFF_BOUNDRY = -1;
	// Yes, it would be good thing to ser
	protected transient SdpFactory sdpFactory;
	protected transient CallDisplayInterface callDisplay;
	protected Map<Long, AbstractCall> callSequenceToCall;

	// We mix view, but this is easier to achieve perf with that.
	protected transient CallStateTableModel model;
	// protected part - some variables that we might use.
	protected InetAddress clientTestNodeAddress;
	protected InetAddress serverJbossBindAddress;

	// timestamp :), its used for files
	protected long testTimesTamp = System.currentTimeMillis();
	protected transient File testDumpDirectory;

	protected transient ScheduledFuture callCreatorTask;
	protected transient ScheduledFuture gracefulStopTask;
	// Timer guard:
	protected transient final ScheduledExecutorService timeGuard;
	// 
	protected transient final ScheduledExecutorService executors;

	// Some getters
	// Some stats
	protected long ongoingCallNumber;
	protected long errorCallNumber;
	protected long completedCallNumber;
	protected long totalCalls;
	protected long maxErrorCallNumber;

	protected transient RtpSocketFactory socketFactory;

	// FIXME: this will go into MGCP test case, will be removed from here
	// some mgcp magic
	protected transient JainMgcpStackImpl stack;
	protected transient JainMgcpStackProviderImpl provider;
	// We need this to map TXID to Call :)
	protected transient Map<Integer, AbstractCall> mgcpTransactionToProxy = new HashMap<Integer, AbstractCall>();
	protected transient Map<String, AbstractCall> requestIdIdToProxy = new HashMap<String, AbstractCall>();

	public AbstractTestCase() {
		this.callSequenceToCall = new HashMap<Long, AbstractCall>();
		// model = new CallStateTableModel(this.callSequenceToCall);
		AbstractCall.resetSequence();
		NamedThreadFactory executorsThreadFactory = new NamedThreadFactory(
				"ExecutorsTestCaseFactory");
		NamedThreadFactory timeGuardThreadFactory = new NamedThreadFactory(
				"GuardThreadFactoryTestCaseFactory");
		executors = Executors.newScheduledThreadPool(2, executorsThreadFactory);
		timeGuard = Executors.newScheduledThreadPool(1, timeGuardThreadFactory);

	}

	private void init() throws SocketException, IOException {

		// init streams
		boolean finished = false;
		try {
			_RTP_TXT_DUMP_FOS = new FileOutputStream(_RTP_TXT_DUMP_FILE);
			_RTP_TXT_DUMP_OSW = new OutputStreamWriter(_RTP_TXT_DUMP_FOS);

			// _RTP_TXT_DUMP_FIS = new FileInputStream(_RTP_TXT_DUMP_FILE);
			// _RTP_TXT_DUMP_ISR = new InputStreamReader(_RTP_TXT_DUMP_FIS);
			finished = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!finished) {
				if (_RTP_TXT_DUMP_OSW != null) {
					_RTP_TXT_DUMP_OSW.close();
					_RTP_TXT_DUMP_OSW = null;
					_RTP_TXT_DUMP_FOS = null;
				}

			}
		}

		if (this.socketFactory == null) {
			this.socketFactory = new RtpSocketFactoryImpl();
			// this.socketFactory.setPortRange("5000-10000");

		}

		if (this.socketFactory != null) {

			this.socketFactory.setBindAddress(this.callDisplay
					.getLocalAddress());
			this.socketFactory.setFormatMap(this.callDisplay.getCodecs());
			this.socketFactory.start();
		}
	}

	public OutputStreamWriter getRtpOSW() {
		return _RTP_TXT_DUMP_OSW;
	}

	public InputStreamReader getRtpISR() {
		FileInputStream _RTP_TXT_DUMP_FIS;
		try {
			_RTP_TXT_DUMP_FIS = new FileInputStream(_RTP_TXT_DUMP_FILE);
			InputStreamReader _RTP_TXT_DUMP_ISR = new InputStreamReader(
					_RTP_TXT_DUMP_FIS);
			return _RTP_TXT_DUMP_ISR;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public ScheduledExecutorService getExecutors() {
		return executors;
	}

	protected void incrementOngoignCall() {
		this.ongoingCallNumber++;
		this.totalCalls++;
	}

	protected void decrementOngoingCall() {
		this.ongoingCallNumber--;
	}

	protected void incrementErrorCall() {
		this.errorCallNumber++;
	}

	protected void incrementCompletedCall() {

		this.completedCallNumber++;
	}

	public long getTestTimeStamp() {
		return this.testTimesTamp;
	}

	public InetAddress getClientTestNodeAddress() {
		return this.clientTestNodeAddress;
	}

	public InetAddress getServerJbossBindAddress() {
		return this.serverJbossBindAddress;
	}

	public CallDisplayInterface getCallDisplayInterface() {
		return this.callDisplay;
	}

	public AbstractCall getCallBySequence(Long seq) {

		AbstractCall ac = this.callSequenceToCall.get(seq);
		if (ac != null)
			ac.setTestCase(this);
		return ac;
	}

	public void callStateChanged(AbstractCall c) {

		CallState callState = c.getState();

		if (callState == CallState.INITIAL) {

			this.incrementOngoignCall();

		} else if (callState == CallState.ENDED) {
			this.decrementOngoingCall();
			this.incrementCompletedCall();
			// as soon as we end one call, we should try to start another.
			this.checkForCallInit();
		} else if (callState == CallState.IN_ERROR) {
			this.decrementOngoingCall();
			this.incrementErrorCall();
			// as soon as we end one call, we should try to start another.
			this.checkForCallInit();
		}

		// System.err.println("updateCallView:"+this.ongoingCallNumber);
		this.callDisplay.updateCallView();
		// this is forterm;
		if (this.testState == TestState.Terminating) {
			if (getOngoingCallNumber() == 0) {
				this.stop(false);
			}
		}
	}

	public CallStateTableModel getTableModel() {
		return this.model;
	}

	public long getCompletedCallNumber() {

		return this.completedCallNumber;
	}

	public long getErrorCallNumber() {
		return this.errorCallNumber;
	}

	public long getOngoingCallNumber() {

		return this.ongoingCallNumber;
	}

	public void setMaxErrorCallNumber(long v) {
		this.maxErrorCallNumber = v;
	}

	public long getTotalCallNumber() {
		return this.totalCalls;
	}

	public void stop(boolean onGracefull) {

		synchronized (this.testState) {
			switch (this.testState) {
			case Terminating:

				if (!onGracefull) {
					return;
				}

				try {

					if (this.provider != null) {
						try {
							this.provider.removeJainMgcpListener(this);
							this.stack.deleteProvider(this.provider);

						} catch (Exception e) {
							e.printStackTrace();
						}

					}

					if (this.stack != null) {
						try {
							this.stack.close();
						} catch (Exception e) {
							e.printStackTrace();
						}

					}

					if (callCreatorTask != null) {

						callCreatorTask.cancel(true);
					}

					// FIXME: add more?

					try {
						for (AbstractCall call : this.callSequenceToCall
								.values()) {
							if (call.getState() == CallState.ESTABILISHED
									|| call.getState() == CallState.INITIAL) {
								call.stop();
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

					if (_RTP_TXT_DUMP_OSW != null) {
						try {
							_RTP_TXT_DUMP_OSW.flush();
							_RTP_TXT_DUMP_OSW.close();
							_RTP_TXT_DUMP_OSW = null;
							_RTP_TXT_DUMP_FOS = null;
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
					if (this.socketFactory != null)
						try {
							this.socketFactory.stop();

						} catch (Exception e) {
							e.printStackTrace();
						}
					// Now lets serialize.

					serialize();
					// dumpSampleTraffic();
				} finally {
					this.testState = TestState.Stoped;
					this.gracefulStopTask = null;
					if (this.timeGuard != null) {
						this.timeGuard.shutdownNow();
					}
					if (this.executors != null) {
						this.executors.shutdownNow();
					}

				}
				this.testState = TestState.Stoped;
				break;

			case Running:

				this.testState = TestState.Terminating;

				// this.gracefulStopTask = this.
				// so we dont have to press stop twice, this is stupid.

				if (this.gracefulStopTask == null) {

					this.gracefulStopTask = this.executors.schedule(
							new GracefulStopTask(this), this.callDisplay
									.getCallDuration() + 1000,
							TimeUnit.MILLISECONDS);
				}
				break;

			default:

				break;

			}
		}

	}

	public void start() throws CreateProviderException,
			TooManyListenersException {
		try {

			stop(false);
			this.clientTestNodeAddress = InetAddress.getByName(this.callDisplay
					.getLocalAddress());
			this.serverJbossBindAddress = InetAddress
					.getByName(this.callDisplay.getRemoteAddress());

			this.stack = new JainMgcpStackImpl(this.clientTestNodeAddress,
					this.callDisplay.getLocalPort());

			this.provider = (JainMgcpStackProviderImpl) this.stack
					.createProvider();

			this.provider.addJainMgcpListener(this);
			testState = TestState.Running;
			onCPSChange();
		} catch (UnknownHostException ex) {
			Logger.getLogger(AbstractTestCase.class.getName()).log(
					Level.SEVERE, null, ex);
		}

	}

	public TestState getTestState() {
		return this.testState;
	}

	public RtpSocketFactory getSocketFactory() {
		return socketFactory;
	}

	public void setCallDisplay(CallDisplayInterface cdi)
			throws IllegalStateException, SocketException, IOException {
		this.callDisplay = cdi;

		this.clientTestNodeAddress = InetAddress.getByName(this.callDisplay
				.getRemoteAddress());
		this.serverJbossBindAddress = InetAddress.getByName(this.callDisplay
				.getRemoteAddress());

		this.sdpFactory = SdpFactory.getInstance();
		this.testDumpDirectory = new File(cdi.getDefaultDataDumpDirectory(), ""
				+ this.testTimesTamp);

		if (!this.testDumpDirectory.exists()) {

			if (!this.testDumpDirectory.mkdirs()) {
				throw new IllegalStateException("Failed to create dirs: "
						+ this.testDumpDirectory);
			}
		} else {
			// This shoudl not happen, but just in case.
			if (this.testDumpDirectory.isDirectory()
					&& this.testDumpDirectory.canWrite()) {

			} else {
				throw new IllegalStateException(
						"Failed to validate dump dir, its either not writeable or is not a directory: "
								+ this.testDumpDirectory);
			}
		}
		_RTP_TXT_DUMP_FILE = new File(this.testDumpDirectory,
				_COLLECTIVE_RTP_FILE);
		this.init();

	}

	// This method is used on loaded test case
	public void setCallDisplay(CallDisplayInterface cdi, File testDumpDirectory)
			throws UnknownHostException, IllegalStateException {
		this.callDisplay = cdi;
		this.sdpFactory = SdpFactory.getInstance();
		// this.testDumpDirectory = new
		// File(testDumpDirectory,""+this.testTimesTamp);
		this.testDumpDirectory = testDumpDirectory;
		_RTP_TXT_DUMP_FILE = new File(this.testDumpDirectory,
				_COLLECTIVE_RTP_FILE);
		model = new CallStateTableModel(this.callSequenceToCall);
		for (AbstractCall call : this.callSequenceToCall.values()) {
			call.setDumpDir(testDumpDirectory);
		}

	}

	public void onCPSChange() {

		if (testState == TestState.Stoped) {
			return;
		}
		// we changed CPS.
		if (this.callCreatorTask != null) {
			this.callCreatorTask.cancel(true);
		}
		int cps = this.getCallDisplayInterface().getCPS();
		if (cps == 0) {
			return;
		}
		int delta = 1000 / this.getCallDisplayInterface().getCPS();
		// we use delta,delta, cause we dont want sudden rush in CPS
		this.callCreatorTask = this.executors.scheduleAtFixedRate(this, delta,
				delta, TimeUnit.MILLISECONDS);
		// this.run();

	}

	public void onCallLengthChange() {

	}

	public abstract AbstractCall getNewCall();

	Vector<Attribute> getSDPAttributes() {
		return this.callDisplay.getCodecs();
	}

	public SdpFactory getSdpFactory() {
		return this.sdpFactory;
	}

	public File getTestDumpDirectory() {
		return this.testDumpDirectory;
	}

	// run in which we create more calls :)
	public void run() {
		// For some twisted reason constructo does not work...
		// model.setCallData(this.callSequenceToCall);
		if (this.testState == TestState.Running) {

			if (this.maxErrorCallNumber != _TURN_OFF_BOUNDRY
					&& this.errorCallNumber >= this.maxErrorCallNumber) {

			}

			if (this.callDisplay.getMaxConcurrentCalls() != _TURN_OFF_BOUNDRY
					&& this.ongoingCallNumber >= this.callDisplay
							.getMaxConcurrentCalls()) {

				return;
			}
			if (this.callDisplay.getMaxCalls() != _TURN_OFF_BOUNDRY
					&& this.totalCalls == this.callDisplay.getMaxCalls()) {

				this.stop(false);
				return;
			}
			try {

				// This creates call, which knows how to estabilish itself and
				// how long it should linger on as active.
				AbstractCall c = this.getNewCall();

				this.callSequenceToCall.put(c.getSequence(), c);
				callStateChanged(c);
				c.start();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void checkForCallInit() {
		if (this.callDisplay.getMaxConcurrentCalls() != -1
				&& this.callDisplay.getCPS() > 0) {
			try {
				run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// some handy methods
	public JainMgcpStackProviderImpl getProvider() {
		return this.provider;
	}

	// Event handlers
	public void processMgcpCommandEvent(JainMgcpCommandEvent command) {

		// For now we dont care for reqeust sent from MMS
		if (command instanceof Notify) {
			Notify notify = (Notify) command;
			AbstractCall cp = getCall(notify.getRequestIdentifier().toString());

			if (cp != null) {
				cp.processMgcpCommandEvent(command);
			}
		}
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent response) {

		// System.out.println("Recived response "+ response);

		try {

			AbstractCall cp = getCall(response);
			if (cp != null) {
				cp.processMgcpResponseEvent(response);
			} else {
				System.err.println("NO CALL");
			}
		} catch (RuntimeException re) {
			re.printStackTrace();
		}

	}

	public void transactionEnded(int arg0) {
		AbstractCall cp = getCall(arg0);
		if (cp != null) {
			cp.transactionEnded(arg0);
		} else {
			logger.severe("No call proxy for txid: " + arg0);
		}

	}

	public void transactionRxTimedOut(JainMgcpCommandEvent commandTimedOut) {
		AbstractCall cp = getCall(commandTimedOut);
		if (cp != null) {
			cp.transactionRxTimedOut(commandTimedOut);
		} else {
			logger.severe("No call proxy for txid: "
					+ commandTimedOut.getTransactionHandle()
					+ " for timed out event");
		}

	}

	public void transactionTxTimedOut(JainMgcpCommandEvent commandTimeOut) {
		AbstractCall cp = getCall(commandTimeOut);
		if (cp != null) {
			cp.transactionTxTimedOut(commandTimeOut);
		} else {
			logger.severe("No call proxy for txid: "
					+ commandTimeOut.getTransactionHandle()
					+ " for timed out event2");
		}
	}

	// CALL MGMT
	protected AbstractCall getCall(JainMgcpEvent mgcpEvent) {
		return this.mgcpTransactionToProxy
				.get(mgcpEvent.getTransactionHandle());
	}

	protected AbstractCall getCall(int txID) {
		return this.mgcpTransactionToProxy.get(txID);
	}

	public void removeCall(JainMgcpEvent mgcpEvent) {
		this.removeCall(mgcpEvent.getTransactionHandle());
	}

	public void removeCall(int txID) {
		this.mgcpTransactionToProxy.remove(txID);
	}

	public void addCall(String ri, AbstractCall cp) {
		this.requestIdIdToProxy.put(ri, cp);

	}

	public void removeCall(String ri) {
		this.requestIdIdToProxy.remove(ri);
	}

	public AbstractCall getCall(String ri) {
		return this.requestIdIdToProxy.get(ri);
	}

	public void addCall(JainMgcpEvent mgcpEvent, AbstractCall cp) {

		this.mgcpTransactionToProxy.put(mgcpEvent.getTransactionHandle(), cp);
	}

	/**
	 * Custom deserialization is needed.
	 */
	private void readObject(ObjectInputStream aStream) throws IOException,
			ClassNotFoundException {
		aStream.defaultReadObject();

	}

	/**
	 * Perofrms all serialization actions
	 */
	protected void serialize() {
		this.localAddress = this.callDisplay.getLocalAddress();
		this.localPort = this.callDisplay.getLocalPort();
		this.remoteAddress = this.callDisplay.getRemoteAddress();
		this.remotePort = this.callDisplay.getRemotePort();
		this.cps = this.callDisplay.getCPS();
		this.callDuration = this.callDisplay.getCallDuration();
		this.maxCalls = this.callDisplay.getMaxCalls();
		this.maxConcurrentCalls = this.callDisplay.getMaxConcurrentCalls();
		this.maxFailCalls = this.callDisplay.getMaxFailCalls();

		FileUtils.serializeTestCase(this);
	}

	/**
	 * Custom serialization is needed.
	 */
	private void writeObject(ObjectOutputStream aStream) throws IOException {
		aStream.defaultWriteObject();

	}

	/**
	 * This method is called after stop, to dump case data.
	 */
	private class GracefulStopTask implements Runnable {

		private AbstractTestCase atc;

		public GracefulStopTask(AbstractTestCase atc) {
			super();
			this.atc = atc;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			atc.stop(true);

		}
	}

	// Those are only for dump purposes
	private String localAddress = "127.0.0.1", remoteAddress = "127.0.0.1";
	private int localPort = 2428, remotePort = 2427;
	private int cps = 1;
	private long callDuration = 2500;
	private long maxCalls = AbstractTestCase._TURN_OFF_BOUNDRY;
	private int maxConcurrentCalls = AbstractTestCase._TURN_OFF_BOUNDRY;
	private int maxFailCalls = AbstractTestCase._TURN_OFF_BOUNDRY;

	private transient String[] textDump;

	public void createGraphicDumps(long sequence) {

		AbstractCall call = null;
		if (sequence != -1) {
			call = this.callSequenceToCall.get(sequence);
		} else {
			int index0 = this.callSequenceToCall.size() / 2;
			Iterator<Long> seqIterator = this.callSequenceToCall.keySet()
					.iterator();
			while (index0 > 0) {

				// seqIterator.next();
				index0--;
			}

			while (call == null && seqIterator.hasNext()) {
				Long seq = seqIterator.next();

				call = this.callSequenceToCall.get(seq);
				
				if (call.getState() == CallState.IN_ERROR
						|| call.getState() == CallState.TIMED_OUT) {
					call = null;
					continue;
				} else {
					break;
				}
			}
		}

		if (call == null) {
			throw new RuntimeException("No call found. "+this.callSequenceToCall);
		}

		try {
			call.setTestCase(this);
			call.resetReader();
			String[] connectionIds = call.getCallLegs();
			textDump = new String[connectionIds.length];
			for(int i = 0;i<connectionIds.length;i++) {
				
				
				String connectionId = connectionIds[i];
				
				
				
				Deviation devCalculator = new Deviation();
				call.resetReader();
				List<RtpPacket> workingTraffic = call
						.getCallLegRtpData(connectionId);
				List<RtpPacket> loadedTraffic = workingTraffic;
				do {
					// we must do this like that cause collection can store up
					// to INteger.MAX_INT entries, and calls can easly have more
					// packets....
					
					//now see http://www.ietf.org/rfc/rfc1889.txt section 6.3.1
					
					int currentSize = workingTraffic.size() - 1;
					for (int index = 0; index < currentSize; index++) {
						RtpPacket p1 = workingTraffic.remove(0);
						RtpPacket p2 = workingTraffic.get(0);
						double D = p2.getTime().getTime()-p1.getTime().getTime()-p2.getTimestamp()+p1.getTimestamp();
						devCalculator.update(D);
					}
					
					//load possibly rest of data.
					loadedTraffic = call
					.getCallLegRtpData(connectionId);
					workingTraffic.addAll(loadedTraffic);
				} while (loadedTraffic.size() > 0);
				
				textDump[i] = connectionId+" interarrival jitter - mean: "+devCalculator.getMean()+" variance: "+devCalculator.getVariance()+" deviation: "+(Math.sqrt(devCalculator.getVariance()));
				
				//now reset streams reader
				call.resetReader();
			}
			
			//lets kill streams
			call.stopReader();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create log data.", e);
		}

	}

	public String[] getTextDump() {
		return this.textDump;
	}

}
