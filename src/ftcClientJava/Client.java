package ftcClientJava;

import java.util.List;
import java.util.Observer;

import javax.swing.text.Document;

import com.google.common.base.Optional;

import cg.common.core.SystemLogger;
import main.java.fusiontables.AuthInfo;
import main.java.fusiontables.FusionTablesConnector;

import interfaces.Connector;
import manipulations.QueryPatching;
import structures.TableInfo;
import test.MockConnector;

public class Client {

	private static void interlace(Document d, Observer textModelObserver, TextModel m) {
		d.addDocumentListener(m.getListener());
		m.addObserver(textModelObserver);
	}

	private final static SystemLogger logging = new SystemLogger();

	
	@SuppressWarnings("unused")
	private static Connector getConnector()
	{
		if (false)
			return 	new FusionTablesConnector(logging,
					Optional.of(new AuthInfo("1002359378366-ipnetharogqs3pmhf9q35ov4m14l6014.apps.googleusercontent.com",
							"wJWwr-FbTNtyCLhHvXE5mCo6")));
		else
			return MockConnector.instance();

	}
	
	private static Runnable startup() {
		ftcClientModel model = new ftcClientModel();
		ftcClientController controller = new ftcClientController(model, logging, getConnector());
		
		DataEngine eng = new DataEngine() {

			@Override
			public List<TableInfo> getTableList(boolean addDetails) {
				return controller.getTableList(addDetails);
			}

			@Override
			public QueryPatching getPatcher(String query, int cursorPos) {
				return controller.getPatcher(query, cursorPos);
			}
		};

		return new Runnable() {

			@Override
			public void run() {
				Gui ui = Gui.createAndShowGUI(controller, controller, controller);
				
				model.errorText.addObserver(ui.createErrorTextObserver());

				interlace(ui.opResultDocument(), ui.createOpResultObserver(), model.resultText);
				interlace(ui.queryTextDocument(), ui.createQueryTextObserver(), model.queryText);

			}
		};
	}

	public static void main(String[] args) {
		Gui.runDeferred(startup());
	}
}
