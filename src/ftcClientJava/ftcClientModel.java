package ftcClientJava;

import javax.swing.table.TableModel;

import cg.common.misc.SimpleObservable;

public class ftcClientModel {

	public final TextModel queryText = new TextModel();
	public final TextModel resultText = new TextModel();
	public final TextModel errorText = new TextModel();
	public final TextModel infoText = new TextModel();
	public final SimpleObservable<TableModel> resultData = new SimpleObservable<TableModel>(null);

	public void setEditorText(String value) {
		queryText.setValue(value);
	}

	public void setResultText(String value) {
		resultText.setValue(value);
	}

	public void setErrorText(String value) {
		errorText.setValue(value);
	}

	public void setInfoText(String value) {
		infoText.setValue(value);
	}

	public void setResultData(TableModel resultData) {
		this.resultData.setValue(resultData);

	}

}
