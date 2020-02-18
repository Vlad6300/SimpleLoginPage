import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.UUID;
public class LoginPage extends JFrame {
    JTextArea userIn = new JTextArea(1,20);
    JTextArea passwIn= new JTextArea(1,20);

    Connection connection;

    LoginPage (){
        setTitle("Login Page");
        setSize(800,600);
        GridLayout layout = new GridLayout(3,3);
        setLayout(layout);

        JTextArea userLable = new JTextArea("Username: ");
        JTextArea passLable = new JTextArea("Password: ");
        userLable.setEditable(false);
        passLable.setEditable(false);

        JButton logBut = new JButton("Log in");
        logBut.addActionListener(new ActionButton(this,0));
        JButton createAcc = new JButton("Create Account");
        createAcc.addActionListener(new ActionButton(this,1));

        JPanel user = new JPanel();
        user.add(userLable, BorderLayout.NORTH);
        user.add(userIn, BorderLayout.SOUTH);

        JPanel password = new JPanel();
        password.add(passLable, BorderLayout.CENTER);
        password.add(passwIn, BorderLayout.SOUTH);


        this.add(new JPanel());
        this.add(user);
        this.add(new JPanel());

        this.add(new JPanel());
        this.add(password);
        this.add(new JPanel());

        this.add(logBut);
        this.add(new JPanel());
        this.add(createAcc);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connection = connect("jdbc:mysql://localhost/user_pass","root","123456qwerty");
        if(connection==null){
            System.out.println("Error Connecting to Database!");
            throw new RuntimeException();
        }

        setVisible(true);
    }

    private Connection connect(String database, String dbuser, String dbpass){
        Connection conn;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(database,dbuser,dbpass);
        }catch (ClassNotFoundException e){
            System.out.println("Driver not found");
            return null;
        }
        catch (SQLException e){
            System.out.println("Failed to connect to database");
            return null;
        }
        return conn;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }



    class ActionButton implements ActionListener{
        LoginPage lp;
        boolean create;

        ActionButton(LoginPage page, int identifier){
            lp = page;
            if(identifier==1)
                create=true;
            else
                create=false;
        }
        public void actionPerformed(ActionEvent e){
            try{
                if(create){
                    String userInput = parseUserInput();
                    if(userInput==null){
                        return;
                    }

                    Statement st = lp.connection.createStatement();
                    ResultSet rs = st.executeQuery("SELECT username FROM Login WHERE username = \""+userInput+"\";");
                    if(rs.next()){
                        JOptionPane.showMessageDialog(null,"Username taken","Error",JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String salt = UUID.randomUUID().toString();
                    salt = salt.replaceAll("-","");

                    String passInput = lp.passwIn.getText();
                    if(passInput.contains(" ")){
                        JOptionPane.showMessageDialog(null,"Password may not contain spaces","Error",JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if(passInput.length()>=50){
                        JOptionPane.showMessageDialog(null,"Password may not be larger than 50 characters","Error",JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    passInput = parsePassInput(passInput, salt);


                    st = lp.connection.createStatement();
                    st.executeUpdate("INSERT INTO Login VALUES(\""+userInput+"\",\""+salt+"\",\""+passInput+"\");");
                }else{
                    String userInput  = parseUserInput();
                    if(userInput==null){
                        return;
                    }
                    String errorMessage = new String();
                    Statement st = lp.connection.createStatement();
                    ResultSet rs = st.executeQuery("SELECT username FROM Login WHERE username = \""+userInput+"\";");
                    String passInput = lp.passwIn.getText();

                    if(!rs.next()||passInput.contains(" ")){
                        errorMessage +="Wrong username password combination\n";
                    }else{
                        st=lp.connection.createStatement();
                        rs = st.executeQuery(" SELECT salt FROM login WHERE username=\""+userInput+"\"; ");
                        rs.next();
                        passInput = parsePassInput(passInput,rs.getString(1));
                        if(passInput==null){
                            return;
                        }
                        st=lp.connection.createStatement();
                        rs = st.executeQuery(" SELECT username FROM login WHERE username=\""+userInput+"\" AND hash=\""+passInput+"\"; ");
                        if(!rs.next()&& errorMessage.isEmpty()){
                            errorMessage +="Wrong username password combination\n";
                        }
                    }
                    if(!errorMessage.isEmpty()){
                        JOptionPane.showMessageDialog(null,errorMessage,"Error",JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    System.out.println("You have been logged in");

                    //Code to start app goes here.



                }
            }catch (SQLException f){
                System.out.println("Database Error");
            }
        }

        private String parseUserInput(){
            String userInput = lp.userIn.getText().trim();
            if(!userInput.equals(userInput.replaceAll("[^a-zA-Z0-9]+",""))){
                JOptionPane.showMessageDialog(null,"Username may not contain special characters","Error",JOptionPane.ERROR_MESSAGE);
                return null;
            }
            if(userInput.isEmpty()){
                JOptionPane.showMessageDialog(null,"Username may not be empty","Error",JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return userInput;
        }

        private String parsePassInput(String passInput, String salt){
            if(passInput.isEmpty()){
                JOptionPane.showMessageDialog(null,"Password can't be empty","Error",JOptionPane.ERROR_MESSAGE);
                return null;
            }
            passInput += "CE291";

            passInput+=salt;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(passInput.getBytes(StandardCharsets.UTF_8));
                passInput = bytesToHex(hash);
            }catch (NoSuchAlgorithmException f){
                JOptionPane.showMessageDialog(null,"Unable to encrypt password","Error",JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return passInput;
        }

    }

}

