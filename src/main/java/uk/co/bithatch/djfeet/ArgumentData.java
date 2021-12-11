package uk.co.bithatch.djfeet;

import org.jsoup.nodes.Element;

public class ArgumentData extends TypedData {

	public enum Direction {
		IN, OUT
	}

	private Direction direction;

	public ArgumentData(Element el, Direction defaultDirection) {
		super(el);
		if (el.hasAttr("direction"))
			direction = Direction.valueOf(el.attr("direction").toUpperCase());
		else
			direction = defaultDirection;
	}

	public Direction getDirection() {
		return direction;
	}

}
