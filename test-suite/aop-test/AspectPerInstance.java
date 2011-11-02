import org.jboss.aop.advice.annotation.JoinPoint;
import org.jboss.aop.joinpoint.JoinPointBean;
import org.jboss.aop.joinpoint.MethodInvocation;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AspectPerInstance {

	long now = System.currentTimeMillis();
	long executionTime;

	public void makeItStop(Object lock) {
		synchronized (lock) {

			try {
				// System.out.println("Locking on " + lock);
				//now = System.currentTimeMillis();
				lock.wait();
				//System.out.println(this+ " "+ lock + " Was locked for " + (System.currentTimeMillis() - now));
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void beforeAdvice(@JoinPoint
	JoinPointBean joinPoint) {
		now = System.currentTimeMillis();
		executionTime = Driver.obj.getExecutionTime();
		if (executionTime <= (System.currentTimeMillis())) {
			//System.out.println("Execution Time = " + executionTime+ joinPoint.getAdvisor());
			makeItStop(Driver.obj);
		}
	}

	public Object methodAdvice(MethodInvocation invocation) throws Throwable {
		// System.out.println(this + " AspectPerInstance.methodAdvice accessing: " + invocation.getMethod().toString());
		// now = System.currentTimeMillis();
		// Lock lock = (Lock) (invocation.getArguments())[0];
		executionTime = Driver.obj.getExecutionTime();
		if (executionTime <= (System.currentTimeMillis())) {
			// System.out.println("Execution Time = " + executionTime);
			makeItStop(Driver.obj);
		}
		Object obj = invocation.invokeNext();
		// Driver.obj.setExecutionTime(Driver.obj.getExecutionTime() - (System.currentTimeMillis() - now));
		return obj;
	}

}
