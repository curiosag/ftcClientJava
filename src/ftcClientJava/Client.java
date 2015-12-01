package ftcClientJava;

import java.util.List;
import java.util.Observer;

import javax.swing.text.Document;

import ftcQueryEditor.FtcAutoComplete;
import interfacing.TableInfo;
import manipulations.QueryPatching;

public class Client {

	private static void interlace(Document d, Observer textModelObserver, TextModel m) {
		d.addDocumentListener(m.getListener());
		m.addObserver(textModelObserver);
	}

	private static Runnable startup() {
		ftcClientModel model = new ftcClientModel();
		ftcClientController controller = new ftcClientController(model);
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
				Gui ui = Gui.createAndShowGUI(eng, controller, controller);
				
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
