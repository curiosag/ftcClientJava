package ftcClientJava;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Observism {

	public static Observer createObserver(JTextArea f)
	{
		return new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				f.setText(TextModel.fromObservable(o));
			}
		};
	}

	public static Observer createObserver(JTextField f)
	{
		return new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				f.setText(TextModel.fromObservable(o));
			}
		};
	}
	
	public static Observer createObserver(JEditorPane f)
	{
		return new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				f.setText(TextModel.fromObservable(o));
			}
		};
	}

}
