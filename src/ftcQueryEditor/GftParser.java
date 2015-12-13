package ftcQueryEditor;

import java.util.List;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level;

import cg.common.check.Check;
import cg.common.swing.DocumentUtil;
import interfaces.SyntaxElement;
import interfaces.SyntaxElementSource;
import interfaces.SyntaxElementType;

public class GftParser extends AbstractParser {

	private SyntaxElementSource syntaxElementSource;
	
	public GftParser(SyntaxElementSource syntaxElementSource)
	{
		Check.notNull(syntaxElementSource);
		this.syntaxElementSource = syntaxElementSource;
		setEnabled(true);
	}
	
	@Override
	public ParseResult parse(RSyntaxDocument doc, String style) {
		DefaultParseResult result = new DefaultParseResult(this);
		List<SyntaxElement> elements = syntaxElementSource.get(DocumentUtil.getText(doc));
		
		for (SyntaxElement e : elements) 
			maybeAddNotice(result, e);
		
		return result;
	}

	private void maybeAddNotice(DefaultParseResult result, SyntaxElement e) {
		Level level;
		if (e.type == SyntaxElementType.error)
			level = Level.ERROR;
		else if (e.hasSemanticError())
			level = Level.WARNING;
		else
			return;
		DefaultParserNotice notice = new DefaultParserNotice(this, null, -1, e.from, e.value.length());
		notice.setLevel(level);
		result.addNotice(notice);
	}

}
