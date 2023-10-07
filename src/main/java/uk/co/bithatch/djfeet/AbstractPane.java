package uk.co.bithatch.djfeet;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import org.tbee.javafx.scene.layout.MigPane;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class AbstractPane extends MigPane {

	private final String applyText;
	private final String title;
	private final Stage stage;
	
	protected Dialog<Boolean> dialog;

	public AbstractPane(Stage stage, String title, String applyText) {
		super();
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public AbstractPane(Stage stage, String title, String applyText, LC layoutConstraints, AC colConstraints, AC rowConstraints) {
		super(layoutConstraints, colConstraints, rowConstraints);
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public AbstractPane(Stage stage, String title, String applyText, LC layoutConstraints, AC colConstraints) {
		super(layoutConstraints, colConstraints);
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public AbstractPane(Stage stage, String title, String applyText, LC layoutConstraints) {
		super(layoutConstraints);
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public AbstractPane(Stage stage, String title, String applyText, String layoutConstraints, String colConstraints, String rowConstraints) {
		super(layoutConstraints, colConstraints, rowConstraints);
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public AbstractPane(Stage stage, String title, String applyText, String layoutConstraints, String colConstraints) {
		super(layoutConstraints, colConstraints);
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public AbstractPane(Stage stage, String title, String applyText, String layoutConstraints) {
		super(layoutConstraints);
		this.applyText = applyText;
		this.title = title;
		this.stage = stage;
	}

	public void show() {
		dialog = new Dialog<>();
		dialog.initOwner(stage);
		dialog.initStyle(StageStyle.UTILITY);
		ButtonType loginButtonType = new ButtonType(applyText, ButtonData.APPLY);
		dialog.setResultConverter(b -> {
			if (b == loginButtonType) {
				return execute();
			} else
				return Boolean.FALSE;
		});
		dialog.setOnCloseRequest(e -> {
			if (dialog.getResult())
				e.consume();
		});
		dialog.setTitle(title);
		
		var pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(loginButtonType, ButtonType.CLOSE);
		DJFeetApp.decorateStylesheets(pane.getStylesheets());
		pane.setContent(this);
		dialog.show();
	}

	protected abstract boolean execute();

	protected Label bold(Label label) {
		label.getStyleClass().add("strong");
		return label;
	}
}
