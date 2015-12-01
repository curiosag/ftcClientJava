package ftcQueryEditor;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ParameterizedCompletion;

import com.google.common.base.Optional;

import gc.common.structures.OrderedIntTuple;
import interfacing.AbstractCompletion;
import interfacing.Completions;
import interfacing.SyntaxElementSource;
import util.StringUtil;

public class FtcCompletionProvider extends DefaultCompletionProvider {

	private final SyntaxElementSource syntaxElementSource;
	private final static List<Completion> noCompletions = new LinkedList<Completion>();
	private Optional<OrderedIntTuple> replacementBoundaries = Optional.absent();

	public FtcCompletionProvider(SyntaxElementSource syntaxElementSource) {
		this.syntaxElementSource = syntaxElementSource;
	}

	@Override
	public boolean isAutoActivateOkay(JTextComponent tc) {
		return false;
	};

	@Override
	public void clearParameterizedCompletionParams() {
		super.clearParameterizedCompletionParams();
	};

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {
		resetCompletions(comp);

		return super.getCompletionsImpl(comp);
	};

	@Override
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {
		return noCompletions;
	}

	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {
		return super.getAlreadyEnteredText(comp);
	}

	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent tc) {
		// TODO Auto-generated method stub
		return null;
	}

	private String recentText = null;
	private int recentCaretPosition = -1;

	private void resetCompletions(JTextComponent comp) {
		String text = comp.getText();
		int caretPosition = comp.getCaretPosition();

		if (caretPosition == recentCaretPosition && StringUtil.nullableEqual(text, recentText))
			return;
		else {
			Completions externalCompletions = syntaxElementSource.get(text, caretPosition);
			replacementBoundaries = externalCompletions.replacementBoundaries;

			clear();
			for (AbstractCompletion c : externalCompletions.getAll())
				addExternalCompletion(c);
		}
	}

	private void addExternalCompletion(AbstractCompletion c) {
		addCompletion(getExternalCompletion(c));
	}

	private Completion getExternalCompletion(AbstractCompletion c) {
		String shortDesc = c.displayName;
		String replacementText = Completions.patchFromCompletion(c);
		return new BasicCompletion(this, replacementText, shortDesc, getSubCompletions(c.children));
	}

	private List<Completion> getSubCompletions(List<AbstractCompletion> children) {
		List<Completion> result = new LinkedList<Completion>();

		for (AbstractCompletion c : children)
			result.add(getExternalCompletion(c));

		return result;
	}

}
