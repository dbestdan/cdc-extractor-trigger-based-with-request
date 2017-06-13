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
	public static Timestamp uptodate = null;

	public CoordinatorRunnable(BlockingQueue<Task> queue, long sessionEndTime) {
		this.queue = queue;
		this.sessionEndTime = sessionEndTime;
		try {
			conn = Client.getConnection();
			String query = "select max(event_id) from audit.logged_actions where " + "table_name in("
					+ tables.get(System.getProperty("tables")) + ")";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();
			rs.next();
			maxSeqID = rs.getLong(1);
			
			
			uptodate = new Timestamp(System.currentTimeMillis());

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

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

			try {
				rs = stmt.executeQuery();
				rs.next();
				long tmpMaxSeqID = rs.getLong(1);
				if (tmpMaxSeqID > maxSeqID) {
					queue.put(new Task(maxSeqID, tmpMaxSeqID));
					maxSeqID = tmpMaxSeqID;
				}
				System.out.println("Coordinator maxseqId: " + maxSeqID);
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
}
