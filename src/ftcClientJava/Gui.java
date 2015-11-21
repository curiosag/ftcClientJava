package ftcClientJava;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;

import com.google.common.base.Optional;

import cg.common.check.Check;
import cg.common.interfaces.AbstractKeyListener;
import interfacing.AbstractCompletion;
import interfacing.SqlCompletionType;
import interfacing.SyntaxElement;
import interfacing.SyntaxElementSource;
import interfacing.SyntaxElementType;
import manipulations.CursorContext;
import manipulations.QueryHandler;
import manipulations.QueryPatching;
import util.Op;

import java.awt.*; //for layout managers and more
import java.awt.event.*; //for action events
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class Gui extends JPanel implements ActionListener, Observer {
	private static final long serialVersionUID = 1L;
	private static final String textFieldString = "JTextField";
	private static final boolean replaceAttributes = true;

	private JLabel actionLabel;
	private JEditorPane queryText;
	private final StyleContext queryTextStyleContext = new StyleContext();
	private final DefaultStyledDocument queryTextDoc = new DefaultStyledDocument(queryTextStyleContext);

	private JEditorPane opResult;

	private JTextField textFieldErr;
	private JTextField textFieldInf;

	final DefaultHighlighter highlighter = new DefaultHighlighter();
	final Highlighter.HighlightPainter syntaxErrorPainter = new UnderlineHighlightPainter(Color.red);
	final Highlighter.HighlightPainter semanticErrorPainter = new UnderlineHighlightPainter(Color.blue);
	
	KeyboardActions keyActions = new KeyboardActions();

	private final ActionListener controller;
	private final SyntaxElementSource higlightingInfo;
	private final DataEngine dataEngine;

	HashMap<SyntaxElementType, Style> styleMapping = new HashMap<SyntaxElementType, Style>();

	public Gui(ActionListener controller, DataEngine dataEngine, SyntaxElementSource higlightingInfo) {
		buildGui();
		this.dataEngine = dataEngine;
		this.higlightingInfo = higlightingInfo;
		this.controller = controller;
		highlighter.setDrawsLayeredHighlights(false);
		addKeyboardActions();
		defineColorMapping();
	}

	private static final Color colKeyword = new Color(127, 0, 109);
	private static final Color colNames = new Color(114, 126, 202);

	private void addStyleMapping(SyntaxElementType t, Color c) {
		Style s = queryTextStyleContext.addStyle(t.name(), null);
		s.addAttribute(StyleConstants.Foreground, c);
		// maybe this will be useful
		// private final Style defaultStyle =
		// StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		if (t == SyntaxElementType.error) {
			StyleConstants.setItalic(s, true);
		}

		if (Op.in(t, SyntaxElementType.sql_keyword, SyntaxElementType.ft_keyword))
			StyleConstants.setBold(s, true);

		styleMapping.put(t, s);
	}

	private void defineColorMapping() {
		addStyleMapping(SyntaxElementType.sql_keyword, colKeyword);
		addStyleMapping(SyntaxElementType.ft_keyword, Color.gray);
		addStyleMapping(SyntaxElementType.operator, Color.black);
		addStyleMapping(SyntaxElementType.tableName, colNames);
		addStyleMapping(SyntaxElementType.columnName, colNames);
		addStyleMapping(SyntaxElementType.viewName, colNames);
		addStyleMapping(SyntaxElementType.alias, colNames);
		addStyleMapping(SyntaxElementType.stringLiteral, Color.blue);
		addStyleMapping(SyntaxElementType.numericLiteral, Color.blue);
		addStyleMapping(SyntaxElementType.identifier, Color.blue);
		addStyleMapping(SyntaxElementType.error, Color.gray);
	}

	private AbstractAction getAction(String actionId) {
		return new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.actionPerformed(new ActionEvent(e.getSource(), e.getID(), actionId));
			}
		};
	}

	private void addKeyboardActions() {
		keyActions.add(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK, getAction(""));
		keyActions.add(KeyEvent.VK_F1, 0, getAction(Const.prev));
		keyActions.add(KeyEvent.VK_F2, 0, getAction(Const.next));
		keyActions.add(KeyEvent.VK_F3, 0, getAction(Const.listTables));
		keyActions.add(KeyEvent.VK_F4, 0, getAction(Const.preview));
		keyActions.add(KeyEvent.VK_F5, 0, getAction(Const.execSql));
		keyActions.add(KeyEvent.VK_F11, 0, getAction(Const.oh));
		keyActions.add(KeyEvent.VK_F12, 0, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				autocomplete();
			}
		});
	}

	private void autocomplete() {
		int cursorPos = queryText.getCaretPosition();
		String query = queryText.getText();
		QueryPatching patcher = dataEngine.getPatcher(query, cursorPos);
		chooseReplacement(patcher);
	}

	public void addQueryTextKeyListener(AbstractKeyListener k) {
		queryText.addKeyListener(k);
	}

	private ActionListener getController() {
		Check.notNull(controller);
		return controller;
	}

	public Document queryTextDocument() {
		return queryText.getDocument();
	}

	public Document opResultDocument() {
		return opResult.getDocument();
	}

	public Observer createErrorTextObserver() {
		return Observism.createObserver(textFieldErr);
	}

	public Observer createInfoTextObserver() {
		return Observism.createObserver(textFieldInf);
	}

	public Observer createOpResultObserver() {
		return Observism.createObserver(opResult);
	}

	public Observer createQueryTextObserver() {
		return Observism.createObserver(queryText);
	}

	private Style getStyle(SyntaxElementType type) {
		Style result = styleMapping.get(type);
		Check.notNull(result);

		return result;
	}

	private void buildGui() {
		setLayout(new BorderLayout());

		JPanel textControlsPane = createTextFieldArea();
		JScrollPane areaScrollPane = createQueryText();
		JPanel rightPane = createResultDisplay();
		JPanel buttonPane = createButtonArea();

		JPanel leftPane = new JPanel(new BorderLayout());

		leftPane.add(areaScrollPane, BorderLayout.PAGE_START);
		leftPane.add(buttonPane, BorderLayout.CENTER);
		leftPane.add(textControlsPane, BorderLayout.PAGE_END);

		add(leftPane, BorderLayout.LINE_START);
		add(rightPane, BorderLayout.LINE_END);
	}

	private JEditorPane createEditorPane() {
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(true);

		return editorPane;
	}

	public static void runDeferred(Runnable r) {
		SwingUtilities.invokeLater(r);
	}

	static Gui createAndShowGUI(DataEngine dataEngine, ActionListener controller, SyntaxElementSource s) {
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		JFrame frame = new JFrame("UI");
		frame.setSize(1800, 1500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Gui result = new Gui(controller, dataEngine, s);
		frame.add(result);

		frame.pack();
		frame.setVisible(true);

		return result;
	}

	private void chooseReplacement(QueryPatching patcher) {

		CursorContext context = patcher.cursorContext;
		SqlCompletionType contextType = context.getModelElementType();

		Optional<String> selectionPrefix = getSelectionPrefix(context, contextType);
		DefaultMutableTreeNode content = getSelectionContent(patcher);
		if (content != null)
			TreeNamePicker.show(content, selectionPrefix, new ItemChosenHandler() {
				@Override
				public void onItemChosen(AbstractCompletion item) {
					hdlOnItemChosen(patcher, item);
				}
			});
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
			queryText.setCaretPosition(endTagPos);
			queryText.select(beginTagPos, endTagPos);
		} else if (startFrom > 0)
			selectNextReplacementTag(0);
	}

	private void reSetCursor(QueryPatching patcher) {
		if (patcher.newCursorPosition.isPresent())
			queryText.setCaretPosition(patcher.newCursorPosition.get());
		else
			queryText.setCaretPosition(patcher.cursorPosition);
	}

	private DefaultMutableTreeNode getSelectionContent(QueryPatching patcher) {
		return ToTreeData.fromCompletions("possible ways from here", patcher.getCompletions());
	}

	private Optional<String> getSelectionPrefix(CursorContext context, SqlCompletionType contextType) {
		return contextType == SqlCompletionType.table ? context.name : context.otherName;
	}

	private JPanel createButtonArea() {
		JButton buttonExecSql = createButton(Const.execSql);
		JButton buttonListTables = createButton(Const.listTables);
		JButton buttonPreview = createButton(Const.preview);
		JButton buttonOh = createButton(Const.oh);
		JButton buttonPrevCmd = createButton(Const.prev);
		JButton buttonNextCmd = createButton(Const.next);

		JPanel buttonPane = new JPanel(new GridLayout(1, 6));
		buttonPane.add(buttonPrevCmd);
		buttonPane.add(buttonNextCmd);
		buttonPane.add(buttonListTables);
		buttonPane.add(buttonOh);
		buttonPane.add(buttonPreview);
		buttonPane.add(buttonExecSql);
		return buttonPane;
	}

	private JPanel createTextFieldArea() {
		textFieldErr = createTextField();
		textFieldInf = createTextField();

		JLabel textFieldLabel = setLabels(textFieldErr, "err: ");
		JLabel ftfLabel = setLabels(textFieldInf, "inf: ");

		actionLabel = new JLabel("...");
		actionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		JPanel textControlsPane = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		textControlsPane.setLayout(gridbag);

		JLabel[] labels = { textFieldLabel, ftfLabel };
		JTextField[] textFields = { textFieldErr, textFieldInf };
		addLabelTextRows(labels, textFields, gridbag, textControlsPane);

		c.gridwidth = GridBagConstraints.REMAINDER; // last
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		textControlsPane.add(actionLabel, c);
		textControlsPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Text Fields"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		return textControlsPane;
	}

	private JButton createButton(String actionCommand) {
		JButton buttonExecSql = new JButton(actionCommand);
		buttonExecSql.addActionListener(this);
		buttonExecSql.setActionCommand(actionCommand);
		return buttonExecSql;
	}

	private JPanel createResultDisplay() {
		opResult = createEditorPane();
		JScrollPane opResultScrollPane = new JScrollPane(opResult);
		opResultScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		opResultScrollPane.setPreferredSize(new Dimension(250, 250));
		opResultScrollPane.setMinimumSize(new Dimension(250, 250));

		JPanel rightPane = new JPanel(new BorderLayout());
		rightPane.add(opResultScrollPane, BorderLayout.CENTER);
		rightPane.add(new JTextField(60), BorderLayout.PAGE_END);
		rightPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("result"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		rightPane.setSize(600, 600);
		return rightPane;
	}

	private void updateQueryTextHighlighting() {
		clearStyles();
		for (SyntaxElement e : higlightingInfo.get(queryText.getText()))
			style(e);
		highlighter.paint(queryText.getGraphics());
	}

	private void clearStyles() {
		queryTextDoc.setCharacterAttributes(0, queryTextDoc.getLength(), getStyle(SyntaxElementType.operator),
				replaceAttributes);
		highlighter.removeAllHighlights();
	}

	private void style(SyntaxElement e) {
		if (!Op.in(e.type, SyntaxElementType.error, SyntaxElementType.errorInfo, SyntaxElementType.unknown))
			queryTextDoc.setCharacterAttributes(e.from, e.value.length(), getStyle(e.type), replaceAttributes);
		if (e.type == SyntaxElementType.error)
			underline(e, syntaxErrorPainter);
		else if (e.hasSemanticError())
			underline(e, semanticErrorPainter);
	}

	private void underline(SyntaxElement e, HighlightPainter p) {
		try {
			highlighter.addHighlight(e.from, e.from + e.value.length(), p);
		} catch (BadLocationException ex) {
		}
	}

	private JScrollPane createQueryText() {
		queryText = new JTextPane(queryTextDoc);
		queryText.setHighlighter(highlighter);
		queryText.setFont(new Font("Monospace", Font.PLAIN, 13));
		queryText.addKeyListener(new AbstractKeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					selectNextReplacementTag(queryText.getCaretPosition());
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (!Op.in(e.getKeyCode(), KeyEvent.VK_TAB, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
						KeyEvent.VK_DOWN))
					updateQueryTextHighlighting();
			}
		});

		JScrollPane areaScrollPane = new JScrollPane(queryText);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		areaScrollPane.setPreferredSize(new Dimension(250, 250));
		areaScrollPane
				.setBorder(
						BorderFactory
								.createCompoundBorder(
										BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("query"),
												BorderFactory.createEmptyBorder(5, 5, 5, 5)),
								areaScrollPane.getBorder()));
		return areaScrollPane;
	}

	private JLabel setLabels(JTextField textField1, String label) {
		JLabel textFieldLabel = new JLabel(label);
		textFieldLabel.setLabelFor(textField1);
		return textFieldLabel;
	}

	private JTextField createTextField() {
		JTextField textField1 = new JTextField(35);
		textField1.setActionCommand(textFieldString);
		textField1.addActionListener(this);
		return textField1;
	}

	private void addLabelTextRows(JLabel[] labels, JTextField[] textFields, GridBagLayout gridbag,
			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
			c.fill = GridBagConstraints.NONE; // reset to default
			c.weightx = 0.0; // reset to default
			container.add(labels[i], c);

			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(textFields[i], c);
		}
	}

	private void onStructureChanged() {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getController().actionPerformed(e);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof QueryHandler)
			runDeferred(() -> onStructureChanged());
	}

}
