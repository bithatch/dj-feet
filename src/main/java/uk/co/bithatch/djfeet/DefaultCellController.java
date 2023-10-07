package uk.co.bithatch.djfeet;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;

public final class DefaultCellController {
	private Label label = new Label();

	{
		label.setAlignment(Pos.CENTER_LEFT);
	}

	public void setItem(BusTreeData item) {
		label.setText(item.getDisplayName());
	}

	public Node getView() {
		return label;
	}
}