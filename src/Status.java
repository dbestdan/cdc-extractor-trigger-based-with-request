
public class Status {
	private long staleness;
	private long latency;
	private boolean expired;
	
	public long getStaleness() {
		return staleness;
	}
	public void setStaleness(long staleness) {
		this.staleness = staleness;
	}
	public long getLatency() {
		return latency;
	}
	public void setLatency(long latency) {
		this.latency = latency;
	}
	public boolean isExpired() {
		return expired;
	}
	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	
	

}
