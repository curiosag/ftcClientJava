package ftcClientJava;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import cg.common.interfaces.OnValueChangedEvent;

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
				f.repaint();
			}
		};
	}

	private static FocusListener createValueChangedListener(JTextField textField, OnValueChangedEvent delegate)
	{
		FocusListener result = new FocusListener(){
			
			OnValueChangedEvent onFocus = delegate;
			String value = textField.getText();
			
			@Override
			public void focusGained(FocusEvent e) {
				value = textField.getText();
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (!value.equals(textField.getText()))
					onFocus.notify(textField);
			}};
		
		return result;
	}
	
	public static void addValueChangedListener(JTextField f, OnValueChangedEvent delegate)
	{
		f.addFocusListener(createValueChangedListener(f, delegate));
	}

	
}
