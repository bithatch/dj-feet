package uk.co.bithatch.djfeet;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.text.TextFlow;

public final class ArgumentsCellController {
	private Label label = new Label();
	private TextFlow args = new TextFlow();

	{
		args.setPrefHeight(20);

	}

	public void setItem(BusTreeData item) {
		label.setText(item.getDisplayName());
		args.getChildren().clear();
		args.getChildren().add(label);
		args.getChildren().addAll(((ArgumentsData) item).argumentsText());
	}

	public Node getView() {
		return args;
	}
}