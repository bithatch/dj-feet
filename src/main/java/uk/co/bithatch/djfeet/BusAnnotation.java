package uk.co.bithatch.djfeet;

import org.jsoup.nodes.Element;

public class BusAnnotation {

	private String name;
	private String value;

	public BusAnnotation(Element el) {
		name = el.attr("name");
		value = el.attr("value");
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

}
