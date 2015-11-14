package ftcClientJava;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.base.Optional;

import interfacing.ColumnInfo;
import interfacing.TableInfo;

public class CompletionPicker extends JPanel implements ListSelectionListener, WindowListener, KeyListener {
	private static final long serialVersionUID = -7917062917946392736L;

	private Optional<String> itemSelected = Optional.absent();
	private Optional<Object> detailItemSelected = Optional.absent();
	private final ItemChosenHandler onItemChosen;

	private static class Item {
		private String value = null;
		private DefaultListModel<String> subitems = new DefaultListModel<String>();

		Item(String value, String[] subitems) {
			this.value = value;
			for (String subitem : subitems)
				this.subitems.addElement(subitem);
		}

		public String toString() {
			return value;
		}
	}

	private DefaultListModel<Item> model = new DefaultListModel<Item>();

	private JList<Item> itemListDisplay = new JList<Item>(model);
	private JList<String> itemSubListDisplay = new JList<String>();

	private CompletionPicker(ItemChosenHandler onItemChosen) {
		super(new GridLayout(0, 2));
		add(itemListDisplay);
		add(itemSubListDisplay);

		itemListDisplay.addListSelectionListener(this);
		itemSubListDisplay.addListSelectionListener(this);
		this.onItemChosen = onItemChosen;
	}

	public void addItem(String item, String[] subitems) {
		model.addElement(new Item(item, subitems));
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == itemListDisplay) {
			Item item = (Item) itemListDisplay.getSelectedValue();
			itemSubListDisplay.setModel(item.subitems);
			itemSelected = Optional.fromNullable(item.value);
		} else {
			detailItemSelected = Optional.fromNullable(itemSubListDisplay.getSelectedValue());
		}
	}

	private static String[] toArray(List<ColumnInfo> columns) {

		String[] result = new String[columns.size()];
		int count = 0;
		for (ColumnInfo c : columns) {
			result[count] = String.format("%s (%s)", c.name, c.type);
			count++;
		}
		return result;

	}

	private static CompletionPicker createCompletions(List<TableInfo> tableInfo, ItemChosenHandler onItemChosen) {
		CompletionPicker c = new CompletionPicker(onItemChosen);
		for (TableInfo i : tableInfo)
			c.addItem(i.name, toArray(i.columns));
		return c;
	}

	public static void show(List<TableInfo> tableInfo, ItemChosenHandler onItemChosen) {
		JFrame frame = new JFrame("Nested Lists");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		CompletionPicker c = createCompletions(tableInfo, onItemChosen);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.add(c);
		frame.addWindowListener(c);
		frame.addKeyListener(c);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		onItemChosen.onItemChosen(itemSelected, detailItemSelected);
	}

	
	@Override
	public void windowOpened(WindowEvent e) {
		select(itemListDisplay, 0);
	}

	private void select(@SuppressWarnings("rawtypes") JList l, int index){
		if (index < l.getModel().getSize())
			l.setSelectionInterval(index, index);
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		int keycode = e.getKeyCode();
		System.out.println(Integer.toString(keycode));
		switch (keycode) {

		case KeyEvent.KEY_LOCATION_RIGHT:
			select(itemSubListDisplay, 0);
			break;
		case KeyEvent.KEY_LOCATION_LEFT:
			select(itemListDisplay, itemListDisplay.getSelectedIndex() >= 0 ? itemListDisplay.getSelectedIndex() : 0);
			break;	
		default:
		}

	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

}
