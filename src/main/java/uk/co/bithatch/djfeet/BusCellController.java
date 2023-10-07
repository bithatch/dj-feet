package uk.co.bithatch.djfeet;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public final class BusCellController {
	private BorderPane view = new BorderPane();
	private Label label = new Label();
	private Label info = new Label();

	{
		info.setAlignment(Pos.CENTER_LEFT);
		label.setAlignment(Pos.CENTER_LEFT);
		label.getStyleClass().add("strong");
		view.setTop(label);
		view.setBottom(info);
	}

	public void setItem(BusData item) {
		label.setText(item.getName());
		var b = new StringBuilder();
		b.append("activatable: ");
		b.append(item.isActivatable() ? "yes" : "no");
		if (item.getPid() > -1) {
			b.append(", pid: ");
			b.append(item.getPid());
		}
		if (item.getCmd() != null) {
			b.append(", cmd: ");
			b.append(item.getCmd());
		}
		info.setText(b.toString());
	}

	public Node getView() {
		return view;
	}
}