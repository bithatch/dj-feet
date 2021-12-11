package uk.co.bithatch.djfeet;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextFlow;
import uk.co.bithatch.djfeet.ArgumentData.Direction;

public class DBusConnectionTab extends Tab {

	private DBusConnection connection;
	private Label busAddress;
	private Label uniqueName;
	private Label busName;
	private DBus dbus;
	private ListView<BusData> busNames;
	private TreeView<BusTreeData> objects;
	private FilteredList<BusData> filteredData;
	private ObservableList<BusData> names;
	private List<String> activatable;

	public DBusConnectionTab(String text, DBusConnection connection) throws DBusException {
		super(text);
		this.connection = connection;

		dbus = connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

		connection.addSigHandler(DBus.NameOwnerChanged.class, (e) -> {
			if (e.oldOwner.equals("") && !e.newOwner.equals("")) {
				var newBusData = createBusData(e.name, e.newOwner);
				Platform.runLater(() -> {
					names.add(0, newBusData);
					Collections.sort(names);
				});
			} else if (e.newOwner.equals("") && !e.oldOwner.equals("")) {
				Platform.runLater(() -> {
					for (Iterator<BusData> bdIt = names.iterator(); bdIt.hasNext();) {
						BusData bd = bdIt.next();
						if (e.oldOwner.equals(bd.getOwner())) {
							bdIt.remove();
							break;
						}
					}
					Collections.sort(names);
				});
			} else {
				Platform.runLater(() -> {
					for (Iterator<BusData> bdIt = names.iterator(); bdIt.hasNext();) {
						BusData bd = bdIt.next();
						if (e.oldOwner.equals(bd.getOwner())) {
							bd.setOwner(e.newOwner);
							busNames.refresh();
							break;
						}
					}
					Collections.sort(names);
				});
			}
		});

		/*
		 * Bus names. Note ListNames() does not seem to list all names, we must add the
		 * activatable names as well.
		 */
		Set<String> allNames = new LinkedHashSet<>(Arrays.asList(dbus.ListNames()));
		try {
			activatable = Arrays.asList(dbus.ListActivatableNames());
		} catch (DBusExecutionException dbe) {
			/*
			 * TODO: dbus-java is returning null here, should be empty array
			 */
			activatable = Collections.emptyList();
		}
		allNames.addAll(activatable);
		names = getBusNames(allNames);
		Collections.sort(names);
		filteredData = new FilteredList<>(names, s -> true);

		var busNameSearch = new TextField();
		busNameSearch.setPromptText("🔍");
		busNameSearch.textProperty().addListener(obs -> {
			String filter = busNameSearch.getText();
			if (filter == null || filter.length() == 0) {
				filteredData.setPredicate(s -> true);
			} else {
				filteredData.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
			}
		});
		busNames = new ListView<>(filteredData);
		busNames.setCellFactory(lv -> new BusCell());
		busNames.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
			update();
		});

		// Search pane
		var busNameSearchPane = new BorderPane();
		busNameSearchPane.getStyleClass().add("spaced");
		busNameSearchPane.setCenter(busNameSearch);

		// Left
		var left = new BorderPane();
		left.setTop(busNameSearchPane);
		left.setCenter(busNames);

		// Bus details
		busAddress = new Label("");
		busName = new Label("");
		uniqueName = new Label("");
		var refreshIcon = new FontIcon(FontAwesome.REFRESH);
		refreshIcon.setIconSize(32);
		var refresh = new Hyperlink(null, refreshIcon);
		refresh.setAlignment(Pos.CENTER);
		refresh.setOnAction((a) -> update());
		var busNameDetails = new MigPane("fill, wrap 2", "[][]", "[][][]");
		busNameDetails.add(new Label("Address:"));
		busNameDetails.add(busAddress);
		busNameDetails.add(new Label("Name:"));
		busNameDetails.add(busName);
		busNameDetails.add(new Label("Unique name:"));
		busNameDetails.add(uniqueName);

		// Right Top
		var rightTop = new BorderPane();
		rightTop.setRight(refresh);
		rightTop.setCenter(busNameDetails);

		// Tree View
		PseudoClass group = PseudoClass.getPseudoClass("group");
		objects = new TreeView<>(new TreeItem<>(new ObjectData(null)));
		objects.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				TreeItem<BusTreeData> selected = objects.getSelectionModel().getSelectedItem();
				if (selected != null && selected.getValue() instanceof MethodData)
					executeMethod((MethodData) selected.getValue());
				else if (selected != null && selected.getValue() instanceof PropertyData)
					loadProperty((PropertyData) selected.getValue());
			}
		});
		objects.setCellFactory(lv -> {
			var cell = new BusTreeCell();
			cell.treeItemProperty().addListener((obs, oldTreeItem, newTreeItem) -> cell.pseudoClassStateChanged(group,
					newTreeItem != null && newTreeItem.getValue().isGroup()));
			return cell;
		});

		// Right
		var right = new BorderPane();
		right.setTop(rightTop);
		right.setCenter(objects);

		// Split
		var sp = new SplitPane(left, right);
		sp.setDividerPositions(0.4d);
		setContent(sp);

		// First update
		update();
	}

	void loadProperty(PropertyData propData) {
		try {
			Properties props = connection.getRemoteObject(
					propData.getInterfaceData().getObjectData().getBusData().getName(),
					propData.getInterfaceData().getObjectData().getPath(), Properties.class);
			var val = props.Get(propData.getInterfaceData().getDisplayName(), propData.getName());
			propData.setValue(val);
			objects.refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void executeMethod(MethodData methodData) {
		ExecuteMethodPane exec = new ExecuteMethodPane(connection, methodData);
		exec.show();
	}

	ObservableList<BusData> getBusNames(Set<String> allNames) {
		return FXCollections.observableArrayList(
				allNames.stream().map(n -> createBusData(n, null)).collect(Collectors.toList()).toArray(new BusData[0]));
	}

	BusData createBusData(String n, String owner) {
		long pid = -1;
		String cmd = null;
		try {
			if(owner == null)
				owner = dbus.GetNameOwner(n);
			pid = dbus.GetConnectionUnixProcessID(n).longValue();
			Path p = Paths.get("/proc/" + pid + "/cmdline");
			if (Files.exists(p)) {
				try (BufferedReader r = Files.newBufferedReader(p)) {
					cmd = String.join(" ", r.readLine().split("\0"));
				}
			}
		} catch (Exception e) {
		}
		return new BusData(n, activatable.contains(n), pid, cmd, owner);
	}

	boolean isStrong(TreeItem<BusTreeData> item) {
		return item.getValue() != null && item.getValue().isGroup();
	}

	void update() {
		ObservableList<TreeItem<BusTreeData>> rootChildren = objects.getRoot().getChildren();
		rootChildren.clear();
		BusData selectedBus = busNames.getSelectionModel().getSelectedItem();
		if (selectedBus == null) {
			busAddress.setText("");
			busName.setText("");
			uniqueName.setText("");
		} else {
			try {
				visitNode(selectedBus, objects.getRoot(), objects.getRoot(), selectedBus.getName(), "/");
			} catch (DBusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			busAddress.setText(connection.getAddress().getRawAddress());
			busName.setText(selectedBus.getName());
			uniqueName.setText(selectedBus.getOwner());
		}
		objects.setShowRoot(rootChildren.size() < 2);
	}

	void visitNode(BusData busData, TreeItem<BusTreeData> rootTreeItem, TreeItem<BusTreeData> treeItem, String name,
			String path) throws DBusException {
		if ("/org/freedesktop/DBus/Local".equals(path)) {
			// this will disconnects us.
			return;
		}

		ObjectData e = addEntry(busData, name, path);
		treeItem.setValue(e);
		String introspectData = e.getIntrospectable().Introspect();
		Document doc = Jsoup.parse(introspectData, Parser.xmlParser());
		Element root = doc.root();
		if (root.childrenSize() == 0)
			return;
		else {
			if (root.childrenSize() > 1)
				System.err.println("WARNING: Unexpected 2nd node at root");
			root = root.child(0);
		}
		if (!root.tagName().equals("node")) {
			return;
		}
		TreeItem<BusTreeData> interfacesItem = null;
		for (Element element : root.children()) {
			if (element.tagName().equals("interface")) {
				if (interfacesItem == null) {
					interfacesItem = new TreeItem<>(new GroupData("Interfaces"));
					treeItem.getChildren().add(interfacesItem);
				}
				TreeItem<BusTreeData> interfaceItem = createInterfaceNode(e, element);
				interfacesItem.getChildren().add(interfaceItem);
			} else if (element.tagName().equals("node")) {
				TreeItem<BusTreeData> child = new TreeItem<>();
				String nodeName = element.attr("name");
				if (path.endsWith("/")) {
					visitNode(busData, rootTreeItem, child, name, path + nodeName);
				} else {
					visitNode(busData, rootTreeItem, child, name, path + '/' + nodeName);
				}
				if (!child.getChildren().isEmpty())
					rootTreeItem.getChildren().add(child);
			}

		}
	}

	TreeItem<BusTreeData> createInterfaceNode(ObjectData objectData, Element element) {
		var interfaceData = new InterfaceData(objectData, element.attr("name"));
		TreeItem<BusTreeData> interfaceItem = new TreeItem<>(interfaceData);
		TreeItem<BusTreeData> methodsItem = null;
		TreeItem<BusTreeData> signalsItem = null;
		TreeItem<BusTreeData> propertiesItem = null;
		for (Element memberEl : element.children()) {
			if (memberEl.tagName().equals("method")) {
				if (methodsItem == null) {
					methodsItem = new TreeItem<>(new GroupData("Methods"));
					interfaceItem.getChildren().add(methodsItem);
				}
				methodsItem.getChildren().add(createMethodNode(interfaceData, memberEl));
			} else if (memberEl.tagName().equals("signal")) {
				if (signalsItem == null) {
					signalsItem = new TreeItem<>(new GroupData("Signals"));
					interfaceItem.getChildren().add(signalsItem);
				}
				signalsItem.getChildren().add(createSignalNode(memberEl));
			} else if (memberEl.tagName().equals("property")) {
				if (propertiesItem == null) {
					propertiesItem = new TreeItem<>(new GroupData("Properties"));
					interfaceItem.getChildren().add(propertiesItem);
				}
				propertiesItem.getChildren().add(createPropertiesNode(interfaceData, memberEl));
			} else if (memberEl.tagName().equals("annotation")) {
				interfaceData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return interfaceItem;
	}

	TreeItem<BusTreeData> createMethodNode(InterfaceData interfaceData, Element element) {
		MethodData methodData = new MethodData(interfaceData, element.attr("name"));
		TreeItem<BusTreeData> methodItem = new TreeItem<>(methodData);
		for (Element memberEl : element.children()) {
			if (memberEl.tagName().equals("arg")) {
				methodData.getArguments().add(new ArgumentData(memberEl, Direction.IN));
			} else if (memberEl.tagName().equals("annotation")) {
				methodData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return methodItem;
	}

	TreeItem<BusTreeData> createSignalNode(Element element) {
		var signalData = new SignalData(element.attr("name"));
		TreeItem<BusTreeData> methodItem = new TreeItem<>(signalData);
		for (Element memberEl : element.children()) {
			if (memberEl.tagName().equals("arg")) {
				signalData.getArguments().add(new ArgumentData(memberEl, Direction.OUT));
			} else if (memberEl.tagName().equals("annotation")) {
				signalData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return methodItem;
	}

	TreeItem<BusTreeData> createPropertiesNode(InterfaceData interfaceData, Element element) {
		var propertiesData = new PropertyData(interfaceData, element);
		TreeItem<BusTreeData> methodItem = new TreeItem<>(propertiesData);
		for (Element memberEl : element.children()) {
			if (memberEl.tagName().equals("annotation")) {
				propertiesData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return methodItem;
	}

	ObjectData addEntry(BusData busData, String name, String path) throws DBusException {
		ObjectData entry = new ObjectData(busData);
		entry.setName(name);
		entry.setPath(path);
		Introspectable introspectable = connection.getRemoteObject(name, path, Introspectable.class);
		entry.setIntrospectable(introspectable);
		return entry;
	}

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
			StringBuilder b = new StringBuilder();
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

	public final class PropertiesCellController {
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
			args.getChildren().add(label);
			args.getChildren().add(access);
			if (((PropertyData) item).getValue() != null) {
				val.setText(" = " + ((PropertyData) item).getFormattedValue());
				args.getChildren().add(val);
			}
		}

		public Node getView() {
			return args;
		}
	}

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
}
