import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.media.server.impl.clock.Scheduler;
import org.mobicents.media.server.spi.clock.Task;
import org.mobicents.media.server.spi.clock.TimerListener;

/**
 * 
 * @author kulikov
 */
public class SchedulerTest {

	private Lock objs = null;

	public SchedulerTest(Lock objs) {
		this.objs = objs;
	}

	private int[] jitter = new int[201];
	private Scheduler scheduler = new Scheduler();

	List<Long> latencies = new ArrayList<Long>();
	List<Long> actualDelays = new ArrayList<Long>();

	public void stop() {
		scheduler.stop();
	}

	public void dump() throws FileNotFoundException, IOException {
		BufferedWriter writter = new BufferedWriter(new FileWriter(
				"/home/abhayani/workarea/mobicents/svn/trunk/servers/media/test-suite/aop-test/target/jitter.txt"));
		for (int i = 0; i < jitter.length; i++) {
			String s = (i - 100) + " " + jitter[i];
			writter.write(s);
			writter.newLine();
		}

		writter.newLine();
		writter.newLine();
		writter.newLine();

		for (long l : actualDelays) {
			writter.write(l + " ");
		}
		
		writter.newLine();
		writter.newLine();
		writter.newLine();

		for (long l : latencies) {
			writter.write(l + " ");
		}		

		writter.flush();
		writter.close();
	}

	public void test() throws Exception {

		scheduler.addTimerListener(new TimerListenerImpl());
		scheduler.start();

		for (int i = 0; i < 100; i++) {
			System.out.println("Starting " + i);
			if (i < 50) {
				scheduler.execute(new TestTask(20));
			} else {
				scheduler.execute(new TestTask(30));
			}
		}
	}

	private class TestTask implements Task {
		private long last;

		private int period;

		public TestTask(int period) {
			this.period = period;
		}

		public int perform() {
			long now = System.currentTimeMillis();
			if (last != 0) {
				long j = (now - last) + 100 - period;
				if (j >= 0 & j < 200) {
					jitter[(int) j]++;
					// System.out.println(jitter[(int)j]);
				}
			}
			last = now;
			return period;
		}

		public boolean isActive() {
			return true;
		}

		public void cancel() {
		}
	}

	public static void main(String[] args) throws Exception {
		SchedulerTest test = new SchedulerTest(new Lock());
		test.test();
	}

	private class TimerListenerImpl implements TimerListener {
		long now = System.currentTimeMillis();

		public void cycleComplete(long credit) {
			latencies.add(credit);
			
			synchronized (objs) {
				objs.setExecutionTime(System.currentTimeMillis() + (credit - 2));
				//System.out.println("Set the credit to "+ credit);
				objs.notify();
			}

		}

		public void cycleStart() {
			actualDelays.add(System.currentTimeMillis() - now);
			now = System.currentTimeMillis();
			
			
		}

	}
}
