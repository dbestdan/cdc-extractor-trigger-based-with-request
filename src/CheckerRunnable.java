
public class CheckerRunnable implements Runnable {
	private long requestStartTime = 0l;
	private long requestExpirationTime = 0L;
	private long staleness = 0L;
	private long latency = 0L;
	private long uptodatetime = 0L;
	private boolean expired = false;
	
	
	
	public CheckerRunnable(long requestStartTime, long requestExpirationTime) {
		this.requestStartTime = requestStartTime;
		this.requestExpirationTime = requestExpirationTime;
	}



	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) {
		synchronized(Coordinator.uptodate) {
			uptodatetime = Co
		}
		}
		
	}

}
