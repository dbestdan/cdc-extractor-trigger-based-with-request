
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
		while (true) {
			try {
				synchronized (CoordinatorRunnable.uptodate) {
					uptodatetime = CoordinatorRunnable.uptodate.getTime();
				}

				// if request is upto date with delta request time or request
				// gets expired
				if ((uptodatetime > requestStartTime) || (System.currentTimeMillis() > requestExpirationTime)) {

					// if request gets fulfilled
					if (uptodatetime > requestStartTime) {
						latency = System.currentTimeMillis() - requestStartTime;						
					}else {
						latency = Long.parseLong(System.getProperty("timeWindow"));
						expired = true;
						staleness = requestStartTime - uptodatetime ;						
					}					
					
					Status status = new Status(staleness, latency, expired);
					
					StatusWriterRunnable.requestStatusQueue.put(status);
					System.out.println("Checker requestStartTime: "+ requestStartTime);
					System.out.println("Checker requestEndTime: "+ requestExpirationTime);
					System.out.println("Checker latency: "+ latency);
					System.out.println("Checker staleness: "+ staleness);
					System.out.println("Checker expiration: "+ expired);
					System.out.println("Checker uptodatetime: "+ uptodatetime);
					System.out.println("--------------------------------");
					break;
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}
