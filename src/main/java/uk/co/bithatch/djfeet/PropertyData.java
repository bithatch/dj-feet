package uk.co.bithatch.djfeet;

import org.jsoup.nodes.Element;

public class PropertyData extends TypedData implements BusTreeData {

	public enum Access {
		READ, WRITE, READWRITE
	}

	private Access access = Access.READ;
	private InterfaceData interfaceData;
	private Object value;

	public PropertyData(InterfaceData interfaceData, Element el) {
		super(el);
		this.interfaceData = interfaceData;
		if (el.hasAttr("access"))
			access = Access.valueOf(el.attr("access").toUpperCase());
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public InterfaceData getInterfaceData() {
		return interfaceData;
	}

	protected Access getAccess() {
		return access;
	}

	public String getFormattedValue() {
		return ExecuteMethodPane.format(value);
	}
}
