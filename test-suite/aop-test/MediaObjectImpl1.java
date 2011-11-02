import java.util.Random;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MediaObjectImpl1 implements MediaObject {

	int objCounts = 100;
	SomeOtherObject objs[] = new SomeOtherObject[objCounts];

	public MediaObjectImpl1() {
		for (int k = 0; k < objCounts; k++) {
			objs[k] = new SomeOtherObject();
		}
	}

	public long fib(int n) {
		if (n <= 1)
			return n;
		else
			return fib(n - 1) + fib(n - 2);
	}

	public void start(Object lock, int itera) {
		// Simulate load
		int someInt = (new Random()).nextInt(19);
		fib(someInt);
		//System.out.println("The fib for " + someInt + " = " + this.fib(someInt));
		for (int k = 0; k < itera; k++) {
			objs[k].start(lock, 0);
		}

	}
}
