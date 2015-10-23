import javax.swing.*;

import cg.common.core.Logging;
import cg.common.io.FileStringStorage;
import cg.common.misc.CmdHistory;
import fusiontables.FusionTablesConnector;
import manipulations.QueryHandler;

import java.awt.*; //for layout managers and more
import java.awt.event.*; //for action events
import java.net.MalformedURLException;
import java.io.IOException;

public class Gui extends JPanel implements ActionListener, KeyListener {
	private static final long serialVersionUID = 1L;
	private static final String textFieldString = "JTextField";
	private static final String execSql = "execSql";
	private static final String listTables = "listTables";
	private static final String preview = "preview";
	private static final String next = ">";
	private static final String prev = "<";
	private static final String oh = ":-O";
	private static final String historyStore = "./commandHistory.txt";
	private JLabel actionLabel;
	JTextArea queryText;
	JEditorPane opResult;
	JTextField textFieldErr;
	JTextField textFieldInf;

	private Logging logging = new Logging() {

		@Override
		public void Info(String info) {
			runDeferred(() -> logInfo(info));
		}

		@Override
		public void Error(String error) {
			runDeferred(() -> logError(error));
		}
	};

	private CmdHistory history = new CmdHistory(new FileStringStorage(historyStore));
	private QueryHandler queryExecutor = new QueryHandler(logging, new FusionTablesConnector(logging));

	public Gui() {
		buildGui();
		displayUsageInfo();
	}

	private void logInfo(String msg) {
		textFieldInf.setText(msg);
	}

	private void logError(String msg) {
		textFieldErr.setText(msg);
	}

	private void ohCmd() {
		setResultText("nothing to do right now");
	}

	private void nextCmd() {
		history.next((x) -> setQueryText(x));
	}

	private void prevCmd() {
		history.prev((x) -> setQueryText(x));
	}

	private String getQueryText() {
		return queryText.getText();
	};

	private void setQueryText(String s) {
		queryText.setText(s);
	};

	private void setResultText(String s) {
		opResult.setText(s);
	}

	private void listTables() {
		setResultText(queryExecutor.getTableInfo());
	}

	private void preview() {
		setResultText(queryExecutor.previewExecutedSql(getQueryText()));
	}

	private void execSql() {
		String sql = getQueryText();
		history.add(sql);
		setResultText(queryExecutor.getQueryResult(sql));
	}

	private void displayUsageInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("USAGE \n\n");
		sb.append("F1 backwards in sql history \n");
		sb.append("F2 forward in sql history \n");
		sb.append("F3 list tables \n");
		sb.append("F4 preview sql \n");
		sb.append("F5 execute sql \n");
		sb.append("F12 the oh command \n");
		setResultText(sb.toString());
	}

	private void echoAction(String action) {
		actionLabel.setText(action);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String prefix = "Fetching ";
		if (textFieldString.equals(e.getActionCommand())) {
			JTextField source = (JTextField) e.getSource();
			echoAction(prefix + source.getText() + "\"");
		} else if (execSql.equals(e.getActionCommand())) {
			echoAction(prefix + " query result");
			runDeferred(() -> execSql());
		} else if (listTables.equals(e.getActionCommand())) {
			echoAction(prefix + " table list");
			runDeferred(() -> listTables());
		} else if (preview.equals(e.getActionCommand())) {
			echoAction(prefix + "preview\"");
			preview();
		} else if (prev.equals(e.getActionCommand())) {
			echoAction("last command");
			prevCmd();
		} else if (next.equals(e.getActionCommand())) {
			echoAction("next command");
			nextCmd();
		} else if (oh.equals(e.getActionCommand())) {
			echoAction("oh");
			ohCmd();
		}
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

	@SuppressWarnings("unused")
	private void loadUrlContent(JEditorPane editorPane) {
		java.net.URL helpURL = null;
		try {
			helpURL = new java.net.URL("https://en.wikipedia.org/wiki/Planned_obsolescence#See_also");
			helpURL = null;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		if (helpURL != null) {
			try {
				editorPane.setPage(helpURL);
			} catch (IOException e) {
				System.err.println("Attempted to read a bad URL: " + helpURL);
			}
		} else {
			System.err.println("Couldn't find file: TextSampleDemoHelp.html");
		}
	}

	public static void runDeferred(Runnable r) {
		SwingUtilities.invokeLater(r);
	}

	static Gui createAndShowGUI() {
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		JFrame frame = new JFrame("UI");
		frame.setSize(1600, 1000);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Gui result = new Gui();
		frame.add(result);

		frame.pack();
		frame.setVisible(true);

		return result;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {

	}

	@Override
	public void keyReleased(KeyEvent e) {
		int keycode = e.getKeyCode();
		switch (keycode) {

		case KeyEvent.VK_F1:
			echoAction("previous sql");
			prevCmd();
			break;

		case KeyEvent.VK_F2:
			echoAction("next sql");
			nextCmd();
			break;

		case KeyEvent.VK_F3:
			echoAction("list tables");
		    listTables();
			break;

		case KeyEvent.VK_F4:
			echoAction("sql preview");
			preview();
			break;

		case KeyEvent.VK_F5:
			echoAction("running sql");
			execSql();
			break;

		case KeyEvent.VK_F12:
			echoAction("surprising things happen");
			ohCmd();
			break;

		default:
			break;
		}

	}

	private JPanel createButtonArea() {
		JButton buttonExecSql = createButton(execSql);
		JButton buttonListTables = createButton(listTables);
		JButton buttonPreview = createButton(preview);
		JButton buttonOh = createButton(oh);
		JButton buttonPrevCmd = createButton(prev);
		JButton buttonNextCmd = createButton(next);

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
		opResult.addKeyListener(this);
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

	private JScrollPane createQueryText() {
		queryText = new JTextArea();
		queryText.addKeyListener(this);
		queryText.setFont(new Font("SansSerif", Font.PLAIN, 12));
		queryText.setLineWrap(true);
		queryText.setWrapStyleWord(true);
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

}
