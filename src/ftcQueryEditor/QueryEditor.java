package ftcQueryEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import cg.common.check.Check;
import cg.common.swing.UnderlineHighlightPainter;
import ftcClientCodeCompletionExperiments.FtcAutoComplete;
import ftcClientJava.Const;
import gc.common.structures.IntTuple;
import interfaces.CompletionsSource;
import interfaces.SyntaxElementSource;
import interfaces.SyntaxElement;
import interfaces.SyntaxElementType;
import manipulations.QueryPatching;
import structures.ClientSettings;

public class QueryEditor extends JRootPane implements SyntaxConstants {

	private static final long serialVersionUID = 1L;

	private RTextScrollPane scrollPane;
	public RSyntaxTextArea queryText;

	private final SyntaxElementSource syntaxElementSource;
	private final CompletionsSource completionsSource;
	private final ClientSettings clientSettings;
	
	public QueryEditor(SyntaxElementSource syntaxElementSource, CompletionsSource completionsSource, ClientSettings clientSettings) {
		Check.notNull(syntaxElementSource);

		this.syntaxElementSource = syntaxElementSource;
		this.completionsSource = completionsSource;
		this.clientSettings = clientSettings;

		queryText = createTextArea();
		queryText.setSyntaxEditingStyle(SYNTAX_STYLE_NONE);
		queryText.addParser(new GftParser(syntaxElementSource));
		queryText.setMarkOccurrences(false);
		queryText.setHighlightCurrentLine(clientSettings.highlightCurrentLine);

		queryText.setParserDelay(700);

		scrollPane = new RTextScrollPane(queryText, false);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		Gutter gutter = scrollPane.getGutter();
		gutter.setBookmarkingEnabled(true);
		scrollPane.setIconRowHeaderEnabled(true);
		scrollPane.setLineNumbersEnabled(clientSettings.lineNumbersEnabled);

		setTheme(clientSettings.editorThemeXml);
		
		URL url = getClass().getResource("bookmark.png");
		gutter.setBookmarkIcon(new ImageIcon(url));

		getContentPane().add(scrollPane);
		ErrorStrip errorStrip = new ErrorStrip(queryText);

		getContentPane().add(errorStrip, BorderLayout.LINE_END);

		setCompletionProvider();
		setTokenMaker();
	}

	private void setCompletionProvider() {
		new FtcAutoComplete(syntaxElementSource, completionsSource).install(queryText);
	}

	private void setTokenMaker() {
		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		atmf.putMapping(Const.languageId, Const.tokenizerClassId);
		queryText.setSyntaxEditingStyle(Const.languageId);
	}

	public JMenu getMenu() {
		JMenu menu = new JMenu("Editor");
		menu.setMnemonic(KeyEvent.VK_E);
		JCheckBoxMenuItem cbItem;
		cbItem = new JCheckBoxMenuItem(new ViewLineHighlightAction());
		cbItem.setSelected(clientSettings.highlightCurrentLine);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineNumbersAction());
		cbItem.setSelected(clientSettings.lineNumbersEnabled);
		menu.add(cbItem);
		
		// cbItem = new JCheckBoxMenuItem(new BookmarksAction());
		// causes rather strange behavior
		// cbItem = new JCheckBoxMenuItem(new MarkOccurrencesAction());
		// JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem(new
		// CodeFoldingAction());
		// cbItem = new JCheckBoxMenuItem(new AnimateBracketMatchingAction());

		cbItem = new JCheckBoxMenuItem(new TabLinesAction());
		cbItem.setSelected(clientSettings.tabLines);
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
		item.setSelected(themeXml.equals(clientSettings.editorThemeXml));
		bg.add(item);
		menu.add(item);
	}

	private RSyntaxTextArea createTextArea() {
		final RSyntaxTextArea queryText = new RSyntaxTextArea(25, 70);
		queryText.setTabSize(3);
		queryText.setCaretPosition(0);
		queryText.requestFocusInWindow();
		queryText.setMarkOccurrences(false);
		queryText.setCodeFoldingEnabled(true);
		queryText.setClearWhitespaceLinesEnabled(false);
		queryText.setAntiAliasingEnabled(true);
		queryText.setLineWrap(false);

		return queryText;
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

	private class CodeFoldingAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public CodeFoldingAction() {
			putValue(NAME, "Code Folding");
		}

		public void actionPerformed(ActionEvent e) {
			queryText.setCodeFoldingEnabled(!queryText.isCodeFoldingEnabled());
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
			clientSettings.tabLines = selected;
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
			setTheme(xml);
		}

	}

	private void setTheme(String xml) {
		InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + xml);
		try {
			Theme theme = Theme.load(in);
			theme.apply(queryText);
			clientSettings.editorThemeXml = xml;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private class ViewLineHighlightAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public ViewLineHighlightAction() {
			putValue(NAME, "Current Line Highlight");
		}

		public void actionPerformed(ActionEvent e) {
			queryText.setHighlightCurrentLine(!queryText.getHighlightCurrentLine());
			clientSettings.highlightCurrentLine = queryText.getHighlightCurrentLine();
		}

	}

	private class ViewLineNumbersAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public ViewLineNumbersAction() {
			putValue(NAME, "Line Numbers");
		}

		public void actionPerformed(ActionEvent e) {
			scrollPane.setLineNumbersEnabled(!scrollPane.getLineNumbersEnabled());
			clientSettings.lineNumbersEnabled = scrollPane.getLineNumbersEnabled();
		}

	}
	
	public Document getDocument() {
		return queryText.getDocument();
	}

}
