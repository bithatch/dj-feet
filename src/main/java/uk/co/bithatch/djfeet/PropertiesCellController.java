package uk.co.bithatch.djfeet;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.text.TextFlow;

public final  class PropertiesCellController {
	private Label label = new Label();
	private TextFlow args = new TextFlow();
	private Label access = new Label();
	private Label val = new Label();

	{
		args.setPrefHeight(20);
	}

	public void setItem(BusTreeData item) {
		label.setText(item.getDisplayName());
		switch (((PropertyData) item).getAccess()) {
		case WRITE:
			access.setText("(write)");
			break;
		case READWRITE:
			access.setText("(read / write)");
			break;
		default:
			access.setText("(read)");
			break;
		}
		args.getChildren().clear();
		args.getChildren().addAll(((PropertyData) item).typeGraphic());
        args.getChildren().add(new Label(" "));
		args.getChildren().add(label);
        args.getChildren().add(new Label(" "));
		args.getChildren().add(access);
		if (((PropertyData) item).getValue() != null) {
			val.setText(" = " + ((PropertyData) item).getFormattedValue());
	        args.getChildren().add(new Label(" "));
			args.getChildren().add(val);
		}
	}

	public Node getView() {
		return args;
	}
}