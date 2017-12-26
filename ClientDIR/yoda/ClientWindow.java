import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.TextArea;
import javax.swing.JTextPane;
import java.awt.TextField;
import javax.swing.JList;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;


public class ClientWindow {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientWindow window = new ClientWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ClientWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 541, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		TextField textField = new TextField();
		textField.setBounds(126, 224, 288, 30);
		frame.getContentPane().add(textField);
		
		JList list = new JList();
		list.setBounds(75, 66, 101, 134);
		frame.getContentPane().add(list);
		
		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		btnSend.setBounds(431, 224, 76, 30);
		frame.getContentPane().add(btnSend);
		
		TextArea textArea = new TextArea();
		textArea.setBounds(193, 44, 233, 156);
		frame.getContentPane().add(textArea);
		
		JLabel lblFriends = new JLabel("friends");
		lblFriends.setBounds(99, 43, 70, 15);
		frame.getContentPane().add(lblFriends);
		
		JLabel lblNewLabel = new JLabel("MSG:");
		lblNewLabel.setBounds(75, 224, 46, 30);
		frame.getContentPane().add(lblNewLabel);
		
		JButton btnadd = new JButton("+");
		btnadd.setBounds(27, 175, 46, 25);
		frame.getContentPane().add(btnadd);
		
		JButton btnSendf = new JButton("file");
		btnSendf.setBounds(431, 187, 76, 25);
		frame.getContentPane().add(btnSendf);
	}
}
