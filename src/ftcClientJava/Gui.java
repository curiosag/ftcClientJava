package ftcClientJava;

import javax.swing.*;
import javax.swing.text.Document;
import cg.common.check.Check;
import cg.common.interfaces.AbstractKeyListener;
import ftcQueryEditor.ExternalRSTATokenProvider;
import ftcQueryEditor.Global;
import ftcQueryEditor.QueryEditor;
import interfacing.SyntaxElement;
import interfacing.SyntaxElementSource;
import manipulations.QueryHandler;
import java.awt.*; 
import java.awt.event.*;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class Gui extends JFrame implements ActionListener, Observer {
	private static final long serialVersionUID = 1L;
	private static final String textFieldString = "JTextField";

	private JLabel actionLabel;
	private QueryEditor queryEditor;

	private JEditorPane opResult;

	private JTextField textFieldErr;
	private JTextField textFieldInf;
	
	KeyboardActions keyActions = new KeyboardActions();

	private final ActionListener controller;
	private final SyntaxElementSource higlightingInfo;
	private final DataEngine dataEngine;

	public Gui(ActionListener controller, DataEngine dataEngine, SyntaxElementSource higlightingInfo) {
		
		this.dataEngine = dataEngine;
		this.higlightingInfo = higlightingInfo;
		this.controller = controller;
		
		Global.externalTokenProvider = new ExternalRSTATokenProvider() {
			
			@Override
			public List<SyntaxElement> getTokens(String query) {
				return higlightingInfo.get(query);
			}
		};
		
		buildGui();
		addKeyboardActions();

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
				queryEditor.autocomplete();
			}
		});	
	}

	public void addQueryTextKeyListener(AbstractKeyListener k) {
		queryEditor.addKeyListener(k);
	}

	private ActionListener getController() {
		Check.notNull(controller);
		return controller;
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
		return Observism.createObserver(queryEditor.queryText);
	}

	private void buildGui() {
		setLayout(new BorderLayout());

		JPanel textControlsPane = createTextFieldArea();

		JPanel rightPane = createResultDisplay();
		JPanel buttonPane = createButtonArea();

		JPanel leftPane = new JPanel(new BorderLayout());
		queryEditor = new QueryEditor(higlightingInfo);
		leftPane.add(queryEditor, BorderLayout.PAGE_START);
		leftPane.add(buttonPane, BorderLayout.CENTER);
		leftPane.add(textControlsPane, BorderLayout.PAGE_END);

		add(leftPane, BorderLayout.LINE_START);
		add(rightPane, BorderLayout.LINE_END);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(queryEditor.getMenu());
		
		setJMenuBar(menuBar);
		menuBar.setVisible(true);
		
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

		Gui result = new Gui(controller, dataEngine, s);
		result.setSize(1800, 1500);
		result.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		result.pack();
		result.setVisible(true);

		return result;
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

	public Document queryTextDocument() {
		return queryEditor.getDocument();
	}

}
