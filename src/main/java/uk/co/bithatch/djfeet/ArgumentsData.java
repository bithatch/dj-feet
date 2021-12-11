package uk.co.bithatch.djfeet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import uk.co.bithatch.djfeet.ArgumentData.Direction;

public abstract class ArgumentsData extends AnnotatbleData implements BusTreeData {

	private List<ArgumentData> arguments = new ArrayList<>();
	private boolean showNamesInSignature = true;
	private Direction direction = Direction.IN;

	public List<ArgumentData> getArguments() {
		return arguments;
	}

	public List<Text> argumentsText() {
		var l = new ArrayList<Text>();
		buildTextList(l);
		return l;
	}
	
	protected Direction getDirection() {
		return direction;
	}

	protected void setDirection(Direction direction) {
		this.direction = direction;
	}

	protected void buildTextList(ArrayList<Text> l) {
		l.add(colorText("(", Color.MAGENTA));
		addArgs(l, direction);
		l.add(colorText(")", Color.MAGENTA));
	}

	public String getSignature() {
		return String.join("", getArguments().stream().map(s -> s.getType()).collect(Collectors.toList()));
	}

	public List<ArgumentData> getInputArguments() {
		var in = new ArrayList<ArgumentData>();
		for (ArgumentData arg : arguments) {
			if (arg.getDirection() == Direction.IN) {
				in.add(arg);
			}
		}
		return in;
	}

	public List<ArgumentData> getOutputArguments() {
		var out = new ArrayList<ArgumentData>();
		for (ArgumentData arg : arguments) {
			if (arg.getDirection() == Direction.OUT) {
				out.add(arg);
			}
		}
		return out;
	}

	protected void addArgs(List<Text> l, Direction dir) {
		int idx = 0;
		for (ArgumentData arg : arguments) {
			if (arg.getDirection() == dir) {
				if (idx > 0) {
					l.add(colorText(", ", Color.DARKMAGENTA));
				}
				addTypeText(l, arg);
				if (showNamesInSignature) {
					if (arg.getName().equals(""))
						l.add(colorText(" arg_" + idx, Color.BLACK));
					else
						l.add(colorText(" " + arg.getName(), Color.BLACK));
				}
				idx++;
			}
		}
	}

	protected boolean isShowNamesInSignature() {
		return showNamesInSignature;
	}

	protected void setShowNamesInSignature(boolean showNamesInSignature) {
		this.showNamesInSignature = showNamesInSignature;
	}
}
