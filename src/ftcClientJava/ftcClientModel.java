package ftcClientJava;

import javax.swing.table.TableModel;

import cg.common.check.Check;
import cg.common.misc.SimpleObservable;
import structures.ClientSettings;

public class ftcClientModel {

	public final TextModel clientId = new TextModel();
	public final TextModel clientSecret = new TextModel();
	
	public final TextModel queryText = new TextModel();
	public final TextModel resultText = new TextModel();

	public final SimpleObservable<TableModel> resultData = new SimpleObservable<TableModel>(null);

	public ftcClientModel(ClientSettings clientSettings) {
		Check.notNull(clientSettings);
		clientId.setValue(clientSettings.clientId);
		clientSecret.setValue(clientSettings.clientSecret);
	}

}
