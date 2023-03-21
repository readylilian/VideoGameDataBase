import com.jcraft.jsch.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class PostgresSSH {

    public static void main(String[] args) throws SQLException {

        int lport = 5432;
        String rhost = "starbug.cs.rit.edu";
        int rport = 5432;
        String user = null; //change to your username
        String password = null; //change to your password
        String databaseName = "p320_33"; //change to your database name
        String fileName = "src/SSHLogin";

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(" ");
                if (tokens.length == 2) {
                    user = tokens[0];
                    password = tokens[1];
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (user != null && password != null) {
            System.out.println("Username: " + user);
            System.out.println("Password: " + password);
        } else {
            System.out.println("Username and/or password not found in file");
        }

        String driverName = "org.postgresql.Driver";
        Connection conn = null;
        Session session = null;
        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            session = jsch.getSession(user, rhost, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.setConfig("PreferredAuthentications","publickey,keyboard-interactive,password");
            session.connect();
            System.out.println("Connected");
            int assigned_port = session.setPortForwardingL(lport, "localhost", rport);
            System.out.println("Port Forwarded");

            // Assigned port could be different from 5432 but rarely happens
            String url = "jdbc:postgresql://localhost:"+ assigned_port + "/" + databaseName;

            System.out.println("database Url: " + url);
            Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);

            Class.forName(driverName);
            conn = DriverManager.getConnection(url, props);
            System.out.println("Database connection established");

            // Do something with the database....
            // Call main method here of application class or something
            Application application = new Application(conn);
            //The below commented out statements are an example of how to access a table with mixed case, and how to
            //display each row returned by a query.
//            Statement st = conn.createStatement();
//            ResultSet res = st.executeQuery("select * from \"Test\"");
//            while(res.next()){
//                for(int i = 1; i <= res.getMetaData().getColumnCount(); i++){
//                    System.out.print(res.getString(i) + " ");
//                }
//                System.out.println();
//            }
            application.init();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Closing Database Connection");
                conn.close();
            }
            if (session != null && session.isConnected()) {
                System.out.println("Closing SSH Connection");
                session.disconnect();
            }
        }
    }

}