package ftcClientJava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.JEditorPane;

import com.google.common.base.Optional;

import cg.common.core.Logging;
import cg.common.io.FileStringStorage;
import cg.common.misc.CmdDestination;
import cg.common.misc.CmdHistory;
import fusiontables.AuthInfo;
import fusiontables.FusionTablesConnector;
import interfacing.AbstractCompletion;
import interfacing.Completions;
import interfacing.SyntaxElement;
import interfacing.SyntaxElementSource;
import interfacing.TableInfo;
import manipulations.QueryHandler;
import manipulations.QueryPatching;

public class ftcClientController implements ActionListener, SyntaxElementSource {
	private static final String historyStore = "./commandHistory.txt";

	ftcClientModel model;

	private Logging logging = new Logging() {

		@Override
		public void Info(String info) {
			model.infoText.setValue(info);
		}

		@Override
		public void Error(String error) {
			model.errorText.setValue(error);
		}
	};

	private CmdHistory history = new CmdHistory(new FileStringStorage(historyStore));
	private final CmdDestination historyScrollDestination = new CmdDestination() {
		@Override
		public void set(String cmd) {
			model.queryText.setValue(cmd);
		}
	};

	private final FusionTablesConnector connector = new FusionTablesConnector(logging,
			Optional.of(new AuthInfo("1002359378366-ipnetharogqs3pmhf9q35ov4m14l6014.apps.googleusercontent.com",
					"wJWwr-FbTNtyCLhHvXE5mCo6")));

	private QueryHandler queryHandler = new QueryHandler(logging, connector);

	public ftcClientController(ftcClientModel model) {
		this.model = model;
		model.resultText.setValue(getUsageInfo());
	}

	public List<TableInfo> getTableList(boolean addDetails) {
		return queryHandler.getTableList(addDetails);
	}

	public QueryPatching getPatcher(String query, int cursorPos) {
		return queryHandler.getPatcher(query, cursorPos);
	}

	private String getUsageInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("USAGE \n\n");
		sb.append("F1 backwards in sql history \n");
		sb.append("F2 forward in sql history \n");
		sb.append("F3 list tables \n");
		sb.append("F4 preview sql \n");
		sb.append("F5 execute sql \n");
		sb.append("F11 autocomplete \n");
		sb.append("F12 the oh command \n");
		return sb.toString();
	}

	private void execSql() {
		String sql = model.queryText.getValue();
		history.add(sql);
		model.resultText.setValue(queryHandler.getQueryResult(sql));
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// model.infoText.setValue(String.format("id %d mask %d",
		// e.getID(), e.getModifiers()));

		switch (e.getActionCommand()) {
		case Const.execSql:
			execSql();
			break;

		case Const.listTables:
			model.resultText.setValue(queryHandler.getTableInfo());
			break;

		case Const.preview:
			model.resultText.setValue(queryHandler.previewExecutedSql(model.queryText.getValue()));
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

		default:
			break;
		}
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

}
