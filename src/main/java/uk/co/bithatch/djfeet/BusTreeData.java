package uk.co.bithatch.djfeet;

public interface BusTreeData {

	default boolean isGroup() {
		return false;
	}
	
	String getDisplayName();
}
