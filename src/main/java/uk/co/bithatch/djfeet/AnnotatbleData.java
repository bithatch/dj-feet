package uk.co.bithatch.djfeet;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.DBusStructType;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

public abstract class AnnotatbleData implements BusTreeData {

	private List<BusAnnotation> annotations = new ArrayList<>();

	public List<BusAnnotation> getAnnotations() {
		return annotations;
	}

	protected void addTypeText(List<Text> l, TypedData arg) {
		try {
			List<Type> results = new ArrayList<>();
			Marshalling.getJavaType(arg.getType(), results, -1);
			l.add(colorText(String.join(", ", results.stream().map(r -> typeName(r)).collect(Collectors.toList())),
					Color.DARKGREEN));
		} catch (DBusException e) {
			l.add(colorText("[" + arg.getType() + "]", Color.RED));
		}
	}

	protected static String typeName(Type type) {
		var typeName = type.getTypeName();
		String typeText = null;
		char[] braces = { '[', ']' };
		if (typeName.equals(CharSequence.class.getName())) {
			typeText = "String";
		} else {
			int idx = typeName.lastIndexOf('@');
			if (idx != -1) {
				typeName = typeName.substring(0, idx);
				if (typeName.equals(DBusListType.class.getName())) {
					typeText = "Array";
				} else if (typeName.equals(DBusMapType.class.getName())) {
					typeText = "Dict";
					braces = new char[] { '{', '}' };
				} else if (typeName.equals(DBusStructType.class.getName())) {
					typeText = "Struct";
					braces = new char[] { '(', ')' };
				}
			}
			if (typeText == null) {
				idx = typeName.lastIndexOf('.');
				typeText = idx == -1 ? typeName : typeName.substring(idx + 1);
			}
			if (typeText.equals(DBusPath.class.getSimpleName())) {
				typeText = "Object Path";
			} else if (typeText.equals(Integer.class.getSimpleName())) {
				typeText = "Int32";
			} else if (typeText.equals(Long.class.getSimpleName())) {
				typeText = "Int64";
			}
		}
		if (type instanceof ParameterizedType) {
			typeText += " of " + braces[0]
					+ String.join(", ", Arrays.asList(((ParameterizedType) type).getActualTypeArguments()).stream()
							.map(t -> typeName(t)).collect(Collectors.toList()))
					+ braces[1];
		}
		return typeText;
	}

	protected static Text colorText(String text, Paint color) {
		Text t = new Text(text);
		t.setFill(color);
		return t;
	}
}
