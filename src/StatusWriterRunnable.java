import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class StatusWriterRunnable implements Runnable, Config {
	private long totalLatency = 0L;
	private long avglatency = 0L;
	private long totalStaleness = 0L;
	private long avgStaleness = 0L;
	private long expirationRatio = 0L;
	private long expirationCount = 0L;

	private Writer stalenessWriter = null;
	private Writer expirationWriter = null;
	private Writer latencyWriter = null;

	private int threadSize = 0;
	private long sleepDuration = 0L;
	private long requestInterval = 0L;
	private long timeWindow = 0L;

	private long totalRequest = 0L;
	public static BlockingQueue<Status> requestStatusQueue = null;

	public StatusWriterRunnable() {
		requestStatusQueue = new ArrayBlockingQueue<Status>(10000);
		this.threadSize = Integer.parseInt(System.getProperty("numberOfThread"));
		this.sleepDuration = Long.parseLong(System.getProperty("sleepDuration"));
		this.requestInterval = Long.parseLong(System.getProperty("requestInterval"));
		this.timeWindow = Long.parseLong(System.getProperty("timeWindow"));

		String stalenessFileName = "staleness" + "_coordinator_sleep_time_" + sleepDuration + "_Thread_" + threadSize
				+ "_request_interval_" + requestInterval + "_time_window_" + timeWindow + "_"
				+ dateFormat.format(new Date());

		String expirationFileName = "expiration" + "_coordinator_sleep_time_" + sleepDuration + "_Thread_" + threadSize
				+ "_request_interval_" + requestInterval + "_time_window_" + timeWindow + "_"
				+ dateFormat.format(new Date());

		String latencyFileName = "latency" + "_coordinator_sleep_time_" + sleepDuration + "_Thread_" + threadSize
				+ "_request_interval_" + requestInterval + "_time_window_" + timeWindow + "_"
				+ dateFormat.format(new Date());
		try {
			stalenessWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(stalenessFileName, true), "UTF-8"));
			expirationWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(expirationFileName, true), "UTF-8"));
			latencyWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(latencyFileName, true), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while (true) {

			totalRequest++;
			Status status = null;
			try {
					status = requestStatusQueue.take();

				totalStaleness += status.getStaleness();
				totalLatency += status.getLatency();

				if (status.isExpired())
					expirationCount++;

				avgStaleness = totalStaleness / totalRequest;
				avglatency = totalLatency / totalRequest;
				expirationRatio = (expirationCount * 100) / totalRequest;

				write(stalenessWriter, avgStaleness, status.getStaleness());
				write(latencyWriter, avglatency, status.getLatency());

				long isExpired = status.isExpired() ? 1 : 0;
				write(expirationWriter, expirationRatio, isExpired);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void write(Writer writer, long avg, long current) {
		try {
			writer.append(totalRequest + "," + avg + "," + current + "\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
