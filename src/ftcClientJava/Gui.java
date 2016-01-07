package ftcClientJava;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import cg.common.check.Check;
import cg.common.core.AbstractLogger;
import cg.common.core.DelegatingLogger;
import cg.common.interfaces.AbstractKeyListener;
import cg.common.interfaces.OnValueChangedEvent;
import cg.common.misc.SimpleObservable;
import cg.common.swing.WindowClosingListener;
import ftcQueryEditor.QueryEditor;
import interfaces.SyntaxElementSource;
import interfaces.CompletionsSource;
import interfaces.SettingsListener;
import manipulations.QueryHandler;
import net.miginfocom.swing.MigLayout;
import structures.ClientSettings;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

public class Gui extends JFrame implements ActionListener, Observer {
	private static final long serialVersionUID = 1L;

	private QueryEditor queryEditor;

	private JEditorPane opResult;

	private JTextField textFieldClientId;
	private JPasswordField textFieldClientSecret;
	private JSpinner fieldDefaultLimit;

	JSplitPane splitPaneH;
	JSplitPane splitPaneV;

	private JTable dataTable = null;

	KeyboardActions keyActions = new KeyboardActions();

	private final ActionListener controller;
	private final SyntaxElementSource syntaxElements;
	private final CompletionsSource completionsSource;
	private final ClientSettings clientSettings;

	private final AbstractLogger logger;

	public Gui(ActionListener controller, SyntaxElementSource syntaxElements, CompletionsSource completionsSource,
			ClientSettings clientSettings, AbstractLogger logger) {

		this.syntaxElements = syntaxElements;
		this.completionsSource = completionsSource;
		this.controller = controller;
		this.clientSettings = clientSettings;
		this.logger = logger;

		buildGui();
		addKeyboardActions();
		this.addWindowListener(new WindowClosingListener() {

			@Override
			public void windowClosing(WindowEvent e) {
				writeClientSettings();
			}
		});

	}

	private void writeClientSettings() {
		clientSettings.clientId = textFieldClientId.getText();
		clientSettings.clientSecret = String.valueOf(textFieldClientSecret.getPassword());
		clientSettings.dividerLocationH = splitPaneH.getDividerLocation();
		clientSettings.dividerLocationV = splitPaneV.getDividerLocation();
		clientSettings.x = getX();
		clientSettings.y = getY();
		clientSettings.width = getWidth();
		clientSettings.height = getHeight();
		clientSettings.defaultQueryLimit = getQueryLimit();
		clientSettings.write();
	}

	private int getQueryLimit() {
		return ((SpinnerNumberModel) fieldDefaultLimit.getModel()).getNumber().intValue();
	};

	private AbstractAction getAction(String name, String actionId) {
		return new AbstractAction(name) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.actionPerformed(new ActionEvent(e.getSource(), e.getID(), actionId));
			}
		};
	}

	private void addKeyboardActions() {
		keyActions.add(KeyEvent.VK_LEFT, KeyEvent.ALT_MASK, getAction("", Const.prev));
		keyActions.add(KeyEvent.VK_RIGHT, KeyEvent.ALT_MASK, getAction("", Const.next));

		keyActions.add(KeyEvent.VK_F3, 0, getAction("", Const.listTables));
		keyActions.add(KeyEvent.VK_F4, 0, getAction("", Const.preview));
		keyActions.add(KeyEvent.VK_F5, 0, getAction("", Const.execSql));
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
	
	public Observer createClientIdObserver() {
		return Observism.createObserver(textFieldClientId);
	}

	public Observer createClientSecretObserver() {
		return Observism.createObserver(textFieldClientSecret);
	}
	
	public void addClientIdChangedListener(OnValueChangedEvent e)
	{
		Observism.addValueChangedListener(textFieldClientId, e);
	}
	
	public void addClientSecretChangedListener(OnValueChangedEvent e)
	{
		Observism.addValueChangedListener(textFieldClientSecret, e);
	}
	
	public Observer createOpResultObserver() {
		return Observism.createObserver(opResult);
	}

	public Observer createQueryObserver() {
		return Observism.createObserver(queryEditor.queryText);
	}

	public Observer createResultDataObserver() {
		return new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				Check.isTrue(o instanceof SimpleObservable);
				SimpleObservable<?> s = (SimpleObservable<?>) o;
				Check.isTrue(s.getValue() instanceof TableModel);
				TableModel model = (TableModel) s.getValue();
				dataTable.setModel(model);
			}
		};
	}

	private void buildGui() {
		setLayout(new BorderLayout());

		JScrollPane resultDataArea = createTableDisplay();
		JPanel buttonArea = createButtonArea();
		queryEditor = new QueryEditor(syntaxElements, completionsSource, clientSettings);
		JPanel resultextArea = createResultDisplay();
		JPanel textControlsPane = createSettingsArea();

		createSplitLayout(resultDataArea, buttonArea, resultextArea, textControlsPane);

		setMenu();

	}

	private void createSplitLayout(JScrollPane resultDataArea, JPanel buttonArea, JPanel resultextArea,
			JPanel textControlsPane) {
		splitPaneV = createSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPaneV.setDividerLocation(clientSettings.dividerLocationV);

		JPanel frame0L = new JPanel();
		frame0L.setLayout(new BoxLayout(frame0L, BoxLayout.Y_AXIS));

		JPanel frame0R = new JPanel();
		frame0R.setLayout(new BoxLayout(frame0R, BoxLayout.Y_AXIS));

		splitPaneH = createSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPaneH.setDividerLocation(clientSettings.dividerLocationH);

		frame0R.add(buttonArea);
		frame0R.add(splitPaneH);

		buttonArea.setAlignmentX(Component.LEFT_ALIGNMENT);
		splitPaneH.setAlignmentX(Component.LEFT_ALIGNMENT);

		splitPaneV.add(frame0L, JSplitPane.LEFT);
		splitPaneV.add(frame0R, JSplitPane.RIGHT);

		frame0L.add(textControlsPane);
		frame0L.add(resultextArea);
		textControlsPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		resultextArea.setAlignmentX(Component.LEFT_ALIGNMENT);

		splitPaneH.setTopComponent(queryEditor);
		splitPaneH.setBottomComponent(resultDataArea);

		add(splitPaneV);
	}

	private JSplitPane createSplitPane(int orientation) {
		JSplitPane frame0 = new JSplitPane();
		frame0.setOrientation(orientation);
		frame0.setDividerSize(2);
		return frame0;
	}

	private void setMenu() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);

		menu.add(createMenuItem(KeyEvent.VK_O, getAction("Open", Const.fileOpen)));
		menu.add(createMenuItem(KeyEvent.VK_S, getAction("Save", Const.fileSave)));
		menu.add(createMenuItem(KeyEvent.VK_E, createExportCsvAction("Export")));

		menuBar.add(menu);
		menuBar.add(queryEditor.getMenu());
		setJMenuBar(menuBar);
		menuBar.setVisible(true);
	}

	private Action createExportCsvAction(String name) {
		return new AbstractAction(name) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (tablePopulated())
					controller.actionPerformed(new ActionEvent(dataTable.getModel(), e.getID(), Const.exportCsv));
				else
					logger.Info("no data to export");
			}

			private boolean tablePopulated() {
				return dataTable != null && dataTable.getModel() != null && dataTable.getModel().getRowCount() > 0;
			}
		};
	}

	private JMenuItem createMenuItem(int keyEvent, Action action) {
		JMenuItem result = new JMenuItem(action);
		result.setAccelerator(KeyStroke.getKeyStroke(keyEvent, ActionEvent.CTRL_MASK));
		result.setMnemonic(keyEvent);
		return result;
	}

	private JScrollPane createTableDisplay() {
		dataTable = new JTable();
		JScrollPane scrollPane = new JScrollPane(dataTable);
		dataTable.setFillsViewportHeight(true);
		return scrollPane;
	}

	private JEditorPane createEditorPane() {
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(true);

		return editorPane;
	}

	public static void runDeferred(Runnable r) {
		SwingUtilities.invokeLater(r);
	}

	private JPanel createButtonArea() {

		JButton buttonExecSql = createButton(Const.execSql, "control_play_blue.png", "execute command (F5)");
		JButton buttonListTables = createButton(Const.listTables, "table.png", "list tables (F3)");
		JButton buttonPreview = createButton(Const.preview, "control_play.png", "view preprocessed query (F4)");
		JButton buttonPrevCmd = createButton(Const.prev, "control_rewind_blue.png", "previous command (Alt+left)");
		JButton buttonNextCmd = createButton(Const.next, "control_fastforward_blue.png", "next command (Alt+right)");
		JButton buttonExportCsvCmd = createButton(Const.exportCsv, "report_disk.png", "export csv (Ctrl+E)");
		buttonExportCsvCmd.setAction(createExportCsvAction(""));
		buttonExportCsvCmd.setIcon(createIcon("report_disk.png")); 
		buttonExportCsvCmd.setToolTipText("export csv (Ctrl+E)");

		JPanel buttonPane = new JPanel(new FlowLayout());

		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));

		buttonPane.add(createSpacer(30));
		addButton(buttonPrevCmd, buttonPane);
		addButton(buttonPreview, buttonPane);
		addButton(buttonExecSql, buttonPane);
		addButton(buttonNextCmd, buttonPane);
		addButton(buttonListTables, buttonPane);
		buttonPane.add(createSpacer(30));
		addButton(buttonExportCsvCmd, buttonPane);

		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 0));

		return buttonPane;
	}

	private Component createSpacer(int width) {
		return Box.createRigidArea(new Dimension(width, 0));
	}

	private JButton createButton(String actionCommand, String iconUrl, String tipText) {
		JButton b = new JButton(createIcon(iconUrl));

		b.addActionListener(this);
		b.setActionCommand(actionCommand);
		b.setToolTipText(tipText);
		return b;
	}

	private ImageIcon createIcon(String iconUrl) {
		return new ImageIcon(getClass().getResource(iconUrl));
	}

	private void addButton(JButton buttonPrevCmd, JPanel buttonPane) {
		buttonPane.add(buttonPrevCmd);
		buttonPane.add(createSpacer(5));
	}

	private JPanel createSettingsArea() {
		textFieldClientId = new JTextField(35);
		textFieldClientSecret = new JPasswordField(35);
		textFieldClientSecret.setEchoChar('*');
		JButton buttonReauth = createButton(Const.reauthenticate, "arrow_refresh.png", "re-authenticate");
		buttonReauth.setMaximumSize(new Dimension(30, 20));
		SpinnerNumberModel numberModel = new SpinnerNumberModel(clientSettings.defaultQueryLimit, 0, 100000, 1);
		fieldDefaultLimit = new JSpinner(numberModel);
		NumberEditor editor = new JSpinner.NumberEditor(fieldDefaultLimit);
		fieldDefaultLimit.setEditor(editor);
		numberModel.addChangeListener(createDefaultLimitChangeListener());

		JPanel resultPane = new JPanel(new MigLayout("wrap 2"));

		resultPane.add(new JLabel("Client Id"));
		resultPane.add(buttonReauth, "gapleft 155");
		resultPane.add(textFieldClientId, "span 2");

		resultPane.add(new JLabel("Client Secret"));

		JCheckBox view = new JCheckBox("show");
		view.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setPasswordVisible(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		resultPane.add(view);

		resultPane.add(textFieldClientSecret, "span 2");

		resultPane.add(new JLabel("Query Limit"));
		resultPane.add(fieldDefaultLimit);
		fieldDefaultLimit.setMinimumSize(new Dimension(100, 18));

		textFieldClientId.setText(clientSettings.clientId);
		textFieldClientSecret.setText(clientSettings.clientSecret);

		resultPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
		return resultPane;
	}

	private ChangeListener createDefaultLimitChangeListener() {
		return new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				clientSettings.defaultQueryLimit = getQueryLimit();
			}
		};
	}

	private void setPasswordVisible(boolean value) {
		if (value)
			textFieldClientSecret.setEchoChar((char) 0);
		else
			textFieldClientSecret.setEchoChar('*');
	}

	private JPanel createResultDisplay() {
		opResult = createEditorPane();
		JScrollPane opResultScrollPane = new JScrollPane(opResult);

		JPanel resultPane = new JPanel(new BorderLayout());
		resultPane.add(opResultScrollPane, BorderLayout.CENTER);

		resultPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		resultPane.setPreferredSize(new Dimension(300, 600));

		return resultPane;
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
			runDeferred(new Runnable() {

				@Override
				public void run() {
					onStructureChanged();
				}
			});
	}

	public Document queryTextDocument() {
		return queryEditor.getDocument();
	}

	public void addAuthInfoSettingsListener(SettingsListener l) {
		addSettingsChangedListener(textFieldClientId, l);
		addSettingsChangedListener(textFieldClientSecret, l);
	}
	
	private void addSettingsChangedListener(JTextField textField, SettingsListener l) {
		Observism.addValueChangedListener(textField, new OnValueChangedEvent(){

			@Override
			public void notify(JTextField field) {
				l.onChanged(textField.getText(), textField.getName());
			}});
	}	
	
	static Gui createAndShowGUI(ActionListener controller, SyntaxElementSource s, CompletionsSource c,
			ClientSettings clientSettings, DelegatingLogger logging) {
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		Gui result = new Gui(controller, s, c, clientSettings, logging);
		result.setPreferredSize(new Dimension(clientSettings.width, clientSettings.height));
		result.setLocation(clientSettings.x, clientSettings.y);

		result.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		result.pack();
		result.setVisible(true);
		result.queryEditor.queryText.grabFocus();

		return result;
	}
}
