package uk.co.bithatch.djfeet;

import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.types.UInt32;

public class ObjectData implements BusTreeData {
	private String name;
	private String path;
	private UInt32 user;
	private String owner;
	private Introspectable introspectable;
	private BusData busData;
	
	public ObjectData(BusData busData) {
		this.busData = busData;
	}
	
	public BusData getBusData() {
		return busData;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setUser(UInt32 user) {
		this.user = user;
	}

	public UInt32 getUser() {
		return user;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getOwner() {
		return owner;
	}

	public void setIntrospectable(Introspectable introspectable) {
		this.introspectable = introspectable;
	}

	public Introspectable getIntrospectable() {
		return introspectable;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getDisplayName() {
		return getPath();
	}

	@Override
	public String toString() {
		return "DBusEntry [name=" + name + ", path=" + path + ", user=" + user + ", owner=" + owner + "]";
	}
}
