package uk.co.bithatch.djfeet;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.types.Variant;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class ExecuteMethodPane extends AbstractPane {

	private TextFlow methodArgs;
	private Label busName;
	private Label objectPath;
	private Label interfaceName;
	private TextArea methodInput;
	private TextArea methodOutput;
	private DBusConnection conn;
	private MethodData methodData;
    private MessageFactory factory;
    private SpelParserConfiguration config;
    private ConstructorResolver resolver;

	public ExecuteMethodPane(Stage stage, DBusConnection conn, MethodData methodData) {
		super(stage, "Execute", "Execute", "fill, wrap 2", "[shrink 0][grow]", "[][][][][][grow][][grow]");

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
		methodOutput.setWrapText(true);
		methodOutput.setEditable(false);

		methodArgs.getChildren().add(new Text(methodData.getDisplayName()));
		methodArgs.getChildren().addAll(methodData.argumentsText());
		busName.setText(methodData.getInterfaceData().getObjectData().getBusData().getName());
		objectPath.setText(methodData.getInterfaceData().getObjectData().getPath());
		interfaceName.setText(methodData.getInterfaceData().getDisplayName());
		
		factory = new MessageFactory(conn.getTransportConfig().getEndianess());
		

        config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
                this.getClass().getClassLoader());
        
        resolver = (context, typeName, argumentTypes) -> {
            if(typeName.equalsIgnoreCase("variant")) {
                if(argumentTypes.size() == 1) {
                    return new ConstructorExecutor() {
                        @Override
                        public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
                            return new TypedValue(new Variant<>(arguments[0]));
                        }
                    };
                }
                else if(argumentTypes.size() == 2) {
                    return new ConstructorExecutor() {
                        @Override
                        public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
                            return new TypedValue(new Variant<>(arguments[0], (String)arguments[1]));
                        }
                    };
                }
            }
            return null;
        };
        
        setPrefWidth(700);
	}

	@Override
	protected boolean execute() {
		Object[] inputArgs = new Object[0];
		if (methodInput.getText().length() > 0) {
			var parser = new SpelExpressionParser(config);
			var evalContext = new StandardEvaluationContext();
            evalContext.addConstructorResolver(resolver);
			var exp = parser.parseExpression("new Object[] { " + methodInput.getText() + " }");
			try {
				inputArgs = (Object[]) exp.getValue(evalContext);
			} catch (SpelEvaluationException see) {
	            methodOutput.setStyle("-fx-text-fill: red;");
				methodOutput.setText(see.getMessage());
				see.printStackTrace();
				return true;
			}
		}
		if (inputArgs.length != methodData.getInputArguments().size()) {
			methodOutput.setText("Incorrect number of parameters. Expected " + methodData.getInputArguments().size()
					+ ", got " + inputArgs.length);

			methodOutput.setStyle("-fx-text-fill: red;");
			return true;
		}

		try {
			var sig = getInputSignature(methodData);
			var call = factory.createMethodCall(methodData.getInterfaceData().getObjectData().getBusData().getName(),
					methodData.getInterfaceData().getObjectData().getPath(),
					methodData.getInterfaceData().getDisplayName(), methodData.getDisplayName(), (byte) 0,
					inputArgs.length == 0 ? null : sig, inputArgs.length == 0 ? null : inputArgs);
			conn.sendMessage(call);

			Message reply = call.getReply();
			if (null == reply) {
				throw new NoReply("No reply within specified time");
			}

			if (reply instanceof org.freedesktop.dbus.messages.Error e) {
				e.throwException();
			}

			var results = Arrays.asList(reply.getParameters());
            methodOutput.setStyle("");
			if (results.isEmpty())
				methodOutput.setText("This method did not return anything.");
			else {
				methodOutput
						.setText(String.join(",", results.stream().map(o -> format(o)).collect(Collectors.toList())));
			}
		} catch (Throwable see) {
            methodOutput.setStyle("-fx-text-fill: red;");
			methodOutput.setText(see.getClass().getName() + ": " + see.getMessage());
			see.printStackTrace();
		}

		return true;
	}

    static String getInputSignature(MethodData data) {
        return String.join("", data.getInputArguments().stream().map(s -> s.getType()).collect(Collectors.toList()));
    }

	@SuppressWarnings("unchecked")
	static String format(Object o) {
		if (o == null)
			return "null";
		if(o instanceof Map.Entry) {
		    var en = (Map.Entry<Object, Object>)o;
		    return format(en.getKey()) + ":" + format(en.getValue());
		}
		else if (o instanceof String)
			return "\"" + ((String) o).replace("\"", "\\\"") + "\"";
		else if (o instanceof Map) {
            return "{" + String.join(", ",
                    ((Map<Object, Object>) o).entrySet().stream().map(ob -> format(ob)).collect(Collectors.toList())) + "}";
        }
		else if (o instanceof Collection) {
			return "[" + String.join(", ",
					((Collection<Object>) o).stream().map(ob -> format(ob)).collect(Collectors.toList())) + "]";
		}
		if (o.getClass().isArray()) {
			return "[" + String.join(", ", (Collection<String>) (Arrays.asList((Object[]) o)).stream()
					.map(ob -> format(ob)).collect(Collectors.toList())) + "]";
		}
		return String.valueOf(o);
	}
}
