package ftcClientJava;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class KeyboardActions {

	private HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();

	public void add(int keyCode, int modifiers, Action action) {
		KeyStroke key = KeyStroke.getKeyStroke(keyCode, modifiers);
		actionMap.put(key, action);
	}

	/**
	 * http://stackoverflow.com/questions/100123/application-wide-keyboard-
	 * shortcut-java-swing
	 */
	public KeyboardActions() {

		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher(new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				if (actionMap.containsKey(keyStroke)) {
					final Action a = actionMap.get(keyStroke);
					final ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), null);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							a.actionPerformed(ae);
						}
					});
					return true;
				}
				return false;
			}
		});
	}
}