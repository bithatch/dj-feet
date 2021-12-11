package uk.co.bithatch.djfeet;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;

import javafx.scene.text.Text;

public class TypedData extends AnnotatbleData {

	private String type;
	private String name;

	public TypedData(Element el) {
		type = el.attr("type");
		name = el.attr("name");
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	public List<Text> typeGraphic() {
		var l = new ArrayList<Text>();
		addTypeText(l, this);
		return l;
	}

}
