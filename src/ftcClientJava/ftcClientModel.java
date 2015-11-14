package ftcClientJava;

public class ftcClientModel {

	public final TextModel queryText = new TextModel();
	public final TextModel resultText = new TextModel();
	public final TextModel errorText = new TextModel();
	public final TextModel infoText = new TextModel();
	
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
		
}
