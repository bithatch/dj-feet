package uk.co.bithatch.djfeet;

public class InterfaceData extends AnnotatbleData {
	
	private String name;
	private ObjectData objectData;

	public InterfaceData(ObjectData objectData, String name) {
		this.name = name;
		this.objectData = objectData;
	}
	
	public ObjectData getObjectData() {
		return objectData;
	}

	public String toString() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return name;
	}
}
