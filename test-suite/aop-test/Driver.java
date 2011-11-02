import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadFactory;

/**
 * @author amit bhayani
 */
public class Driver {

	public static volatile Lock obj = null;

	private volatile boolean start = true;

	SchedulerTest schedulerTest = null;
	MediaObjectImpl1 pojo = null;

	Thread t = null;

	public static void main(String[] args) throws Exception {
		System.out.println("--- start test ---");
		Driver d = new Driver();

		d.init();

		d.initScheduler();

		d.test();
	}

	private void init() throws InterruptedException {
		obj = new Lock();
		pojo = new MediaObjectImpl1();

		t = new Thread(new SomeTask());
	}

	private void initScheduler() {
		schedulerTest = new SchedulerTest(obj);
		try {
			schedulerTest.test();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void test() throws InterruptedException, FileNotFoundException, IOException {

		t.start();

		Thread.sleep(1000 * 60);

		this.start = false;

		schedulerTest.stop();

		schedulerTest.dump();

	}

	class MinThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	}

	class SomeTask implements Runnable {
		
		long now = System.currentTimeMillis();

		public void run() {
			// System.out.println("Started some task");
			while (start) {
				// System.out.println("inside loop");
				int i = (new Random()).nextInt(50);
				// System.out.println("The Fib for " + i);
				pojo.start(obj, 100);
				//System.out.println("Took " + i + " = " + (System.currentTimeMillis() - now));
				//now = System.currentTimeMillis();
				// System.out.println("------");

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			System.out.println("Stopped some task");
		}

	}

}
