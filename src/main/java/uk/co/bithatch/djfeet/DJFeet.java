package uk.co.bithatch.djfeet;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DJFeet {
	public static void main(String[] args) {
		DJFeetApp.main(args);
	}

	public static class DJFeetApp extends Application {
		public static void main(String[] args) {
			launch(args);
		}

		@Override
		public void start(final Stage primaryStage) {
			primaryStage.setTitle("dj-feet");
			primaryStage.setScene(new Scene(new BorderPane() {
				{
					setCenter(new StackPane() {
						{
							getChildren().add(new TabPane() {
								{
									try {
										getTabs().add(new DBusConnectionTab("System",
												DBusConnection.getConnection(DBusBusType.SYSTEM)));
									} catch (Exception e) {
									}
									try {
										getTabs().add(new DBusConnectionTab("Session",
												DBusConnection.getConnection(DBusBusType.SESSION)));
									} catch (Exception e) {
									}
									String address = null;
									try {
										try (BufferedReader reader = Files
												.newBufferedReader(Paths.get(System.getProperty("java.io.tmpdir"))
														.resolve("dbus-java.address"))) {
											address = reader.readLine();
										}
										getTabs().add(new DBusConnectionTab("D-Bus Java",
												DBusConnection.getConnection(address)));
									} catch (Exception e) {
										try  {
											address = Preferences.systemNodeForPackage(DJFeet.class).get("dbusAddress", null);
											if(address != null)
												getTabs().add(new DBusConnectionTab("D-Bus Java",
														DBusConnection.getConnection(address)));
										} 
										catch(Exception e2) {
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

								private void createBusTab(int insertPoint) {
									TextInputDialog input = new TextInputDialog();
									input.setHeaderText("Bus Address");
									input.setTitle("Add A Connection");
									String address = input.showAndWait().orElse(null);
									if (address == null) {
										getSelectionModel().select(0);
										if(getTabs().isEmpty())
											System.exit(0);
											
									} else {
										try {
											getTabs().add(insertPoint, new DBusConnectionTab(address,
													DBusConnection.getConnection(address)));
											getSelectionModel().select(insertPoint);
										} catch (Exception e) {
											e.printStackTrace();
											getSelectionModel().select(0);
											Platform.runLater(() -> createBusTab(insertPoint));
										}
									}
								}
							});

						}
					});
					setPrefSize(800, 600);
					getStylesheets().add(DJFeet.class.getResource("DJFeet.css").toExternalForm());
				}
			}));
			primaryStage.show();
			primaryStage.setOnCloseRequest((evt) -> {
				System.exit(0);
			});
		}
	}
}
