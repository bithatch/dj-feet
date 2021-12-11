package uk.co.bithatch.djfeet;

import uk.co.bithatch.djfeet.ArgumentData.Direction;

public class SignalData extends ArgumentsData implements BusTreeData {
	
	private String name;

	public SignalData(String name) {
		this.name = name;
		setShowNamesInSignature(false);
		setDirection(Direction.OUT);
	}

	public String toString() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return name;
	}
}
