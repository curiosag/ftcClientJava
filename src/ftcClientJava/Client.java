package ftcClientJava;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observer;

import javax.swing.JTextField;
import javax.swing.text.Document;

import com.google.common.base.Optional;

import cg.common.core.AbstractLogger;
import cg.common.core.DelegatingLogger;
import cg.common.core.SystemLogger;
import cg.common.interfaces.OnValueChangedEvent;
import cg.common.io.PreferencesStringStorage;
import main.java.fusiontables.AuthInfo;
import main.java.fusiontables.FusionTablesConnector;
import main.java.fusiontables.PreferencesDataStoreFactory;
import structures.ClientSettings;
import interfaces.Connector;
import test.MockConnector;

public class Client {

	private static void interlace(Document d, Observer textModelObserver, TextModel m) {
		d.addDocumentListener(m.getListener());
		m.addObserver(textModelObserver);
	}

	private final static DelegatingLogger logging = new DelegatingLogger(new SystemLogger());

	@SuppressWarnings("unused")
	private static Connector getConnector(ClientSettings clientSettings) {
		if (true)
			return new FusionTablesConnector(logging,
					Optional.of(new AuthInfo(clientSettings.clientId, clientSettings.clientSecret)), Client.class);
		else
			return MockConnector.instance();

	}

	private static Runnable startup() {
		ClientSettings clientSettings = ClientSettings.instance(Client.class);
		ftcClientModel model = new ftcClientModel(clientSettings);
		ftcClientController controller = new ftcClientController(model, logging, getConnector(clientSettings),
				clientSettings, new PreferencesStringStorage(Const.PREF_ID_CMDHISTORY, Client.class));

		logging.setDelegate(createModelLogger(model.resultText));

		return new Runnable() {

			@Override
			public void run() {
				Gui ui = Gui.createAndShowGUI(controller, controller, controller, clientSettings, logging);

				// StdIORedirect.addRedirect(new CallbackHandler(new
				// OnLogLineWritten(){
				//
				// @Override
				// public void notify(String value) {
				// // model.resultText.setValue(value);
				//
				// }}));

				model.resultData.addObserver(ui.createResultDataObserver());

				model.clientId.addObserver(ui.createClientIdObserver());
				model.clientSecret.addObserver(ui.createClientSecretObserver());
				ui.addClientIdChangedListener(createOnValueChangedEvent(model.clientId));
				ui.addClientSecretChangedListener(createOnValueChangedEvent(model.clientSecret));

				ui.addAuthInfoSettingsListener(controller.getAuthInfoSettingsListener());

				interlace(ui.opResultDocument(), ui.createOpResultObserver(), model.resultText);
				interlace(ui.queryTextDocument(), ui.createQueryObserver(), model.queryText);
			}
		};
	}

	private static OnValueChangedEvent createOnValueChangedEvent(TextModel target) {
		return new OnValueChangedEvent() {

			@Override
			public void notify(JTextField field) {
				target.setValue(field.getText());
			}
		};
	}

	private static AbstractLogger createModelLogger(TextModel resultText) {
		return new AbstractLogger() {

			@Override
			protected void hdlInfo(String info) {
				addLogLine(resultText, info);
			}

			@Override
			protected void hdlError(String error) {
				addLogLine(resultText, error);
			}

			private void addLogLine(TextModel resultText, String info) {

				Format formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
				String date = formatter.format(new Date());

				resultText.setValue(prune(resultText.getValue()) + "\n" + date + "\n" + info);
			}

			private String prune(String value) {
				if (!value.endsWith("\n"))
					value = value + "\n";

				int current = 0;
				while (value.length() - current >= Const.MAX_LOGSIZE)
					current = value.indexOf('\n', current);

				return value.substring(current);
			}

		};
	}

	public static void main(String[] args) {
		Gui.runDeferred(startup());
	}
}
