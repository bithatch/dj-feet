package uk.co.bithatch.djfeet;

public class BusData implements Comparable<BusData> {
	
	private String name;
	private boolean activatable;
	private long pid;
	private String cmd;
	private String owner;

	public BusData(String name, boolean activatable, long pid, String cmd, String owner) {
		this.name = name;
		this.activatable = activatable;
		this.pid = pid;
		this.cmd = cmd;
		this.owner = owner;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public String getCmd() {
		return cmd;
	}
	
	public long getPid() {
		return pid;
	}
	
	public boolean isActivatable() {
		return activatable;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}

	@Override
	public int compareTo(BusData o) {
		String an = name;
		String bn = o.getName();
		if (an.startsWith(":") && !bn.startsWith(":")) {
			return 1;
		} else if (!an.startsWith(":") && bn.startsWith(":")) {
			return -1;
		} else
			return an.compareTo(bn);
	}
}
