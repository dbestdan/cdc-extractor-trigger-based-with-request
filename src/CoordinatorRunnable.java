import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Task of Coordinator 
 * 1. Get Maximum Sequence Id 
 * 2. Check number of worker thread available 
 * 3. Divide tasks to available worker threads 
 * 4. Execute Workers 
 * 5. Execute Checker ( Checker will continuously check whether the
 * 	  request has expired or not 
 * 6. Wait for workers to finish its task 
 * 7. Record Latency
 * 
 * @author hadoop
 *
 */
public class CoordinatorRunnable implements Runnable, Config {
	private Connection conn = null;
	private PreparedStatement stmt = null;
	private ResultSet rs = null;
	public long maxSeqID = 0L;
	private BlockingQueue<Task> queue = null;
	private long sessionEndTime = 0L;
	public static long sessionStartTime = 0L;
	private Writer out;
	private Writer latencyWriter;
	private String tableNames = null;
	private long timeWindow = 0L;
	private long totalLatency = 0L;
	private long avgLatency = 0L;
	private int threadSize = 0;
	public static Long totalRequest = 0L;
	public static Integer completedThread = 0;
	public static Timestamp uptodate = null;
	public static Long requestStartTime = 0L;
	public Timestamp requestStartTimeStamp = null;
	public static Long requestExpirationTime = 0L;
	public static Integer requestInProgress = 0;
	ArrayList<Thread> threads = new ArrayList<Thread>();

	public CoordinatorRunnable(BlockingQueue<Task> queue, long sessionEndTime) {
		this.queue = queue;
		this.sessionEndTime = sessionEndTime;
		try {
			conn = Client.getConnection();
			String query = "select max(event_id) from audit.logged_actions where " + "table_name in("
					+ tables.get(System.getProperty("tables")) + ") and "
							+ "pg_xact_commit_timestamp(transaction_id::text::xid) < ?";
			stmt = conn.prepareStatement(query);


		} catch (SQLException e) {
			e.printStackTrace();
		} 

		timeWindow = Long.parseLong(System.getProperty("timeWindow"));
		threadSize = Integer.parseInt(System.getProperty("numberOfThread"));

		String latencyFileName = "latency_" + "_coordinator_sleep_time_" + System.getProperty("sleepDuration")
				+ "_Thread_" + System.getProperty("numberOfThread") + "_" + dateFormat.format(new Date());

		try {
			latencyWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(latencyFileName, true), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}

		// uptodate time
		Date date = null;
		try {
			date = dateFormat.parse("2010-10-10-10-10-10");
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		long time = date.getTime();
		uptodate = new Timestamp(time);
	}

	@Override
	public void run() {

		// writing to the log

		while (sessionEndTime > System.currentTimeMillis()) {
			try {
				long sleepDuration = Long.parseLong(System.getProperty("sleepDuration"));
				Thread.sleep(sleepDuration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (totalRequest) {
				totalRequest++;
			}
			threads.clear();

			// start latency clock
			synchronized (requestStartTime) {
				requestStartTime = System.currentTimeMillis();
				requestStartTimeStamp = new Timestamp(requestStartTime);

			}

			synchronized (requestExpirationTime) {
				requestExpirationTime = requestStartTime + timeWindow;
			}

			//completedThread - how many threads has completed assigned task
			// In the beginning of the every request it will be set as 0
			synchronized (completedThread) {
				completedThread = 0;
			}
			// set execution on progress
			// requestInProgress = 1 or 0
			// 1 means current request is still in progress. Checker will set it to 0
			// after recording staleness and expiration ratio
			synchronized (CoordinatorRunnable.requestInProgress) {
				CoordinatorRunnable.requestInProgress = 1;
			}
			System.out.println("execution: " + CoordinatorRunnable.requestInProgress);

			

			try {
				System.out.println("Coordinator RequestTimeStamp " + requestStartTimeStamp);
				stmt.setTimestamp(1, requestStartTimeStamp);
				rs = stmt.executeQuery();
				rs.next();
				long tmpMaxSeqID = rs.getLong(1);
				
				System.out.println("Coordinator maxId " + tmpMaxSeqID);
				if (tmpMaxSeqID > maxSeqID) {
					
					
					
					long stepSize = (tmpMaxSeqID - maxSeqID) / threadSize;
					long tmpNextMaxSeqID = 0L;
					for (int i = 1; i <= threadSize; i++) {
						if (i == threadSize) {
							tmpNextMaxSeqID = tmpMaxSeqID;
							queue.put(new Task(maxSeqID, tmpNextMaxSeqID));
						} else {
							tmpNextMaxSeqID = maxSeqID + stepSize;
							queue.put(new Task(maxSeqID, tmpNextMaxSeqID));
							maxSeqID = tmpNextMaxSeqID;
						}

						// initiate worker thread
						threads.add(new Thread(new WorkerRunnable(i, queue)));

					}

					// Start WorkerThread
					for (int i = 0; i < threads.size(); i++) {
						threads.get(i).start();
					}

					// wait for threads to finish its task
					for (int i = 0; i < threads.size(); i++) {
						threads.get(i).join();
					}
					System.out.println("execution: " + CoordinatorRunnable.requestInProgress);
					System.out.println("request Start Time " + requestStartTime);
					System.out.println("expiration Time: " + requestExpirationTime);
					maxSeqID = tmpMaxSeqID;

					// end latency clock
					long requestEndTime = System.currentTimeMillis();

					// calculate avglatency
					long latency = requestEndTime - requestStartTime;
					totalLatency += latency;

					avgLatency = totalLatency / totalRequest;

					// write latency to a file
					writeLatency(totalRequest, avgLatency, latency);
					// wait for checker to finish its task
					while (requestInProgress == 1) {
						// when checker is done, it will set requestInProgress
						// to false
						System.out.println("Checker is still in progress");

					}
					// then cycle it again

				} else {
					System.out.println("Coordinator --------- No change");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				try {
					rs.close();
					// stmt.close();
					// conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public void writeLatency(long totalRequest, long avgLatency, long currentLatency) {
		try {
			latencyWriter.append(totalRequest + "," + avgLatency + "," + currentLatency +"\n");
			latencyWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
