import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;

public class StausWriterRunnable implements Runnable, Config {
	private long expirationTime = 0L;
	private int threadSize = 0;
	private long totalStaleness = 0L;
	private long avgStaleness = 0L;
	private long expirationRatio = 0L;
	private long requestTime = 0L;
	private long expirationCount = 0L;
	private Writer stalenessWriter = null;
	private Writer expirationWriter = null;
	private long totalRequest = 0L;
	private int completedThread =0;

	public StausWriterRunnable() {
		this.threadSize = Integer.parseInt(System.getProperty("numberOfThread"));

		String stalenessFileName = "staleness" + "_coordinator_sleep_time_" + System.getProperty("sleepDuration")
				+ "_Thread_" + System.getProperty("numberOfThread") + "_" + dateFormat.format(new Date());

		String expirationFileName = "expiration" + "_coordinator_sleep_time_" + System.getProperty("sleepDuration")
				+ "_Thread_" + System.getProperty("numberOfThread") + "_" + dateFormat.format(new Date());
		try {
			stalenessWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(stalenessFileName, true), "UTF-8"));
			expirationWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(expirationFileName, true), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while (true) {
			
			
			

			boolean isInProgress = false;
			synchronized (CoordinatorRunnable.requestInProgress) {
				if (CoordinatorRunnable.requestInProgress == 1) {
					isInProgress = true;
				}
			}
			if (isInProgress) {
				
				synchronized (CoordinatorRunnable.requestExpirationTime) {
					expirationTime = CoordinatorRunnable.requestExpirationTime;
				}
				synchronized (CoordinatorRunnable.requestStartTime) {
					requestTime = CoordinatorRunnable.requestStartTime;
				}
				// get total request from coordinator
				synchronized (CoordinatorRunnable.totalRequest) {
					totalRequest = CoordinatorRunnable.totalRequest;
				}
				
				synchronized(CoordinatorRunnable.completedThread) {
					completedThread = CoordinatorRunnable.completedThread;
				}

				//System.out.println("test");
				if ((System.currentTimeMillis() > expirationTime)
						|| (threadSize == completedThread)) {

					long staleness = 0L;
					// check if request is expired or not
					if (System.currentTimeMillis() > expirationTime) {
						synchronized (CoordinatorRunnable.uptodate) {
							System.out.println("Checker Request Time "+ requestTime );
							System.out.println("Checker uptodate: " + CoordinatorRunnable.uptodate.getTime());
							staleness = requestTime - CoordinatorRunnable.uptodate.getTime();
							System.out.println("Checker staleness: " + staleness);
						}
						expirationCount++;

					} 

					totalStaleness += staleness;
					avgStaleness = totalStaleness / totalRequest;
					System.out.println("Checker expirationCount: "+expirationCount);
					System.out.println("Checker total Request: "+totalRequest);
					expirationRatio = ((expirationCount *100) / totalRequest);

					// write both avgStaleness and ExpirationRatio to a file
					write(stalenessWriter, totalRequest, avgStaleness, staleness);
					write(expirationWriter, totalRequest, expirationRatio, expirationCount);

					// Indicate that current request has completed
					synchronized(CoordinatorRunnable.requestInProgress) {
						CoordinatorRunnable.requestInProgress = 0;
					}
					System.out.println("execution : 0");

				}
			}
		}

	}

	public void write(Writer writer, long totalRequest, long avg, long current) {
		try {
			writer.append(totalRequest + "," + avg +  "," + current +","+requestTime+","+CoordinatorRunnable.uptodate.getTime()+"\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
