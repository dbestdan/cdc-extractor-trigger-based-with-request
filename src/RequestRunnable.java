
public class RequestRunnable implements Runnable{
	private long requestInterval = 0l;
	private long timeWindow = 0L;
	
	public RequestRunnable() {
		requestInterval = Long.parseLong(System.getProperty("requestInterval"));
		timeWindow = Long.parseLong(System.getProperty("timeWindow"));
	}
	
	
	@Override
	public void run() {
		// TODO This will continuously fire request with current time stamp and window time
		while(true) {
			try {
				long requestInterval = Long.parseLong(System.getProperty("requestInterval"));
				Thread.sleep(requestInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			
			
			
		}
			
	
		
	}
	
	

}
