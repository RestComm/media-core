package org.mobicents.javax.media.mscontrol.load;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.MediaEvent;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaEventNotifier;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.spi.DriverManager;
import org.junit.Test;
import org.mobicents.javax.media.mscontrol.spi.DriverImpl;
import junit.framework.Assert;
import junit.framework.TestCase;

public class DTMFLoadTest {
	// Property key for the Unique MGCP stack name for this application
	public static final String MGCP_STACK_NAME = "mgcp.stack.name";

	// Property key for the IP address where CA MGCP Stack (SIP Servlet
	// Container) is bound
	public static final String MGCP_STACK_IP = "mgcp.server.address";

	// Property key for the port where CA MGCP Stack is bound
	public static final String MGCP_STACK_PORT = "mgcp.local.port";

	// Property key for the IP address where MGW MGCP Stack (MMS) is bound
	public static final String MGCP_PEER_IP = "mgcp.bind.address";

	// Property key for the port where MGW MGCP Stack is bound
	public static final String MGCP_PEER_PORT = "mgcp.server.port";

	public static class Task {
		MediaSession msSession;
		Boolean initialized=false;
		NetworkConnection nc1=null,nc2=null;
		MediaGroup mg1=null,mg2=null;
		SignalDetectorEvent sdEvent=null;
		
		public Task(MediaSession msSession) {
			this.msSession = msSession;
		}
	
		public boolean gotEvent()
		{
			return sdEvent!=null;
		}

		public boolean initSession()  throws Exception {
			try {
				nc1 = msSession.createNetworkConnection(NetworkConnection.BASIC);
				nc2 = msSession.createNetworkConnection(NetworkConnection.BASIC);
				SdpPortManager sdp1 = nc1.getSdpPortManager();
				SdpPortManager sdp2 = nc2.getSdpPortManager();
				EventWaiter sdpWaiter1 = new EventWaiter(sdp1);
				EventWaiter sdpWaiter2 = new EventWaiter(sdp2);
				sdp1.generateSdpOffer();
				SdpPortManagerEvent offerEvent = (SdpPortManagerEvent) sdpWaiter1.waitForOKEvent(2000);

				byte[] sdpb1 = offerEvent.getMediaServerSdp();

				sdp2.processSdpOffer(sdpb1);
				SdpPortManagerEvent answerEvent = (SdpPortManagerEvent) sdpWaiter2.waitForOKEvent(2000);
				byte[] sdpb2 = answerEvent.getMediaServerSdp();

				sdp1.processSdpAnswer(sdpb2);
				mg1 = msSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
				mg2 = msSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
				mg1.join(javax.media.mscontrol.join.Joinable.Direction.DUPLEX, nc1);
				mg2.join(javax.media.mscontrol.join.Joinable.Direction.DUPLEX, nc2);
				initialized=true;
				return true;
			}
			catch(Exception e)
			{
				if(mg1 != null) mg1.release();
				if(mg2 != null) mg2.release();
				if(nc1 != null) nc1.release();
				if(nc2 != null) nc2.release();								
			}					
			
			return false;
		}
		
		public SignalDetectorEvent run() throws Exception {
			//error already counter in initialization
			if(!initialized)
				return null;
						
			EventWaiter signalWaiter = new EventWaiter(mg1.getSignalDetector());
			mg1.getSignalDetector().receiveSignals(1, null, null, null);
			Thread.sleep(2000);
			mg2.getPlayer().play(URI.create("file:////opt/mobsource/media/jsr-309-tck/media/dtmf-1.wav"), null, null);
			sdEvent = (SignalDetectorEvent) signalWaiter.waitForOKEvent(2000);
			return sdEvent;
		}
		
		public void release()
		{
			if(mg1 != null) mg1.release();
			if(mg2 != null) mg2.release();
			if(nc1 != null) nc1.release();
			if(nc2 != null) nc2.release();
		}
	}


	int numErrors = 0;
	@Test
	public void test10ConcurrentCalls() throws Exception {
		Properties properties = new Properties();
		properties.setProperty(MGCP_STACK_NAME, "sts");
		properties.setProperty(MGCP_PEER_IP, "127.0.0.1");
		properties.setProperty(MGCP_PEER_PORT, "2427");
		properties.setProperty(MGCP_STACK_IP, "127.0.0.1");
		properties.setProperty(MGCP_STACK_PORT, "2727");

		// create the Media Session Factory
		final MsControlFactory msControlFactory = new DriverImpl().getFactory(properties);
		//MediaSession msSession = msControlFactory.createMediaSession();
		int[] results = new int[100];
		int w = 50; // make 10 calls
		ExecutorService exec = Executors.newFixedThreadPool(w);
		CompletionService<Object> cservice = new ExecutorCompletionService<Object>(exec);
				
		numErrors = 0; 
		
		final Task[] currArray=new Task[w];
		Initiator[] initiators=new Initiator[w];
		Runner[] runners=new Runner[w];
		Closer[] closers=new Closer[w];
		//lets init arrays
		for(int q=0;q<w;q++) {
			currArray[q]=new Task(msControlFactory.createMediaSession());			
			initiators[q]=new Initiator(currArray[q]);							
			runners[q]=new Runner(currArray[q]);
			closers[q]=new Closer(currArray[q]);
		}
		
		//lets start initiators
		for(int q=0;q<w;q++) {					
			cservice.submit(initiators[q]);					
		}
		
		//lets wait that all initiators completed
		for(int q=0;q<w;q++) {
			try {
				cservice.take().get();
			}
	        catch (InterruptedException e) {}
	        catch (ExecutionException e) {}
		}
		
		//now lets submit all runners
		for(int q=0;q<w;q++) {
			cservice.submit(runners[q]);			
		}
		
		System.out.println("Now at pass " + w);
		
		//and wait for their responses
		for(int q=0;q<w;q++) {
			try {
				cservice.take().get();
			}
	        catch (InterruptedException e) {}
	        catch (ExecutionException e) {}
		}
		
		//lets start closers
		for(int q=0;q<w;q++) {					
			cservice.submit(closers[q]);					
		}
		
		//lets wait that all closers completed
		for(int q=0;q<w;q++) {
			try {
				cservice.take().get();
			}
	        catch (InterruptedException e) {}
	        catch (ExecutionException e) {}
		}
		
		for(int q=0;q<w;q++) {
			if(!currArray[q].gotEvent())
				numErrors++;
		}
		
		exec.shutdown();
		exec.shutdownNow();
		System.out.println("Errors occured = " + numErrors  + " at " + w + " concurrent calls");
	}

	public static class Initiator implements Callable
	{		
		Task toInit;
		public Initiator(Task toInit)
		{
			this.toInit=toInit;
		}
		
		public Object call()
		{
			try
			{
				toInit.initSession();					
			}
			catch(Exception e)
			{				
			}		
			
			return null;
		}
	}
	
	public static class Runner implements Callable
	{
		Task toRun;
		public Runner(Task toRun)
		{
			this.toRun=toRun;
		}
		
		public Object call()
		{
			try
			{
				toRun.run();					
			}
			catch(Exception e)
			{				
			}		
			
			return null;
		}
	}
	
	public static class Closer implements Callable
	{
		Task toClose;
		public Closer(Task toClose)
		{
			this.toClose=toClose;
		}
		
		public Object call()
		{
			toClose.release();
			return null;
		}
	}
	
	public static class EventWaiter
	{
		private MediaEvent waitInternal(long timeout, boolean requireSuccess) throws Exception
		{
			MediaEventNotifier mediaeventnotifier = notifier;
			MediaEvent result;

			synchronized(notifier) {
				if(timeout > 0L)
				{
					if(received == null)
						notifier.wait(1000L);
					
					if(received == null && timeout > 1000L)
					{

						long currentTime = System.currentTimeMillis();
						for(long endTime = (currentTime + timeout) - 1000L; received == null && currentTime < endTime; currentTime = System.currentTimeMillis())
						{
							//clientLogger.debug((new StringBuilder()).append("Waiting for event from ").append(notifier).append(" (").append((endTime - currentTime) / 1000L).append("s remaining)").toString());
							notifier.wait(1000L);
						}
					}
					
					if(requireSuccess)
					{
						Assert.assertNotNull((new StringBuilder()).append("Timed out waiting for event from ").append(notifier).append(", after ").append(timeout / 1000L).append("seconds").toString(), received);
						Assert.assertEquals((new StringBuilder()).append(notifier).append(" event error: \"").append(received.getError()).append("\" - ").append(received.getErrorText() == null ? "" : (new StringBuilder()).append(received.getErrorText()).append(" - ").toString()).toString(), MediaErr.NO_ERROR, received.getError());
					}
				}
			}
			
			result = received;
			received = null;
			return result;
		}

		public void discardEvent(long timeout) throws Exception
		{
			waitInternal(timeout, true);
		}

		public void failIfEvent(long timeout) throws Exception
		{
			MediaEvent event = waitInternal(timeout, false);
			Assert.assertNull((new StringBuilder()).append("Unexpected event from ").append(notifier).append(": ").append(event).toString(), event);
		}

		public MediaEvent waitForAnyEvent(long timeout) throws Exception
		{
			MediaEvent event = waitInternal(timeout, false);
			Assert.assertNotNull((new StringBuilder()).append("Timed out waiting for event from ").append(notifier).append(", after ").append(timeout / 1000L).append("seconds").toString(), event);
			return event;
		}

		public MediaEvent waitForOKEvent(long timeout) throws Exception
		{
			return waitInternal(timeout, true);
		}

		public MediaEvent waitForEventID(EventType eventID) throws Exception
		{
			MediaEvent result = waitForOKEvent(5000L);
			Assert.assertEquals("Bad eventId: ", eventID, result.getEventType());
			return result;
		}

		public void detach()
		{
			notifier.removeListener(listener);
		}

		private MediaEvent received;
		MediaEventNotifier notifier;
		final MediaEventListener listener;

		public EventWaiter(final MediaEventNotifier notifier)
		{
			this.notifier = notifier;
			//clientLogger = aLog;
			final EventWaiter waiter = this;
			listener = new MediaEventListener() {
				public void onEvent(MediaEvent anEvent)
				{
					//clientLogger.debug((new StringBuilder()).append(notifier).append(" received: ").append(anEvent).toString());
					synchronized(notifier)
					{
						received = anEvent;
						notifier.notify();
					}
				}
			};

			notifier.addListener(listener);
		}
	}
}
