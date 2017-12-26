import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*; 
import java.util.List; 
import javax.swing.event.*;

class Client {

    private static JFrame loginFrame;
    private static JFrame chartFrame;
    private static JFrame addFriendsFrame;
    private static JFrame fileFrame;
    
    private JTextField  userText;
    private JPasswordField passwordText;
    private PrintWriter serverWrite;
    private InputStream serverIn;
    private Socket serverSocket;
    private JTextArea chartText;
    private String myName;
    private String targetName;
    private JTextField searchFriend;
    private JLabel searchResult;
    private TextField sendText = new TextField();
    private JList recvFileList;

    private List<User> myFriends = new ArrayList <User>();
    TextArea textArea = new TextArea();
    private JList friendList = new JList();
    private JFileChooser btnBrowse;
    private int tAck = 0;
    private int fileMode = 0;
    private FileOutputStream fileOutputStream; 

    private class User {
        public String name;
        public String password;
        public String msg;
        public User() {
            name = "";
            password = "";
            msg = "";
        }
    }

    private class ListenThread extends Thread {
        public ListenThread() {
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (fileMode == 0) {
                        byte[] buf = new byte[256];  
                        {
                            int len = 0;  
                            len = serverIn.read(buf);
                            String inPack = new String(buf, 0, len);
                            System.out.println("Server : " + inPack);
                            System.out.println("  Result Server : " + inPack.substring(0,2));
                            char cmd = inPack.charAt(0);

                            if (cmd == 'l') { //login
                                if (inPack.substring(0,2).equals("ly")) {
                                    
                                    System.out.println("Yes");
                                    loginFrame.dispose();
                                    buildChartFrame();
                                    initFriends(inPack);
                                }
                            }
                            else if (cmd == 's') { //send msg
                                System.out.println("recv a msg !" + inPack);
                                String srcName = inPack.substring(1,21);
                                String dstName = inPack.substring(21,41);
                                String msg = inPack.substring(40,201);
                                System.out.println("srcName :" + srcName.length());
                                System.out.println("dstName :" + dstName.length());
                                
                                for (User user : myFriends) if (user.name.equals(srcName.substring(0,user.name.length()))) {
                                    System.out.println("zjc");
                                    user.msg += "[" + srcName + "]: " + msg + "\n";
                                }
                                updateCharArea();
                            }
                            else if (cmd == 'a') {//search result
                                System.out.println("recv a search msg!" + inPack);
                                if (inPack.charAt(1) == 'y') {
                                    System.out.println("has name!");
                                    String serachName = inPack.substring(2,22);
                                    for (int i = 0; i < serachName.length(); i++) if (serachName.charAt(i) == '\0') {
                                        serachName = serachName.substring(0,i + 1); break;
                                    }
                                    System.out.println(serachName);
                                    searchResult.setText(serachName);
                                    searchResult.updateUI();
                                }
                                else {
                                    searchResult.setText("User Not Found");
                                    searchResult.updateUI();
                                }
                            }
                            else if (cmd == 'n') { //new friends
                                updateFriends(inPack);
                            }
                            else if (cmd == '0') {//ack
                                tAck += 1;
                            }
                            else if (cmd == '1') {//sync file list
                                updateRecvFiles(inPack);
                            }
                            else if (cmd == '2') {
                                fileMode = 1;
                                String pack = "0";
                                pack = addPadding(pack,256);
                                try{
                                    serverWrite.println(pack); serverWrite.flush();
                                } catch (Exception ee) {
                                    System.out.println("FUCK Send!");
                                }
                            }
                        }
                    }
                    else {//file Mode

                        byte[] buf = new byte[512];
                        String pack;
                        int len; 
                        while (true) {
                            len = serverIn.read(buf);
                            System.out.println(new String(buf));
                            System.out.println("len = " + len);
                            fileOutputStream.write(buf,0,len);
                            pack = "0";
                            pack = addPadding(pack,256);
                            try{
                                serverWrite.println(pack); serverWrite.flush();
                            } catch (Exception ee) {
                                System.out.println("FUCK Send!");
                            }

                            if (len < 512) break;
                        }
                        fileOutputStream.close();
                        System.out.println("Finished");
                        fileMode = 0;
                    } 
                }
            } 
            catch (Exception ee) {
                System.out.println("FUCK!");
            }
        }
    }

    private void initFriends(String inPack) {
        int cnt = Integer.parseInt(inPack.substring(2,3));
        System.out.println("cnt = " + cnt);
        for (int i = 0; i < cnt; i++) {
            User friend = new User();
            for (int j = 0; j < 19; j++) if (inPack.charAt(3 + i * 20 + j)!='\0') {
                friend.name += inPack.charAt(3 + i * 20 + j);
            } else break;
            
            friend.msg = "";
            for (int j = 0; j < 29; j++) if (inPack.charAt(3 + cnt * 20 + i * 30 + j) != '\0') {
                friend.msg += inPack.charAt(3 + cnt * 20 + i * 30 + j);
            } else break;
            
            if (friend.msg.length() > 0)
                friend.msg = "[" + friend.name + "]: " + friend.msg;

            System.out.println("name = " + friend.name); 
            System.out.println("msg = " + friend.msg); 
            myFriends.add(friend);
        }
        setFriendsList();
    }

    private void updateFriends(String inPack) {
        int cnt = Integer.parseInt(inPack.substring(2,3));
        System.out.println("cnt = " + cnt);
        myFriends = new ArrayList <User>();
        for (int i = 0; i < cnt; i++) {
            User friend = new User();
            for (int j = 0; j < 19; j++) if (inPack.charAt(3 + i * 20 + j)!='\0') {
                friend.name += inPack.charAt(3 + i * 20 + j);
            } else break;
            System.out.println("name = " + friend.name); 
            myFriends.add(friend);
        }
        setFriendsList();
    }
    
    private void updateRecvFiles(String inPack) {
        int cnt = Integer.parseInt(inPack.substring(2,3));
        System.out.println("cnt = " + cnt);
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        for (int i = 0; i < cnt; i++){
            
            String srcName,fileName; srcName = ""; fileName = ""; 
            for (int j = 0; j < 19; j++) if (inPack.charAt(3 + i * 50 + j) != '\0') {
                srcName += inPack.charAt(3 + i * 50 + j);
            } else break;

            for (int j = 0; j < 29; j++) if (inPack.charAt(3 + i * 50 + j + 20) != '\0') {
                fileName += inPack.charAt(3 + i * 50 + 20 + j);
            }else break;

            String file;
            file = "[" + srcName + "]:" +fileName; 
            listModel.addElement(file);
        }
        
        recvFileList.setModel(listModel);
        recvFileList.updateUI();
    }

    public String addPadding(String pack,int len) {
        String out = pack;
        while (out.length() < len) out = out + '\0';
        return out;
    }

    public String setString(String base, String str, int l,int r) {
        String out = "";
        for (int i = 0; i < l; i++) out += base.charAt(i);
        for (int i = l; i <= r; i ++) {
            int j = i - l;
            if (j < str.length()) out += str.charAt(j); else out += base.charAt(i);
        }
        for (int i = r + 1; i < base.length(); i++) out += base.charAt(i);
        return out;
    }

    private void setFriendsList() {
         int cnt = myFriends.size();
         DefaultListModel<String> listModel = new DefaultListModel<String>();
         for (int i = 0; i < cnt; i++) listModel.addElement(myFriends.get(i).name);
         friendList.setModel(listModel);
         friendList.updateUI();
    }



    private class LoginButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String buttonName = e.getActionCommand();
            if (buttonName.equals("login")) {
                String pack;
                    
                pack = "l";
                String userName = userText.getText();
                String password = String.valueOf(passwordText.getPassword());
                pack = setString(pack,addPadding(userName,20),1,20);
                pack = setString(pack,addPadding(password,20),21,40);
                pack = addPadding(pack,256);

                System.out.println("pack : " + pack);

                myName = userName;

                try{
                    serverWrite.println(pack); serverWrite.flush();
                } catch (Exception ee) {
                    System.out.println("FUCK Send!");
                }
            }
        }
    }

    private class SendButtonListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String buttonName = e.getActionCommand();
            if (buttonName.equals("Send")) {
                String pack;

                pack = "s";
                if (friendList.getSelectedValue() != null) {
                    targetName = friendList.getSelectedValue().toString();
                    String msg = "";
                    msg = sendText.getText();
                        pack = setString(pack,addPadding(myName,20),1,20);
                        pack = setString(pack,addPadding(targetName,20),21,40);
                        pack = setString(pack,addPadding(msg,200 - 41 + 1),41,200);
                        pack = addPadding(pack,256);

                        System.out.println("pack : " + pack);
                    
                    for (User user : myFriends) if (user.name.equals(targetName)) {
                        user.msg += "[" + myName + "]: " + msg + "\n";
                        updateCharArea();
                    }
                    try{
                        serverWrite.println(pack); serverWrite.flush();
                    } catch (Exception ee) {
                        System.out.println("FUCK Send!");
                    } 
                }
                
            }
        }
    }

    private class AddButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            buildAddFriendsFrame();
        }
    }

    private class SearchButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String pack;
            pack = "a"; //ask for friends
            String targetName = searchFriend.getText();
            pack = setString(pack,addPadding(myName,20),1,20);
            pack = setString(pack,addPadding(targetName,20),21,40);
            pack = addPadding(pack,256);

            System.out.println("search pack : "  + pack);
            try {
                        serverWrite.println(pack); serverWrite.flush();
            } 
            catch (Exception ee) {
                        System.out.println("FUCK Send!");
            } 
        }
    }

    private class AddFriendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String pack;
            pack = "n"; //new friends;
            String targetName = searchFriend.getText();
            pack = setString(pack,addPadding(myName,20),1,20);
            pack = setString(pack,addPadding(targetName,20),21,40);
            pack = addPadding(pack,256);

            System.out.println("new friend pack : "  + pack);
            try {
                        serverWrite.println(pack); serverWrite.flush();
            } 
            catch (Exception ee) {
                        System.out.println("FUCK Send!");
            } 
        }
    }
    private class FriendListUpdateListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
           updateCharArea();
        }
    }

    private class SendFileButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                File file = btnBrowse.getSelectedFile();
                System.out.println("path = " + file.getAbsolutePath());
                System.out.println("fileName = " + file.getName());
                
                FileInputStream fis = new FileInputStream(file);
                DataOutputStream dos = new DataOutputStream(serverSocket.getOutputStream());

                int tSend = 0;

                String pack = "f";
                String targetName = friendList.getSelectedValue().toString();
                String fname = file.getName();
                
                pack = setString(pack,addPadding(myName,20),1,20);
                pack = setString(pack,addPadding(targetName,20),21,40);
                pack = setString(pack,addPadding(fname,100),41,140);
                pack = addPadding(pack,256);

                System.out.println("new file pack : "  + pack);
                try {
                            serverWrite.println(pack); serverWrite.flush();
                } 
                catch (Exception ee) {
                            System.out.println("FUCK Send!");
                } 

                tSend += 1;

                int sendPackSize = 512;
                byte[] sendBytes = new byte[512];
                int len = 0;
                while ((len = fis.read(sendBytes,0,sendPackSize)) > 0)
                {
                    //System.out.println(new String(sendBytes));
                    while (tSend != tAck){System.out.println(tSend + " " + tAck);}
                    try {
                        dos.write(sendBytes,0,len);
                    } 
                    catch (Exception ee) {
                            System.out.println("FUCK Send!");
                    }
                    System.out.println("A PackSend! tSend = " + tSend + " tAck = " + tAck);
                    if (len < sendPackSize) break;
                    tSend += 1;
                }
                System.out.println("Finished!");
                tAck = 0;
            }
            catch (Exception dde) {System.out.println("hehe");}
        }
    }

    private class SendfButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //fileMode = 1;
            buildFileFrame();
            String pack = "1";// sync files list;
            pack = addPadding(pack,256);
            try {
                serverWrite.println(pack); serverWrite.flush();
            } 
            catch (Exception ee) {
                System.out.println("FUCK Send!");
            } 

        }
    }

    private class ReceiveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String fileName;
            if (recvFileList.getSelectedValue() != null) {
                fileName = recvFileList.getSelectedValue().toString();
                int pos = 0;
                String fname = "";
                for (int i = 0; i < fileName.length(); i++) if (fileName.charAt(i) == ':') {
                    pos = i; break;
                }

                for (int i = pos + 1; i < fileName.length(); i++) fname += fileName.charAt(i);

                System.out.println("fname = " + fname);
                String pack;
                pack = "2";//ask for files
                pack = setString(pack,addPadding(fname,30),1,30);

                addPadding(pack,256);
                try {
                    serverWrite.println(pack); serverWrite.flush();
                } 
                catch (Exception ee) {
                    System.out.println("FUCK Send!");
                }

                File file = new File("downloads/" + fname);
                try {
                    fileOutputStream = new FileOutputStream(file);
                } catch (Exception ee){}
                //System.out.println("fileMode " + fileMode);
            }
        }
    }

    public void updateCharArea() {
        if (friendList.getSelectedValue() != null) {
            targetName = friendList.getSelectedValue().toString();
            for (User user : myFriends) if (user.name.equals(targetName)) {
                textArea.setText(user.msg);
                textArea.repaint();
            }
        }
    }

    public void buildLoginFrame() {
        try {
            serverSocket = new Socket("127.0.0.1", 15636);
            
            serverWrite = new PrintWriter(serverSocket.getOutputStream());
            serverIn = serverSocket.getInputStream();

            ListenThread listenThread = new ListenThread();
            listenThread.start();
        } catch (Exception error) {}
        // 创建 JFrame 实例
        loginFrame = new JFrame("Doge's WeChat");
        // Setting the width and height of frame
        loginFrame.setSize(350, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* 创建面板，这个类似于 HTML 的 div 标签
         * 我们可以创建多个面板并在 JFrame 中指定位置
         * 面板中我们可以添加文本字段，按钮及其他组件。
         */
        JPanel panel = new JPanel();    
        // 添加面板
        loginFrame.add(panel);
        /* 
         * 调用用户定义的方法并添加组件到面板
         */
        placeLoginComponents(panel);

        // 设置界面可见
        loginFrame.setVisible(true); 
    }

    public void buildChartFrame() {
        chartFrame = new JFrame(myName + "'s weChat");
        targetName = myName;
        chartFrame.setBounds(100, 100, 541, 300);
        chartFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chartFrame.getContentPane().setLayout(null);
        
        //TextField textField = new TextField();
        sendText.setBounds(126, 224, 288, 30);
        chartFrame.getContentPane().add(sendText);
        
        friendList.setBounds(75, 66, 101, 134);
        FriendListUpdateListener friendListUpdateListener = new FriendListUpdateListener();
        friendList.addListSelectionListener(friendListUpdateListener);
        
        chartFrame.getContentPane().add(friendList);
        
        JButton btnSend = new JButton("Send");
        SendButtonListener sendButtonListener = new SendButtonListener();
        btnSend.addActionListener(sendButtonListener);
        
        btnSend.setBounds(431, 224, 76, 30);
        chartFrame.getContentPane().add(btnSend);
        
        //TextArea textArea = new TextArea();
        
        textArea.setBounds(193, 44, 233, 156);
        chartFrame.getContentPane().add(textArea);
        
        JLabel lblFriends = new JLabel("friends");
        lblFriends.setBounds(99, 43, 70, 15);
        chartFrame.getContentPane().add(lblFriends);
        
        JLabel lblNewLabel = new JLabel("MSG:");
        lblNewLabel.setBounds(75, 224, 46, 30);
        chartFrame.getContentPane().add(lblNewLabel);
        
        JButton btnAdd = new JButton("+");
        AddButtonListener addButtonListener = new AddButtonListener();
        btnAdd.addActionListener(addButtonListener);

        btnAdd.setBounds(27, 175, 46, 25);
        chartFrame.getContentPane().add(btnAdd);
        
        JButton btnSendf = new JButton("file");

        SendfButtonListener sendfButtonListener = new SendfButtonListener();
        btnSendf.addActionListener(sendfButtonListener);

        btnSendf.setBounds(431, 187, 76, 25);
        chartFrame.getContentPane().add(btnSendf);

        chartFrame.setVisible(true);
    }

    private void buildAddFriendsFrame() {
        addFriendsFrame = new JFrame();
        addFriendsFrame.setBounds(100, 100, 297, 244);
        addFriendsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addFriendsFrame.getContentPane().setLayout(null);
        
        searchFriend = new JTextField();
        searchFriend.setBounds(28, 64, 163, 26);
        addFriendsFrame.getContentPane().add(searchFriend);
        searchFriend.setColumns(10);
        
        searchResult = new JLabel("User Not Found");
        searchResult.setBounds(43, 121, 134, 15);
        addFriendsFrame.getContentPane().add(searchResult);
        
        JButton btnSearch = new JButton("search");
        btnSearch.setBounds(195, 64, 90, 25);
        SearchButtonListener searchButtonListener = new SearchButtonListener();
        btnSearch.addActionListener(searchButtonListener);

        addFriendsFrame.getContentPane().add(btnSearch);
        
        JButton btnAddFriend = new JButton("Add");
        AddFriendButtonListener addFriendButtonListener = new AddFriendButtonListener();
        btnAddFriend.addActionListener(addFriendButtonListener);
        btnAddFriend.setBounds(38, 148, 117, 25);
        addFriendsFrame.getContentPane().add(btnAddFriend);
        addFriendsFrame.setVisible(true);

    }

    private void buildFileFrame() {
        fileFrame = new JFrame();
        fileFrame.setBounds(100, 100, 450, 300);
        fileFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fileFrame.getContentPane().setLayout(null);
        
        recvFileList = new JList();
        recvFileList.setBounds(48, 39, 127, 135);
        fileFrame.getContentPane().add(recvFileList);
        
        JButton btnReceive = new JButton("receive");
        btnReceive.setBounds(62, 219, 117, 25);
        ReceiveButtonListener receiveButtonListener = new ReceiveButtonListener();
        btnReceive.addActionListener(receiveButtonListener);

        fileFrame.getContentPane().add(btnReceive);
        
        btnBrowse = new JFileChooser();
        btnBrowse.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES );
        btnBrowse.showDialog(new JLabel(), "Choose");
        btnBrowse.setBounds(189, 26, 221, 148);
        fileFrame.getContentPane().add(btnBrowse);
        
        JButton btnSendFile = new JButton("sendFile");

        SendFileButtonListener sendFileButtonListener = new SendFileButtonListener();
        btnSendFile.addActionListener(sendFileButtonListener);

        btnSendFile.setBounds(280, 232, 117, 25);
        fileFrame.getContentPane().add(btnSendFile);
        fileFrame.setVisible(true);
    }
    private void placeLoginComponents(JPanel panel) {

        /* 布局部分我们这边不多做介绍
         * 这边设置布局为 null
         */
        panel.setLayout(null);

        // 创建 JLabel
        JLabel userLabel = new JLabel("User:");
        /* 这个方法定义了组件的位置。
         * setBounds(x, y, width, height)
         * x 和 y 指定左上角的新位置，由 width 和 height 指定新的大小。
         */
        userLabel.setBounds(10,20,80,25);
        panel.add(userLabel);

        /* 
         * 创建文本域用于用户输入
         */
        userText = new JTextField(20);
        userText.setBounds(100,20,165,25);
        panel.add(userText);

        // 输入密码的文本域
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(10,50,80,25);
        panel.add(passwordLabel);

        /* 
         *这个类似用于输入的文本域
         * 但是输入的信息会以点号代替，用于包含密码的安全性
         */
        passwordText = new JPasswordField(20);
        passwordText.setBounds(100,50,165,25);
        panel.add(passwordText);

        // 创建登录按钮
        JButton loginButton = new JButton("login");
        loginButton.setBounds(10, 80, 80, 25);
        LoginButtonListener loginButtonListener = new LoginButtonListener();
        loginButton.addActionListener(loginButtonListener);
        panel.add(loginButton);
    }

}

public class ClientMain {
    public static void main(String[] args) {
        
        Client client = new Client();
        client.buildLoginFrame();
        //client.buildChartFrame();
    }
}