package uk.co.bithatch.djfeet;

import javafx.scene.Node;
import javafx.scene.control.ListCell;

public final class BusCell extends ListCell<BusData> {

	private final BusCellController ccc = new BusCellController();
	private final Node view = ccc.getView();

	@Override
	protected void updateItem(BusData item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			ccc.setItem(item);
			setGraphic(view);
		}
	}
}