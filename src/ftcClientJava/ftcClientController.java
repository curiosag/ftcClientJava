package ftcClientJava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JEditorPane;

import cg.common.check.Check;
import cg.common.core.AbstractLogger;
import cg.common.core.Logging;
import cg.common.io.FileStringStorage;
import cg.common.misc.CmdDestination;
import cg.common.misc.CmdHistory;
import interfaces.Connector;
import interfaces.SyntaxElementSource;
import interfaces.SyntaxElement;
import manipulations.QueryHandler;
import manipulations.QueryPatching;
import structures.Completions;
import structures.TableInfo;

public class ftcClientController implements ActionListener, SyntaxElementSource {
	private static final String historyStore = "./commandHistory.txt";

	public final ftcClientModel model;
	private final QueryHandler queryHandler;
	private final AbstractLogger logging;

	private CmdHistory history = new CmdHistory(new FileStringStorage(historyStore));
	private final CmdDestination historyScrollDestination = new CmdDestination() {
		@Override
		public void set(String cmd) {
			model.queryText.setValue(cmd);
		}
	};
	
	public ftcClientController(ftcClientModel model, AbstractLogger logging, Connector connector) {
		this.model = model;
		this.queryHandler = new QueryHandler(logging, connector);
		this.logging = logging;
		
		model.resultText.setValue(getUsageInfo());
		
		setupLogging();
	}

	private void setupLogging() {
		logging.addObserver(new Observer(){

			@Override
			public void update(Observable o, Object arg) {
				Check.isTrue(o instanceof Logging);
				Logging logging = (Logging) o;
				if (logging.lastError().isPresent()) 
					model.errorText.setValue(logging.lastError().get());
				if (logging.lastInfo().isPresent()) 
					model.infoText.setValue(logging.lastInfo().get());
			}});
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
