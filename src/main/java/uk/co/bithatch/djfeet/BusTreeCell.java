package uk.co.bithatch.djfeet;

import javafx.scene.Node;
import javafx.scene.control.TreeCell;

public final class BusTreeCell extends TreeCell<BusTreeData> {

	private final ArgumentsCellController argumentsController = new ArgumentsCellController();
	private final Node argumentsView = argumentsController.getView();
	private final PropertiesCellController propertiesController = new PropertiesCellController();
	private final Node propertiesView = propertiesController.getView();
	private final DefaultCellController defaultController = new DefaultCellController();
	private final Node defaultView = defaultController.getView();

	@Override
	protected void updateItem(BusTreeData item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			if (item instanceof PropertyData) {
				propertiesController.setItem(item);
				setGraphic(propertiesView);
			} else if (item instanceof ArgumentsData) {
				argumentsController.setItem(item);
				setGraphic(argumentsView);
			} else {
				defaultController.setItem(item);
				setGraphic(defaultView);
			}
		}
	}
}