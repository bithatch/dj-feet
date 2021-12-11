package uk.co.bithatch.djfeet;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.tbee.javafx.scene.layout.MigPane;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.StageStyle;

public class ExecuteMethodPane extends MigPane {

	private TextFlow methodArgs;
	private Label busName;
	private Label objectPath;
	private Label interfaceName;
	private TextArea methodInput;
	private TextArea methodOutput;
	private DBusConnection conn;
	private MethodData methodData;

	public ExecuteMethodPane(DBusConnection conn, MethodData methodData) {
		super("fill, wrap 2", "[shrink 0][grow]", "[][][][][][grow][][grow]");

		this.conn = conn;
		this.methodData = methodData;

		add(bold(new Label("Method name")));
		add(methodArgs = new TextFlow());
		add(bold(new Label("Bus name")));
		add(busName = new Label());
		add(bold(new Label("Object path")));
		add(objectPath = new Label());
		add(bold(new Label("Interface")));
		add(interfaceName = new Label());
		add(bold(new Label("Method input")), "span 2");
		add(methodInput = new TextArea(), "span 2, grow");
		methodInput.setPrefColumnCount(32);
		methodInput.setPrefRowCount(6);
		add(bold(new Label("Method output")), "span 2");
		add(methodOutput = new TextArea(), "span 2, grow");
		methodOutput.setPrefColumnCount(32);
		methodOutput.setPrefRowCount(6);
		methodOutput.setEditable(false);

		methodArgs.getChildren().add(new Text(methodData.getDisplayName()));
		methodArgs.getChildren().addAll(methodData.argumentsText());
		busName.setText(methodData.getInterfaceData().getObjectData().getBusData().getName());
		objectPath.setText(methodData.getInterfaceData().getObjectData().getPath());
		interfaceName.setText(methodData.getInterfaceData().getDisplayName());
	}

	public void show() {
		Dialog<Boolean> dialog = new Dialog<>();
		dialog.initStyle(StageStyle.UTILITY);
		ButtonType loginButtonType = new ButtonType("Execute", ButtonData.APPLY);
		dialog.setResultConverter(b -> {
			if (b == loginButtonType) {
				execute();
				return Boolean.TRUE;
			} else
				return Boolean.FALSE;
		});
		dialog.setOnCloseRequest(e -> {
			if (dialog.getResult())
				e.consume();
		});
		dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CLOSE);
		dialog.getDialogPane().getStylesheets().add(DJFeet.class.getResource("DJFeet.css").toExternalForm());
		dialog.getDialogPane().setContent(this);
		dialog.show();
	}

	void execute() {
		SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
				this.getClass().getClassLoader());
		Object[] inputArgs = new Object[0];
		if (methodInput.getText().length() > 0) {
			ExpressionParser parser = new SpelExpressionParser(config);
			StandardEvaluationContext evalContext = new StandardEvaluationContext();
//			evalContext.setVariables(bindings);
			Expression exp = parser.parseExpression("new Object[] { " + methodInput.getText() + " }");
			try {
				inputArgs = (Object[]) exp.getValue(evalContext);
			} catch (SpelEvaluationException see) {
				methodOutput.setText(see.getMessage());
				see.printStackTrace();
				return;
			}
		}
		if (inputArgs.length != methodData.getInputArguments().size()) {
			methodOutput.setText("Incorrect number of parameters. Expected " + methodData.getInputArguments().size()
					+ ", got " + inputArgs.length);
			return;
		}

		try {
			var sig = methodData.getSignature();
			MethodCall call = new MethodCall(methodData.getInterfaceData().getObjectData().getBusData().getName(),
					methodData.getInterfaceData().getObjectData().getPath(),
					methodData.getInterfaceData().getDisplayName(), methodData.getDisplayName(), (byte) 0,
					inputArgs.length == 0 ? null : sig, inputArgs.length == 0 ? null : inputArgs);
			conn.sendMessage(call);

			Message reply = call.getReply();
			if (null == reply) {
				throw new NoReply("No reply within specified time");
			}

			if (reply instanceof Error) {
				((Error) reply).throwException();
			}

			var results = Arrays.asList(reply.getParameters());
			if (results.isEmpty())
				methodOutput.setText("This method did not return anything.");
			else
				methodOutput
						.setText(String.join(",", results.stream().map(o -> format(o)).collect(Collectors.toList())));
		} catch (Exception see) {
			methodOutput.setText(see.getClass().getName() + ": " + see.getMessage());
			see.printStackTrace();
			return;
		}

	}

	@SuppressWarnings("unchecked")
	static String format(Object o) {
		if (o == null)
			return "null";
		if (o instanceof String)
			return "\"" + ((String) o).replace("\"", "\\\"") + "\"";
		if (o instanceof Collection) {
			return "[" + String.join(", ",
					((Collection<Object>) o).stream().map(ob -> format(ob)).collect(Collectors.toList())) + "]";
		}
		if (o.getClass().isArray()) {
			return "[" + String.join(", ", (Collection<String>) (Arrays.asList((Object[]) o)).stream()
					.map(ob -> format(ob)).collect(Collectors.toList())) + "]";
		}
		return String.valueOf(o);
	}

	Label bold(Label label) {
		label.getStyleClass().add("strong");
		return label;
	}
}
