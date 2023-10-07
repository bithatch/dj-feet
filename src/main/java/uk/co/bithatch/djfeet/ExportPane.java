package uk.co.bithatch.djfeet;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.utils.Util;
import org.freedesktop.dbus.utils.generator.InterfaceCodeGenerator;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class ExportPane extends AbstractPane {
	private final static Preferences PREFS = Preferences.userNodeForPackage(DBusConnectionTab.class);

	private TextField packageName;
	private TextField outputDirectory;
	private Button browse;
	private DBusConnection conn;
	private List<ObjectData> objectData;
    private CheckBox boundProperties;
    private CheckBox disableFilter;

	public ExportPane(Stage stage, DBusConnection conn, List<ObjectData> objectData) {
		super(stage, "Export to Java Source", "Export", "fill, wrap 3", "[shrink 0][grow][]", "[][][][][][grow][][grow]");

		this.conn = conn;
		this.objectData = objectData;

		var home = new File(System.getProperty("user.home"));
		var dir = new File(PREFS.get("exportDirectory",
				home.getPath() + File.separator + "Documents" + File.separator + "DJ-Feet Exports"));

		add(bold(new Label("Package name")));
		add(packageName = new TextField(), "growx, span 2");
		packageName.setPromptText(objectData.size() == 0 ? "Automatic" : toPackageName(objectData.get(0).getName()));
		add(bold(new Label("Output Directory")));
		add(outputDirectory = new TextField(), "growx");
		outputDirectory.setText(dir.getAbsolutePath());
		add(browse = new Button(".."));
        add(boundProperties = new CheckBox("Use @DBusBoundProperty for properties"), "growx, span 3");
        boundProperties.setSelected(true);
        add(disableFilter = new CheckBox("Disable filter"), "growx, span 3");
		browse.setOnAction((e) -> {
			var directoryChooser = new DirectoryChooser();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			directoryChooser.setInitialDirectory(dir);
			directoryChooser.setTitle("Choose directory to export to.");
			var selectedDirectory = directoryChooser.showDialog(stage);
			if (selectedDirectory != null) {
				outputDirectory.setText(selectedDirectory.getAbsolutePath());
			}
		});
		
		setPrefWidth(500);
	}

	@Override	
	protected boolean execute() {
		try {
			var txt = outputDirectory.getText();
			if (txt.length() == 0)
				throw new IOException("Must supply an output directory.");
			var selectedDirectory = new File(txt);
			if (!selectedDirectory.exists()) {
				selectedDirectory.mkdirs();
			}
			PREFS.put("exportDirectory", selectedDirectory.getAbsolutePath());
			
			var allMap = new HashMap<File, String>();
			for (var data : objectData) {
				var in = conn.getRemoteObject(data.getBusData().getName(), data.getPath(), Introspectable.class);
				var idata = in.Introspect();
				var cg = new InterfaceCodeGenerator(disableFilter.isSelected(), idata, data.getPath(), "*", packageName.getText().equals("") ? null : packageName.getText(), boundProperties.isSelected());
				var analyze = cg.analyze(true);
				if (analyze == null) {
					throw new IllegalStateException("Unable to create interface files");
				}
				allMap.putAll(analyze);
			}
			
			writeToFile(selectedDirectory.getAbsolutePath(), allMap);
			
			var alt = new Alert(AlertType.INFORMATION);
			alt.initOwner(dialog.getOwner());
			alt.setTitle("Export Complete");
			alt.setContentText(String.format("%d Java sources were created.", allMap.size()));
			alt.showAndWait();
			return true;
		} catch (Exception dbe) {
			dbe.printStackTrace();
			var alt = new Alert(AlertType.ERROR);
			alt.initOwner(dialog.getOwner());
			alt.setTitle("Error");
			alt.setContentText(dbe.getMessage() == null ? "No message supplied." : dbe.getMessage());
			alt.showAndWait();
			return false;
		}
	}

	static void writeToFile(String _outputDir, Map<File, String> _filesToGenerate) {
		for (var entry : _filesToGenerate.entrySet()) {
			var outputFile = new File(_outputDir, entry.getKey().getPath());

			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}

			if (Util.writeTextFile(outputFile.getAbsolutePath(), entry.getValue(), Charset.defaultCharset(), false)) {
				LoggerFactory.getLogger(InterfaceCodeGenerator.class).info("Created class file {}",
						outputFile.getAbsolutePath());
			} else {
				LoggerFactory.getLogger(InterfaceCodeGenerator.class).error("Could not write content to class file {}",
						outputFile.getName());
			}
		}
	}
	
    static String toPackageName(String interfaceName) {
        if (interfaceName.contains(".")) {
            return interfaceName.substring(0, interfaceName.lastIndexOf('.')).toLowerCase();
        } else {
            return interfaceName.toLowerCase();
        }
    }
}
