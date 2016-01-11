package ftcClientJava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;
import com.google.common.base.Stopwatch;

import cg.common.check.Check;
import cg.common.core.AbstractLogger;
import cg.common.http.HttpStatus;
import cg.common.interfaces.Continuation;
import cg.common.io.FileUtil;
import cg.common.io.StringStorage;
import cg.common.misc.CmdDestination;
import cg.common.misc.CmdHistory;
import cg.common.threading.Function;
import interfaces.CompletionsSource;
import interfaces.Connector;
import interfaces.OnFileAction;
import interfaces.SyntaxElementSource;
import interfaces.SyntaxElement;
import manipulations.QueryHandler;
import structures.ClientSettings;
import structures.Completions;
import structures.QueryResult;
import uglySmallThings.AsyncWork;
import uglySmallThings.CSV;
import uglySmallThings.Events;
import util.StringUtil;

public class ftcClientController implements ActionListener, SyntaxElementSource, CompletionsSource {

	public final ftcClientModel model;
	private final QueryHandler queryHandler;
	private final AbstractLogger logging;
	private final ClientSettings clientSettings;
	private final Connector connector;

	private SwingWorker<QueryResult, Object> executionWorker = AsyncWork.createEmptyWorker();
	private final CmdHistory history;
	private final CmdDestination historyScrollDestination = new CmdDestination() {
		@Override
		public void set(String cmd) {
			model.queryText.setValue(cmd);
		}
	};

	private Stopwatch executionStopwatch = Stopwatch.createUnstarted();
	private boolean isExecuting = false;

	public ftcClientController(ftcClientModel model, AbstractLogger logging, Connector connector,
			ClientSettings clientSettings, StringStorage cmdHistoryStorage) {
		this.model = model;
		this.queryHandler = new QueryHandler(logging, connector, clientSettings);
		this.logging = logging;
		this.clientSettings = clientSettings;
		this.connector = connector;

		history = new CmdHistory(cmdHistoryStorage);
	}

	private void setStateIsExecuting(boolean value) {
		Check.isFalse(isExecuting && value);
		isExecuting = value;
		if (isExecuting) {
			Events.ui.post(Thread.State.RUNNABLE);
			executionStopwatch.reset();
			executionStopwatch.start();
		} else {
			executionStopwatch.stop();
			Events.ui.post(Thread.State.TERMINATED);
		}
	}

	private boolean getStateIsExecuting() {
		return isExecuting;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		switch (e.getActionCommand()) {
		case Const.execSql:
			if (!getStateIsExecuting())
				hdlExecSql();
			break;

		case Const.cancelExecSql:
			if (getStateIsExecuting())
				hdlCancelExecSql();
			break;

		case Const.listTables:
			model.resultData.setValue(queryHandler.getTableInfo());
			break;

		case Const.viewPreprocessedQuery:
			logging.Info(queryHandler.previewExecutedSql(model.queryText.getValue()));
			break;

		case Const.memorizeQuery:
			hdlRememberCommand();
			break;

		case Const.previousCommand:
			history.prev(historyScrollDestination);
			break;

		case Const.nextCommand:
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

		case Const.changeAuthInfo:
			if (!getStateIsExecuting())
				hdlChangeAuthInfo();
			break;

		case Const.reauthenticate:
			if (!getStateIsExecuting())
				hdlReauthenticate();
			break;

		default:
			break;
		}
	}

	private void hdlRememberCommand() {
		history.add(model.queryText.getValue());
		logging.Info("command memorized");
	}

	private void hdlChangeAuthInfo() {
		if (checkCredentialsPlausible())
			resetConnector();
	}

	private void hdlExecSql() {
		executionWorker = AsyncWork.goUnderground(new Function<QueryResult>() {
			@Override
			public QueryResult invoke() {
				setStateIsExecuting(true);
				try {
					return getQueryResult();
				} catch (Exception e) {
					return new QueryResult(HttpStatus.SC_METHOD_FAILURE, null, "Exception occured: " + e.getMessage());
				}
			}
		}, new Continuation<QueryResult>() {
			@Override
			public void invoke(QueryResult value) {
				onQueryResult(value);
				setStateIsExecuting(false);
			}
		});
		executionWorker.execute();
	}

	private QueryResult getQueryResult() {
		String sql = model.queryText.getValue();

		history.add(sql);

		return queryHandler.getQueryResult(sql);
	}

	private void onQueryResult(QueryResult result) {
		if (result.data.isPresent())
			model.resultData.setValue(result.data.get());

		String msg;
		if (result.data.isPresent())
			msg = String.format("Read %d records ", result.data.get().getRowCount());
		else
			msg = "Executed query ";

		float elapsed = (float) executionStopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000;
		msg = msg + String.format("in %.3f seconds \n", elapsed);
		logging.Info(msg + result.message.or(""));

	}

	private void hdlCancelExecSql() {
		if (executionWorker.isDone())
			return;

		if (!executionWorker.isCancelled()) {
			executionWorker.cancel(true);
			onQueryResult(new QueryResult(HttpStatus.SC_METHOD_FAILURE, null, "execution cancelled"));
		}
		setStateIsExecuting(false);
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
		if (tablePopulated()) {
			new FileAction("Export", clientSettings.pathCsvFile, null, new OnFileAction() {

				@Override
				public void onFileAction(ActionEvent e, File file) {
					clientSettings.pathCsvFile = util.FileUtil.getPathOnly(file);
					logging.Info(CSV.write(model.resultData.getValue(), file.getPath()));
				}
			}).actionPerformed(e);
		} else
			logging.Info("no data to export");
	}

	private boolean tablePopulated() {
		return model.resultData.getValue() != null && model.resultData.getValue().getRowCount() > 0;
	}

	@Override
	public List<SyntaxElement> get(String query) {
		return queryHandler.getHighlighting(query);
	}

	@Override
	public Completions get(String query, int cursorPos) {
		return queryHandler.getPatcher(query, cursorPos).getCompletions();
	}

	private boolean checkCredentialsPlausible() {
		boolean result = !(StringUtil.emptyOrNull(model.clientSecret.getValue())
				|| StringUtil.emptyOrNull(model.clientId.getValue()));

		if (!result)
			logging.Info("incomplete authentication credentials");

		return result;
	}

	public void resetConnector() {
		Dictionary<String, String> credentials = new Hashtable<String, String>();
		credentials.put(ClientSettings.keyClientSecret, model.clientSecret.getValue());
		credentials.put(ClientSettings.keyClientId, model.clientId.getValue());
		queryHandler.reset(credentials);
	}

}
