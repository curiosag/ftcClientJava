package ftcQueryEditor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;
import interfaces.CompletionsSource;
import interfaces.SqlCompletionType;
import structures.AbstractCompletion;
import structures.Completions;
import util.Op;
import util.StringUtil;

public class FtcCompletionProvider extends DefaultCompletionProvider {

	private final CompletionsSource completionsSource;
	private final static List<Completion> noCompletions = new LinkedList<Completion>();
	private Optional<OrderedIntTuple> replacementBoundaries = Optional.absent();
	private SqlCompletionType[] completionTypes;

	private static final SqlCompletionType[] schemaCompletions = {SqlCompletionType.table, SqlCompletionType.column};
	
	public FtcCompletionProvider(CompletionsSource completionsSource, SqlCompletionType ... types) {
		this.completionsSource = completionsSource;
		this.completionTypes = types;
		setListCellRenderer(new FtcListCellRenderer());   
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
		return super.getParameterizedCompletions(tc);
	}

	private String recentText = null;
	private int recentCaretPosition = -1;

	private void resetCompletions(JTextComponent comp) {
		String text = comp.getText();
		int caretPosition = comp.getCaretPosition();
		if (caretPosition == recentCaretPosition && StringUtil.nullableEqual(text, recentText))
			return;
		else {
			Completions externalCompletions = completionsSource.get(text, caretPosition);
			replacementBoundaries = externalCompletions.replacementBoundaries;
			clear();
			for (AbstractCompletion c : externalCompletions.getAll())
				addCompletion(c);
		}
	}

	private void addCompletion(AbstractCompletion c) {
		if(completionTypes.length == 0 || Op.in(c.completionType, completionTypes))
			addCompletion(createCompletion(this, c));
	}

	public int size(){
		return completions.size();
	}
	
	public List<Completion> getParameterCompletions(JTextComponent comp)
	{
		List<Completion> result = new ArrayList<Completion>();
		
		String text = comp.getText();
		int caretPosition = comp.getCaretPosition();
		
		for (AbstractCompletion c : completionsSource.get(text, caretPosition).getAll())
			if (Op.in(c.completionType, schemaCompletions)) // recursive template completions would be cool but...
				result.add(createParamCompletion(this, c));

		return result;
	}

	public static org.fife.ui.autocomplete.AbstractCompletion createCompletion(CompletionProvider provider, AbstractCompletion c)
	{
		if (Op.in(c.completionType, schemaCompletions))
			return new BasicCompletion(provider, Completions.patchFromCompletion(c), null, getSubCompletions(provider, c.children));
		else
			return new TemplateCompletion(provider, c.displayName, c.displayName, Completions.patchFromCompletion(c)) ;
		
	}
	
	
	public static org.fife.ui.autocomplete.AbstractCompletion createParamCompletion(CompletionProvider provider, AbstractCompletion c)
	{
		Check.isTrue(Op.in(c.completionType, schemaCompletions));
		
		// null for short description will cause it to patch displayName only
		return new  BasicCompletion(provider, c.displayName);
		
	}
	
	private static List<Completion> getSubCompletions(CompletionProvider provider, List<AbstractCompletion> children) {
		List<Completion> result = new LinkedList<Completion>();

		for (AbstractCompletion c : children){
			Completion item = new BasicCompletion(provider, Completions.patchFromCompletion(c),  c.displayName , getSubCompletions(provider, c.children));
			
			result.add(item);
		}

		return result;
	}

	
}
