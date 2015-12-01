package ftcQueryEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.tree.DefaultMutableTreeNode;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.common.base.Optional;

import cg.common.check.Check;
import cg.common.interfaces.AbstractKeyListener;
import ftcClientJava.CompletionPicker;
import ftcClientJava.Const;
import ftcClientJava.DataEngine;
import ftcClientJava.ItemChosenHandler;
import ftcClientJava.ToTreeData;
import ftcClientJava.UnderlineHighlightPainter;
import gc.common.structures.IntTuple;
import interfacing.AbstractCompletion;
import interfacing.SqlCompletionType;
import interfacing.SyntaxElement;
import interfacing.SyntaxElementSource;
import interfacing.SyntaxElementType;
import manipulations.CursorContext;
import manipulations.QueryPatching;
import util.Op;

public class QueryEditor extends JPanel implements SyntaxConstants {
	private static final long serialVersionUID = 1L;

	private RTextScrollPane scrollPane;
	public RSyntaxTextArea queryText;

	final DefaultHighlighter highlighter = new RSyntaxTextAreaHighlighter();
	final Highlighter.HighlightPainter syntaxErrorPainter = new UnderlineHighlightPainter(Color.red);
	final Highlighter.HighlightPainter semanticErrorPainter = new UnderlineHighlightPainter(Color.blue);

	private final SyntaxElementSource syntaxElementSource;
	
	public QueryEditor(SyntaxElementSource syntaxElementSource) {
		Check.notNull(syntaxElementSource);
	
		this.syntaxElementSource = syntaxElementSource;
	
		queryText = createTextArea();
		queryText.setSyntaxEditingStyle(SYNTAX_STYLE_NONE);
		queryText.setHighlighter(highlighter);

		scrollPane = new RTextScrollPane(queryText, true);
		Gutter gutter = scrollPane.getGutter();
		gutter.setBookmarkingEnabled(true);

		URL url = getClass().getResource("bookmark.png");
		gutter.setBookmarkIcon(new ImageIcon(url));

		add(scrollPane);
		ErrorStrip errorStrip = new ErrorStrip(queryText);

		add(errorStrip, BorderLayout.LINE_END);

		highlighter.setDrawsLayeredHighlights(false);

		setUpdateListener();
		setCompletionProvider();
		setTokenMaker();
	}

	private void setCompletionProvider() {
		new FtcAutoComplete(syntaxElementSource).install(queryText);
	}

	private void setUpdateListener() {
		queryText.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {

				updateQueryTextHighlighting();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateQueryTextHighlighting();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateQueryTextHighlighting();
			}
		});
	}

	private void setTokenMaker() {
		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		atmf.putMapping(Const.languageId, Const.tokenizerClassId);
		queryText.setSyntaxEditingStyle(Const.languageId);
	}

	public JMenu getMenu() {
		JMenu menu = new JMenu("Editor");
		JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem(new CodeFoldingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineHighlightAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineNumbersAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new AnimateBracketMatchingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new BookmarksAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new MarkOccurrencesAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new TabLinesAction());
		menu.add(cbItem);
		menu.addSeparator();

		JMenu themes = new JMenu("Themes");
		ButtonGroup bg = new ButtonGroup();
		addThemeItem("Default", "default.xml", bg, themes);
		addThemeItem("Default (System Selection)", "default-alt.xml", bg, themes);
		addThemeItem("Dark", "dark.xml", bg, themes);
		addThemeItem("Eclipse", "eclipse.xml", bg, themes);
		addThemeItem("IDEA", "idea.xml", bg, themes);
		addThemeItem("Visual Studio", "vs.xml", bg, themes);
		menu.add(themes);
		return menu;
	}

	private void addThemeItem(String name, String themeXml, ButtonGroup bg, JMenu menu) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(new ThemeAction(name, themeXml));
		bg.add(item);
		menu.add(item);
	}

	private RSyntaxTextArea createTextArea() {
		final RSyntaxTextArea queryText = new RSyntaxTextArea(25, 70);
		queryText.setTabSize(3);
		queryText.setCaretPosition(0);
		queryText.requestFocusInWindow();
		queryText.setMarkOccurrences(true);
		queryText.setCodeFoldingEnabled(true);
		queryText.setClearWhitespaceLinesEnabled(false);
		queryText.setAntiAliasingEnabled(true);
		queryText.setLineWrap(false);

		return queryText;
	}

	public void updateQueryTextHighlighting() {
		clearStyles();
		List<SyntaxElement> highlightings = syntaxElementSource.get(queryText.getText());
		if (highlightings.size() > 0) {
			for (SyntaxElement e : highlightings)
				underline(e);
			highlighter.paint(queryText.getGraphics());
		}
	}

	private void clearStyles() {
		highlighter.removeAllHighlights();
	}

	private void underline(SyntaxElement e) {
		if (e.type == SyntaxElementType.error)
			underline(e, syntaxErrorPainter);
		else if (e.hasSemanticError())
			underline(e, semanticErrorPainter);
	}

	private void underline(SyntaxElement e, HighlightPainter p) {
		try {
			highlighter.addHighlight(e.from, e.from + e.value.length() - 1, p);
		} catch (BadLocationException ex) {
		}
	}

	public void reSetCursor(QueryPatching patcher) {
		if (patcher.newCursorPosition.isPresent())
			queryText.setCaretPosition(patcher.newCursorPosition.get());
		else
			queryText.setCaretPosition(patcher.cursorPosition);
	}

	public String getText() {
		return queryText.getText();
	}

	public void autocomplete() {

		int cursorPos = queryText.getCaretPosition();
		String query = queryText.getText();
	//TODO	QueryPatching patcher = dataEngine.getPatcher(query, cursorPos);
		//TODO	chooseReplacement(patcher);
	}

	private IntTuple getCaretCoord() {

		int x, y;
		try {
			Rectangle c = queryText.getUI().modelToView(queryText, queryText.getCaretPosition());
			int offsetX = queryText.getLocationOnScreen().x;
			int offsetY = queryText.getLocationOnScreen().y;

			x = c.x + offsetX;
			y = c.y + offsetY;

		} catch (BadLocationException e) {
			x = -1;
			y = -1;
		}
		return IntTuple.instance(x, y);
	}

	private void hdlOnItemChosen(QueryPatching patcher, AbstractCompletion item) {
		queryText.setText(patcher.patch(item));
		reSetCursor(patcher);
		updateQueryTextHighlighting();
		selectNextReplacementTag(0);
	}

	private void selectNextReplacementTag(int startFrom) {
		int beginTagPos = queryText.getText().indexOf(interfacing.Const.replacementTagBegin, startFrom);
		int endTagPos = queryText.getText().indexOf(interfacing.Const.replacementTagEnd, beginTagPos);
		if (beginTagPos > 0 && endTagPos > 0) {
			endTagPos = endTagPos + interfacing.Const.replacementTagBegin.length();

			// queryText.setCaretPosition(beginTagPos); set by select to end of
			// selection
			queryText.select(beginTagPos, endTagPos);

		} else if (startFrom > 0)
			selectNextReplacementTag(0);
	}

	private void chooseReplacement(QueryPatching patcher) {

		CursorContext context = patcher.cursorContext;
		SqlCompletionType contextType = context.getModelElementType();

		Optional<String> selectionPrefix = getSelectionPrefix(context, contextType);
		DefaultMutableTreeNode content = getSelectionContent(patcher);
		if (content != null)
			CompletionPicker.show(patcher.getCompletions(), new ItemChosenHandler() {
				@Override
				public void onItemChosen(AbstractCompletion item) {
					hdlOnItemChosen(patcher, item);
				}
			}, getCaretCoord());

		// if (content != null)
		// TreeNamePicker.show(content, selectionPrefix, new ItemChosenHandler()
		// {
		// @Override
		// public void onItemChosen(AbstractCompletion item) {
		// hdlOnItemChosen(patcher, item);
		// }
		// });
	}

	private DefaultMutableTreeNode getSelectionContent(QueryPatching patcher) {
		return ToTreeData.fromCompletions("possible ways from here", patcher.getCompletions());
	}

	private Optional<String> getSelectionPrefix(CursorContext context, SqlCompletionType contextType) {
		return contextType == SqlCompletionType.table ? context.name : context.otherName;
	}

	private class BookmarksAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public BookmarksAction() {
			putValue(NAME, "Bookmarks");
		}

		public void actionPerformed(ActionEvent e) {
			scrollPane.setIconRowHeaderEnabled(!scrollPane.isIconRowHeaderEnabled());
		}

	}

	private class CodeFoldingAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public CodeFoldingAction() {
			putValue(NAME, "Code Folding");
		}

		public void actionPerformed(ActionEvent e) {
			queryText.setCodeFoldingEnabled(!queryText.isCodeFoldingEnabled());
		}

	}

	private class MarkOccurrencesAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public MarkOccurrencesAction() {
			putValue(NAME, "Mark Occurrences");
		}

		public void actionPerformed(ActionEvent e) {
			queryText.setMarkOccurrences(!queryText.getMarkOccurrences());
		}

	}

	private class TabLinesAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private boolean selected;

		public TabLinesAction() {
			putValue(NAME, "Tab Lines");
		}

		public void actionPerformed(ActionEvent e) {
			selected = !selected;
			queryText.setPaintTabLines(selected);
		}

	}

	private class ThemeAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private String xml;

		public ThemeAction(String name, String xml) {
			putValue(NAME, name);
			this.xml = xml;
		}

		public void actionPerformed(ActionEvent e) {
			InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + xml);
			try {
				Theme theme = Theme.load(in);
				theme.apply(queryText);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}

	private class ViewLineHighlightAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public ViewLineHighlightAction() {
			putValue(NAME, "Current Line Highlight");
		}

		public void actionPerformed(ActionEvent e) {
			queryText.setHighlightCurrentLine(!queryText.getHighlightCurrentLine());
		}

	}

	private class ViewLineNumbersAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public ViewLineNumbersAction() {
			putValue(NAME, "Line Numbers");
		}

		public void actionPerformed(ActionEvent e) {
			scrollPane.setLineNumbersEnabled(!scrollPane.getLineNumbersEnabled());
		}

	}

	private class AnimateBracketMatchingAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public AnimateBracketMatchingAction() {
			putValue(NAME, "Animate Bracket Matching");
		}

		public void actionPerformed(ActionEvent e) {
			queryText.setAnimateBracketMatching(!queryText.getAnimateBracketMatching());
		}

	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				QueryEditor e = new QueryEditor(null);
				e.setSize(600, 400);
				e.setVisible(true);
			}
		});
	}

	public Document getDocument() {
		return queryText.getDocument();
	}

}
