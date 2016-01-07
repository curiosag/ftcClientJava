package ftcClientJava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JEditorPane;
import javax.swing.table.TableModel;

import com.google.common.base.Stopwatch;

import cg.common.check.Check;
import cg.common.core.AbstractLogger;
import cg.common.io.FileUtil;
import cg.common.io.StringStorage;
import cg.common.misc.CmdDestination;
import cg.common.misc.CmdHistory;
import interfaces.CompletionsSource;
import interfaces.Connector;
import interfaces.OnFileAction;
import interfaces.SettingsListener;
import interfaces.SyntaxElementSource;
import interfaces.SyntaxElement;
import manipulations.QueryHandler;
import manipulations.QueryPatching;
import structures.ClientSettings;
import structures.Completions;
import structures.QueryResult;
import structures.TableInfo;
import uglySmallThings.CSV;
import util.StringUtil;

public class ftcClientController implements ActionListener, SyntaxElementSource, CompletionsSource {
	
	public final ftcClientModel model;
	private final QueryHandler queryHandler;
	private final AbstractLogger logging;
	private final ClientSettings clientSettings;
	private final Connector connector;

	private final CmdHistory history;
	private final CmdDestination historyScrollDestination = new CmdDestination() {
		@Override
		public void set(String cmd) {
			model.queryText.setValue(cmd);
		}
	};

	private Stopwatch stopwatch = Stopwatch.createUnstarted();

	public ftcClientController(ftcClientModel model, AbstractLogger logging, Connector connector,
			ClientSettings clientSettings, StringStorage cmdHistoryStorage) {
		this.model = model;
		this.queryHandler = new QueryHandler(logging, connector, clientSettings);
		this.logging = logging;
		this.clientSettings = clientSettings;
		this.connector = connector;
		
		history = new CmdHistory(cmdHistoryStorage);
	}

	public List<TableInfo> getTableList(boolean addDetails) {
		return queryHandler.getTableList(addDetails);
	}

	public QueryPatching getPatcher(String query, int cursorPos) {
		return queryHandler.getPatcher(query, cursorPos);
	}

	private void execSql() {
		String sql = model.queryText.getValue();
		history.add(sql);
		stopwatch.reset();
		stopwatch.start();
		QueryResult result = queryHandler.getQueryResult(sql);
		stopwatch.stop();

		if (result.data.isPresent())
			model.resultData.setValue(result.data.get());

		String msg;
		if (result.data.isPresent())
			msg = String.format("Read %d records ", result.data.get().getRowCount());
		else
			msg = "Executed query ";

		float elapsed = (float) stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000;
		msg = msg + String.format("in %.3f seconds \n", elapsed);
		logging.Info(msg + result.message.or(""));
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		switch (e.getActionCommand()) {
		case Const.execSql:
			execSql();
			break;

		case Const.listTables:
			model.resultData.setValue(queryHandler.getTableInfo());
			break;

		case Const.preview:
			logging.Info(queryHandler.previewExecutedSql(model.queryText.getValue()));
			break;

		case Const.oh:
			;
			break;

		case Const.prev:
			history.prev(historyScrollDestination);
			break;

		case Const.next:
			history.next(historyScrollDestination);
			break;

		case Const.fileOpen:
			hdlFileOpen();
			break;

		case Const.fileSave:
			hdlFileSave();
			break;

		case Const.exportCsv:
			hdlExportCsvAction(e);
			break;
			
		case Const.reauthenticate:
			hdlReauthenticate();
			break;

		default:
			break;
		}
	}

	private void hdlReauthenticate() {
		connector.clearStoredLoginData();
		resetConnector();
	}

	private void hdlFileOpen() {
		new FileAction("Open", clientSettings.pathScriptFile, null, new OnFileAction() {

			@Override
			public void onFileAction(ActionEvent e, File file) {
				model.queryText.setValue(FileUtil.readFromFile(file.getPath()));
				clientSettings.pathScriptFile = util.FileUtil.getPathOnly(file);
			}
		}).actionPerformed(null);
	}

	private void hdlFileSave() {

		new FileAction("Save", clientSettings.pathScriptFile, null, new OnFileAction() {

			@Override
			public void onFileAction(ActionEvent e, File file) {
				FileUtil.writeToFile(model.queryText.getValue(), file.getPath());
				clientSettings.pathScriptFile = util.FileUtil.getPathOnly(file);
			}
		}).actionPerformed(null);
	}

	private void hdlExportCsvAction(ActionEvent e) {
		new FileAction("Export", clientSettings.pathCsvFile, null, new OnFileAction() {

			@Override
			public void onFileAction(ActionEvent e, File file) {
				Check.isTrue(e.getSource() instanceof TableModel);

				clientSettings.pathCsvFile = util.FileUtil.getPathOnly(file);
				logging.Info(CSV.write((TableModel) e.getSource(), file.getPath()));
			}
		}).actionPerformed(e);
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

	@Override
	public List<SyntaxElement> get(String query) {
		return queryHandler.getHighlighting(query);
	}

	@Override
	public Completions get(String query, int cursorPos) {
		return queryHandler.getPatcher(query, cursorPos).getCompletions();
	}

	public SettingsListener getAuthInfoSettingsListener() {
		return new SettingsListener() {

			@Override
			public void onChanged(String value, String key) {
				if (credentialsPlausible()) 
					resetConnector();
			}

			private boolean credentialsPlausible() {
				return !(StringUtil.emptyOrNull(model.clientSecret.getValue())
						|| StringUtil.emptyOrNull(model.clientId.getValue()));
			}
		};
	}
	
	private void resetConnector() {
		Dictionary<String, String> credentials = new Hashtable<String, String>();
		credentials.put(ClientSettings.keyClientSecret, model.clientSecret.getValue());
		credentials.put(ClientSettings.keyClientId, model.clientId.getValue());
		queryHandler.reset(credentials);
	}

}
