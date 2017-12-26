import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.SwingConstants;


public class AddFriends {

	private JFrame frame;
	private JTextField searchFriend;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AddFriends window = new AddFriends();
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
	public AddFriends() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 297, 244);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		searchFriend = new JTextField();
		searchFriend.setBounds(28, 64, 163, 26);
		frame.getContentPane().add(searchFriend);
		searchFriend.setColumns(10);
		
		JLabel searchResult = new JLabel("User Not Found");
		searchResult.setBounds(43, 121, 134, 15);
		frame.getContentPane().add(searchResult);
		
		JButton btnSearch = new JButton("search");
		btnSearch.setBounds(195, 64, 90, 25);
		frame.getContentPane().add(btnSearch);
		
		JButton btnAddFriend = new JButton("Add");
		btnAddFriend.setBounds(38, 148, 117, 25);
		frame.getContentPane().add(btnAddFriend);
	}
}
