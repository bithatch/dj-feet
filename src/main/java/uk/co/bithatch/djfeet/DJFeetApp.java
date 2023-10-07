package uk.co.bithatch.djfeet;

import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.scenicview.ScenicView;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DJFeetApp extends Application {
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) {
		primaryStage.setTitle("dj-feet");
		var scene = createScene(primaryStage);
		new JMetro(Style.LIGHT).setScene(scene);
		decorateStylesheets(scene.getStylesheets());
		
        try {
            if (Boolean.getBoolean("djfeet.debugScene"))
                ScenicView.show(scene);
        } catch (Throwable e) {}
		
		primaryStage.setScene(scene);
		primaryStage.show();
		primaryStage.setOnCloseRequest((evt) -> {
			System.exit(0);
		});
	}

    public static void decorateStylesheets(ObservableList<String> stylesheets) {
        stylesheets.add(DJFeet.class.getResource("DJFeet.css").toExternalForm());
    }

	private Scene createScene(final Stage primaryStage) {
		return new Scene(new BorderPane() {
			{
				setCenter(new StackPane() {
					{
						getChildren().add(createTabs(primaryStage));
					}
				});
				setPrefSize(800, 600);
				getStyleClass().add(JMetroStyleClass.UNDERLINE_TAB_PANE);
				getStylesheets().add(DJFeet.class.getResource("DJFeet.css").toExternalForm());
			}

		});
	}

	private TabPane createTabs(final Stage primaryStage) {
		return new TabPane() {
			{
				createTabForConnectionBuilder(DBusConnectionBuilder.forSystemBus(), "System");
				createTabForConnectionBuilder(DBusConnectionBuilder.forSessionBus(), "Session");
				String address = null;
				try {
					try (BufferedReader reader = Files.newBufferedReader(
							Paths.get(System.getProperty("java.io.tmpdir")).resolve("dbus-java.address"))) {
						address = reader.readLine();
					}
					createTabForConnectionBuilder(DBusConnectionBuilder.forAddress(address), "D-Bus Java");
				} catch (Exception e) {
					try {
						address = Preferences.systemNodeForPackage(DJFeet.class).get("dbusAddress", null);
						if (address != null)
							createTabForConnectionBuilder(DBusConnectionBuilder.forAddress(address), "D-Bus Java");
					} catch (Exception e2) {
					}
				}
				int insertPoint = getTabs().size();
				try {
					Tab addSession = new Tab(null, null);
					addSession.setGraphic(new FontIcon(FontAwesome.PLUS_CIRCLE));
					getTabs().add(addSession);
				} catch (Exception e) {
					e.printStackTrace();
				}
				getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
					if (n.getText() == null) {
						createBusTab(insertPoint);

					}
				});
				if (insertPoint == 0)
					createBusTab(0);
			}

			private void createTabForConnectionBuilder(DBusConnectionBuilder builder, String name) {
				try {
					var tab = new DBusConnectionTab(primaryStage, name, builder.build());
					tab.setClosable(false);
					getTabs().add(tab);
				} catch (Exception e) {
				}
			}

			private void createBusTab(int insertPoint) {
				var input = new TextInputDialog();
				input.setHeaderText("Bus Address");
				input.setTitle("Add A Connection");
				var address = input.showAndWait().orElse(null);
				if (address == null) {
					getSelectionModel().select(0);
					if (getTabs().isEmpty())
						System.exit(0);

				} else {
					try {
						getTabs().add(insertPoint, new DBusConnectionTab(primaryStage, address,
								DBusConnectionBuilder.forAddress(address).build()));
						getSelectionModel().select(insertPoint);
					} catch (Exception e) {
						e.printStackTrace();
						getSelectionModel().select(0);
						Platform.runLater(() -> createBusTab(insertPoint));
					}
				}
			}
		};
	}
}