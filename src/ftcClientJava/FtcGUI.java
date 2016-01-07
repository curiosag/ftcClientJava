package ftcClientJava;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.FlowLayout;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

public class FtcGUI extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FtcGUI frame = new FtcGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public FtcGUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBounds(5, 5, 440, 290);
		contentPane.add(splitPane);
		
		JPanel panel0L = new JPanel();
		splitPane.setRightComponent(panel0L);
		GroupLayout gl_panel0L = new GroupLayout(panel0L);
		gl_panel0L.setHorizontalGroup(
			gl_panel0L.createParallelGroup(Alignment.LEADING)
				.addGap(0, 418, Short.MAX_VALUE)
		);
		gl_panel0L.setVerticalGroup(
			gl_panel0L.createParallelGroup(Alignment.LEADING)
				.addGap(0, 288, Short.MAX_VALUE)
		);
		panel0L.setLayout(gl_panel0L);
		
		JPanel panel0R = new JPanel();
		splitPane.setLeftComponent(panel0R);
		GroupLayout gl_panel0R = new GroupLayout(panel0R);
		gl_panel0R.setHorizontalGroup(
			gl_panel0R.createParallelGroup(Alignment.LEADING)
				.addGap(0, 10, Short.MAX_VALUE)
		);
		gl_panel0R.setVerticalGroup(
			gl_panel0R.createParallelGroup(Alignment.LEADING)
				.addGap(0, 288, Short.MAX_VALUE)
		);
		panel0R.setLayout(gl_panel0R);
	}
}
