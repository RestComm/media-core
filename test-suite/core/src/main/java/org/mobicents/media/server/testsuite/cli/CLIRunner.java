package org.mobicents.media.server.testsuite.cli;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import jain.protocol.ip.mgcp.CreateProviderException;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sdp.Attribute;
import javax.sdp.SdpFactory;

import org.apache.log4j.PropertyConfigurator;
import org.mobicents.media.server.testsuite.general.AbstractTestCase;
import org.mobicents.media.server.testsuite.general.CallDisplayInterface;
import org.mobicents.media.server.testsuite.general.TestState;
import org.mobicents.media.server.testsuite.general.ann.AnnouncementTest;
import org.mobicents.media.server.testsuite.general.file.FileUtils;

/**
 * Start time:12:13:39 2009-07-16<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * This is class which enables running test tool in cli mode.
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class CLIRunner implements CallDisplayInterface {

	private transient static final String _COLLECTIVE_FILE_NAME = "graph.txt";

	private String localAddress = "127.0.0.1", remoteAddress = "127.0.0.1";
	private int localPort = 2428, remotePort = 2427;
	private int cps = 1;
	private long callDuration = 2500;
	private long maxCalls = AbstractTestCase._TURN_OFF_BOUNDRY;
	private int maxConcurrentCalls = AbstractTestCase._TURN_OFF_BOUNDRY;
	private int maxFailCalls = AbstractTestCase._TURN_OFF_BOUNDRY;
	private File dataDumpDir = new File("datadump");
	private String audioFileURL = new File("target/audio/ulaw_13s.wav").toURI().toString();
	private Vector<Attribute> codec = new Vector<Attribute>();
	private AbstractTestCase testCase;
	private TestTypeEnum testType = TestTypeEnum.AnnTest;
	
	private boolean performOfflineDumps;
	private long callSequenceToDump;
	
	private boolean performTestRun;

	
	private static final LongOpt[] _LONG_OPTS = new LongOpt[15];
	private static final String _GETOPT_PARAMS_STRING = "h:q:w:e:r:t:y:u:i:o:p:a:f:g";
	private static final Logger log = Logger.getLogger(CLIRunner.class.getName());

	private static void configLog4j() {
		InputStream inStreamLog4j = CLIRunner.class.getClassLoader().getResourceAsStream("log4j.properties");
		Properties propertiesLog4j = new Properties();
		try {
			propertiesLog4j.load(inStreamLog4j);
			PropertyConfigurator.configure(propertiesLog4j);
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.finest("log4j configured");

	}

	static {

		_LONG_OPTS[0] = new LongOpt("usage", LongOpt.NO_ARGUMENT, null, 'h');
		_LONG_OPTS[1] = new LongOpt("localaddr", LongOpt.OPTIONAL_ARGUMENT, null, 'q');
		_LONG_OPTS[2] = new LongOpt("remoteaddr", LongOpt.OPTIONAL_ARGUMENT, null, 'w');
		_LONG_OPTS[3] = new LongOpt("remoteport", LongOpt.OPTIONAL_ARGUMENT, null, 'e');
		_LONG_OPTS[4] = new LongOpt("localport", LongOpt.OPTIONAL_ARGUMENT, null, 'r');
		_LONG_OPTS[5] = new LongOpt("concurrentcalls", LongOpt.OPTIONAL_ARGUMENT, null, 't');
		_LONG_OPTS[6] = new LongOpt("maxcalls", LongOpt.OPTIONAL_ARGUMENT, null, 'y');
		_LONG_OPTS[7] = new LongOpt("datadir", LongOpt.OPTIONAL_ARGUMENT, null, 'u');
		_LONG_OPTS[8] = new LongOpt("audiofile", LongOpt.OPTIONAL_ARGUMENT, null, 'i');
		_LONG_OPTS[9] = new LongOpt("audiocodec", LongOpt.OPTIONAL_ARGUMENT, null, 'o');
		_LONG_OPTS[10] = new LongOpt("testtype", LongOpt.OPTIONAL_ARGUMENT, null, 'p');
		_LONG_OPTS[11] = new LongOpt("cps", LongOpt.OPTIONAL_ARGUMENT, null, 'a');
		_LONG_OPTS[12] = new LongOpt("maxfail", LongOpt.OPTIONAL_ARGUMENT, null, 'f');
		_LONG_OPTS[13] = new LongOpt("callduration", LongOpt.OPTIONAL_ARGUMENT, null, 'l');
		_LONG_OPTS[14] = new LongOpt("graph", LongOpt.OPTIONAL_ARGUMENT, null, 'g');

		configLog4j();
	}

	/**
     * 	
     */
	public CLIRunner() {
		// TODO Auto-generated constructor stub
		convertCodec("0 pcmu/8000");
	}

	// ////////////////
	// Usage method //
	// ////////////////
	public static void usage() {
		StringBuffer sb = new StringBuffer();

		sb.append("java " + CLIRunner.class.getName() + " [OPTIONS] --testtype TestType \n");
		sb.append("Where options can be:\n");
		sb.append("--localaddr       : local address, default is 127.0.0.1\n");
		sb.append("--remoteaddr      : remote address, default is 127.0.0.1\n");
		sb.append("--localpport      : local port, default is 2428\n");
		sb.append("--remoteport      : remote port, default is 2427\n");
		sb.append("--concurrentcalls : concurrent calls, default is -1, which means unbound\n");
		sb.append("--maxcalls        : max calls, default is -1, which means unbound\n");
		sb.append("--datadir         : data dump directory, default is ./datadump. IN case of offline runs, it must point to valid test, like: /datadump/1231463412\n");
		sb.append("--audiofile       : audio file url, if requried, default is file:/./../../target/audio/ulaw_13s.wav\n");
		sb.append("--audiocodec      : audio codec to be used if requried, default is \'0 pcmu/8000\', value should be specifiedd in \'\'\n");
		sb.append("--testtype        : test type, currently there is only one available: AnnTest\n");
		sb.append("--maxfail         : specifies how many calls may fail until testtool will stop sending requests to server, default is -1, which means unbound\n");
		sb.append("--callduration    : specifies how long test runs(in milliseconds), default is 2500 \n");
		sb.append("--cps             : specifies calls per second, default is 1 \n");
		sb.append("--usage           : print this message\n");
		sb.append("--graph           : It makes test case produce graphic files and present some txt output. Graphic files depend on testcase, some do not support graphic. If there is no value, test case picks one call arbitrary, otherwise it performs this operation for specific call. Argumetn must match call sequence.\n");
		sb.append("example options part: --localaddress=127.0.0.1 --localport=2499 --concurentcalls=12 --audiocodec=\'8 pcma/8000\' --testtype=AnnTest\n");
		log.severe("Usage: \n" + sb);

	}

	// ///////////////////////
	// Some helper methods //
	// ///////////////////////
	private void convertCodec(String s) {
		SdpFactory sdpFactory = SdpFactory.getInstance();
		codec.clear();

		codec.add(sdpFactory.createAttribute("rtpmap", s.replaceAll("'", "")));
	}

	private void convertTest(String v) {
		testType = TestTypeEnum.fromString(v);

	}

	// ////////////////////////
	// Methods used by test //
	// ////////////////////////
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.media.server.testsuite.general.CallDisplayInterface#getCPS
	 * ()
	 */
	public int getCPS() {
		return this.cps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * getCallDuration()
	 */
	public long getCallDuration() {
		return this.callDuration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.media.server.testsuite.general.CallDisplayInterface#getCodec
	 * ()
	 */
	public Vector<Attribute> getCodecs() {
		return this.codec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * getDefaultDataDumpDirectory()
	 */
	public File getDefaultDataDumpDirectory() {
		return this.dataDumpDir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.media.server.testsuite.general.CallDisplayInterface#getFileURL
	 * ()
	 */
	public String getFileURL() {
		return this.audioFileURL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * getLocalAddress()
	 */
	public String getLocalAddress() {
		return this.localAddress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * getLocalPort()
	 */
	public int getLocalPort() {
		return this.localPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * getRemoteAddress()
	 */
	public String getRemoteAddress() {
		return this.remoteAddress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * getRemotePort()
	 */
	public int getRemotePort() {
		return this.remotePort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.CallDisplayInterface#
	 * updateCallView()
	 */
	public void updateCallView() {
		if (log.isLoggable(Level.INFO)) {
			StringBuffer sb = new StringBuffer();
			sb.append("=============================================================\n");
			sb.append("Press 'q' + Enter to stop test\n");
			sb.append("Current calls     : " + this.testCase.getOngoingCallNumber() + "\n");
			sb.append("Success calls     : " + this.testCase.getCompletedCallNumber() + "\n");
			sb.append("Failed calls      : " + this.testCase.getErrorCallNumber() + "\n");
			sb.append("Max failed calls  : " + this.maxFailCalls + "\n");
			sb.append("Total calls       : " + this.testCase.getTotalCallNumber() + "\n");

			log.info(sb.toString());
		}
	}

	public int getMaxConcurrentCalls() {
		return this.maxConcurrentCalls;
	}

	public long getMaxCalls() {
		return this.maxCalls;
	}

	public int getMaxFailCalls() {
		return this.maxFailCalls;
	}

	public static void main(String[] args) {

		CLIRunner cli = new CLIRunner();
		cli.parseArgs(args);
		;
		if (cli.performTestRun)
			cli.runTest();
		// This is required, since mgc stack leaves some threads alive....
		if (cli.performOfflineDumps) {
			if(cli.loadTest())
			{
				cli.performDumps();
			}else
			{
				log.severe("Failed to load test from: "+cli.dataDumpDir);
			}
			
		}
		System.exit(0);

	}

	

	

	private boolean loadTest() {
		//we must load test if its not there yet
		if(testCase !=null)
		{
			return true;
		}
		try{
			AbstractTestCase test = FileUtils.deserializeTestCase(this.dataDumpDir);
			
			this.testCase = test;
			this.testCase.setCallDisplay(this,this.dataDumpDir);
			this.dataDumpDir = this.dataDumpDir.getParentFile();
			return true;
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}

	private void performDumps() {
		this.testCase.createGraphicDumps(callSequenceToDump);
		String[] txtDumps = this.testCase.getTextDump();
		log.info(" ------------- TEST Output -------------");
		for(String s: txtDumps)
		{
			log.info(">>"+s);
		}
		log.info(" ---------------------------------------");
	}

	private void runTest() {

		try {
			this.testCase = testType.getTestCaseForType(this);
			log.info("Starting test case, prest 'q' to exit test");
		} catch (UnknownHostException ex) {
			log.log(Level.SEVERE, null, ex);
			return;
		} catch (Exception e) {
			log.log(Level.SEVERE, null, e);
			return;
		}

		try {

			this.testCase.start();
			while (this.testCase.getTestState() != TestState.Stoped) {
				try {
					Thread.sleep(1000);

					if (System.in.available() > 0) {

						int r = System.in.read();
						if (r == 'q') {

							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();

				}
			}

		} catch (CreateProviderException ex) {
			log.log(Level.SEVERE, null, ex);
		} catch (TooManyListenersException ex) {
			log.log(Level.SEVERE, null, ex);
		} finally {
			if (testCase != null) {
				try {

					testCase.stop(false);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					Thread.currentThread().sleep(this.getCallDuration() + 5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	private void parseArgs(String[] args) {

		Getopt getOpt = new Getopt("CLIRunner", args, _GETOPT_PARAMS_STRING, _LONG_OPTS);
		getOpt.setOpterr(true);
		int c = -1;
		String v = null;

		while ((c = getOpt.getopt()) != -1) {

			// _GETOPT_PARAMS_STRING ="h:q:w:e:r:t:y:u:i:o:p";
			switch (c) {
			case 'h':
				usage();
				System.exit(0);
			case 'q':
				// local address
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Local Address must have value");
				} else {
					try {
						InetAddress.getByName(v);
						this.localAddress = v;
					} catch (UnknownHostException ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'w':
				// remote address
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Remote Address must have value");
				} else {
					try {
						InetAddress.getByName(v);
						this.remoteAddress = v;
					} catch (UnknownHostException ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'e':
				// remote port
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Remote Port must have value");
				} else {
					try {

						this.remotePort = Integer.valueOf(v);
					} catch (NumberFormatException ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'r':
				// local port
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Local Port must have value");
				} else {
					try {

						this.localPort = Integer.valueOf(v);
					} catch (NumberFormatException ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 't':
				// concurrent calls
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Concurrent Calls must have value");
				} else {
					try {

						this.maxConcurrentCalls = Integer.valueOf(v);
					} catch (NumberFormatException ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'y':
				// max calls
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Max Calls must have value");
				} else {
					try {

						this.maxCalls = Integer.valueOf(v);
					} catch (NumberFormatException ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'u':
				// data dir
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Data Dir must have value");
				} else {
					this.dataDumpDir = new File(v);
				}
				//this.performTestRun = true;
				break;
			case 'i':
				// audio file
				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Audio File URL must have value");
				} else {
					try {
						new URL(v);
						this.audioFileURL = v;
					} catch (Exception ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'o':

				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Audio Codec must have value");
				} else {
					try {
						this.convertCodec(v);
					} catch (Exception ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'p':

				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Test Type must have value");
				} else {
					try {
						this.convertTest(v);
					} catch (Exception ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'a':

				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Test Type must have value");
				} else {
					try {
						this.cps = Integer.valueOf(v);
					} catch (Exception ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;

			case 'f':

				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Max Failure Calls must have value");
				} else {
					try {
						this.maxFailCalls = Integer.valueOf(v);
					} catch (Exception ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'l':

				v = getOpt.getOptarg();
				if (v == null) {
					log.severe("Call Duration must have value");
				} else {
					try {
						this.callDuration = Long.valueOf(v);
					} catch (Exception ex) {
						log.log(Level.SEVERE, null, ex);
					}
				}
				this.performTestRun = true;
				break;
			case 'g':
			
				this.performOfflineDumps = true;
				v = getOpt.getOptarg();
				if(v == null)
				{
					this.callSequenceToDump = -1;
				}else
				{
					this.callSequenceToDump = Long.valueOf(v);
				}
				break;

			default:
				log.severe("Wrong parameter!! ---> " + Character.toString((char) c));

			}
		}

	}
}

enum TestTypeEnum {

	AnnTest;

	public static TestTypeEnum fromString(String v) {
		if (v.equals(AnnTest.toString())) {
			return AnnTest;
		}

		throw new RuntimeException("There is no such test type, valid are: " + AnnTest + ", value passed:" + v);
	}

	public AbstractTestCase getTestCaseForType(CallDisplayInterface cdi) throws IllegalStateException, SocketException, IOException {
		if (this.toString().equals(AnnTest.toString())) {

			AnnouncementTest at = new AnnouncementTest();
			at.setCallDisplay(cdi);
			return at;
		}

		throw new RuntimeException("There is no such test type: " + this);
	}
}
