package uk.co.bithatch.djfeet;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
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
	private Stage stage;
	private ButtonBase refresh;
	private ButtonBase export;

	public DBusConnectionTab(Stage stage, String text, DBusConnection connection) throws DBusException {
		super(text);
		this.stage = stage;
		this.connection = connection;

		dbus = connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

		connection.addSigHandler(DBus.NameOwnerChanged.class, (e) -> {
			if (e.oldOwner.equals("") && !e.newOwner.equals("")) {
				var newBusData = createBusData(e.name, e.newOwner);
				Platform.runLater(() -> {
					/*
					 * Do our own sort, using Collections.sort() can cause excessive swaps, losing
					 * the selection
					 */
					for (int i = 0; i < names.size(); i++) {
						var n = names.get(i);
						if (newBusData.compareTo(n) < 0) {
							names.add(i, newBusData);
							return;
						}
					}
					names.add(0, newBusData);
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
				});
			} else {
				Platform.runLater(() -> {
					for (Iterator<BusData> bdIt = names.iterator(); bdIt.hasNext();) {
						BusData bd = bdIt.next();
						if (e.oldOwner.equals(bd.getOwner())) {
							bd.setOwner(e.newOwner);
							break;
						}
					}
				});
			}
		});

		/*
		 * Bus names. Note ListNames() does not seem to list all names, we must add the
		 * activatable names as well.
		 */
		var allNames = new LinkedHashSet<>(Arrays.asList(dbus.ListNames()));
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
		var busNameDetails = new MigPane("fill, wrap 2", "[][]", "[][][]");
		busNameDetails.add(new Label("Address:"));
		busNameDetails.add(busAddress);
		busNameDetails.add(new Label("Name:"));
		busNameDetails.add(busName);
		busNameDetails.add(new Label("Unique name:"));
		busNameDetails.add(uniqueName);

		// Refresh
		var refreshIcon = new FontIcon(FontAwesome.REFRESH);
		refreshIcon.setIconSize(32);
		refresh = new Button("Refresh", refreshIcon);
//		refresh.getStyleClass().setAll("btn", "btn-primary");
		refresh.setAlignment(Pos.CENTER);
		refresh.setContentDisplay(ContentDisplay.TOP);
		refresh.setOnAction((a) -> update());

		// Export
		var exportIcon = new FontIcon(FontAwesome.SAVE);
//		exportIcon.getStyleClass().add(JMetroStyleClass.BACKGROUND);
		exportIcon.setIconSize(32);
		export = new Button("Export", exportIcon);
//		export.getStyleClass().setAll("btn", "btn-primary");
		export.setContentDisplay(ContentDisplay.TOP);
		export.setAlignment(Pos.CENTER);
		export.setOnAction((a) -> export());

		// Actions
		var tools = new HBox();
		tools.setPadding(new Insets(4));
		tools.setSpacing(4);
		tools.setFillHeight(true);
		tools.setAlignment(Pos.CENTER);
		tools.getChildren().add(refresh);
		tools.getChildren().add(export);

		// Right Top
		var rightTop = new BorderPane();
		rightTop.setRight(tools);
		rightTop.setCenter(busNameDetails);

		// Tree View
		var group = PseudoClass.getPseudoClass("group");
		objects = new TreeView<>(new TreeItem<>(new ObjectData(null)));
		objects.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		objects.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> setAvailable());
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
			var props = connection.getRemoteObject(propData.getInterfaceData().getObjectData().getBusData().getName(),
					propData.getInterfaceData().getObjectData().getPath(), Properties.class);
			var val = props.Get(propData.getInterfaceData().getDisplayName(), propData.getName());
			propData.setValue(val);
			objects.refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void executeMethod(MethodData methodData) {
		new ExecuteMethodPane(stage, connection, methodData).show();
	}

	ObservableList<BusData> getBusNames(Set<String> allNames) {
		return FXCollections.observableArrayList(allNames.stream().map(n -> createBusData(n, null))
				.collect(Collectors.toList()).toArray(new BusData[0]));
	}

	BusData createBusData(String n, String owner) {
		long pid = -1;
		String cmd = null;
		try {
			if (owner == null)
				owner = dbus.GetNameOwner(n);
			pid = dbus.GetConnectionUnixProcessID(n).longValue();
			var p = Paths.get("/proc/" + pid + "/cmdline");
			if (Files.exists(p)) {
				try (var r = Files.newBufferedReader(p)) {
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

	void setAvailable() {
		int opaths = 0;
		for (var i : objects.getSelectionModel().getSelectedItems()) {
			if (i.getValue() instanceof ObjectData)
				opaths++;
		}
		refresh.setDisable(busNames.getSelectionModel().isEmpty());
		export.setDisable(
				busNames.getSelectionModel().isEmpty() || (objects.getSelectionModel().getSelectedItems().size() > 0
						&& opaths != objects.getSelectionModel().getSelectedItems().size()));
	}

	void export() {
		var sel = objects.getSelectionModel().getSelectedItems().isEmpty() ? objects.getRoot().getChildren()
				: objects.getSelectionModel().getSelectedItems();
		new ExportPane(stage, connection,
				new ArrayList<ObjectData>(sel.stream().filter(o -> o.getValue() instanceof ObjectData)
						.map(o -> (ObjectData) o.getValue()).collect(Collectors.toList())))
				.show();
	}

	void update() {
		var rootChildren = objects.getRoot().getChildren();
		rootChildren.clear();
		var selectedBus = busNames.getSelectionModel().getSelectedItem();
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
			busAddress.setText(connection.getAddress().toString());
			busName.setText(selectedBus.getName());
			uniqueName.setText(selectedBus.getOwner());
		}
		objects.setShowRoot(rootChildren.size() < 2);
		setAvailable();
	}

	void visitNode(BusData busData, TreeItem<BusTreeData> rootTreeItem, TreeItem<BusTreeData> treeItem, String name,
			String path) throws DBusException {
		if ("/org/freedesktop/DBus/Local".equals(path)) {
			// this will disconnects us.
			return;
		}

		var e = addEntry(busData, name, path);
		treeItem.setValue(e);
		var introspectData = e.getIntrospectable().Introspect();
		var doc = Jsoup.parse(introspectData, Parser.xmlParser());
		var root = doc.root();
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
		for (var element : root.children()) {
			if (element.tagName().equals("interface")) {
				if (interfacesItem == null) {
					interfacesItem = new TreeItem<>(new GroupData("Interfaces"));
					treeItem.getChildren().add(interfacesItem);
				}
				var interfaceItem = createInterfaceNode(e, element);
				interfacesItem.getChildren().add(interfaceItem);
			} else if (element.tagName().equals("node")) {
				var child = new TreeItem<BusTreeData>();
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
		for (var memberEl : element.children()) {
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
		var methodData = new MethodData(interfaceData, element.attr("name"));
		var methodItem = new TreeItem<BusTreeData>(methodData);
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
		var methodItem = new TreeItem<BusTreeData>(signalData);
		for (var memberEl : element.children()) {
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
		var methodItem = new TreeItem<BusTreeData>(propertiesData);
		for (var memberEl : element.children()) {
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
}
