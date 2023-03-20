import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.sql.*;

public class Application {
    private final Connection conn;
    Scanner scanner = new Scanner(System.in);
    private String currentUser;

    public Application(Connection conn){
        this.conn = conn;
    }

    public void init(){

        //login first
        this.currentUser = login();

        boolean quit = true;
        while(quit){
            String nextCmd = scanner.nextLine();
            quit = commandParser(nextCmd);

        }
    }

    private String login(){
        while(true){
            System.out.println("Please enter your username and password separated by a space");
            ArrayList<String> loginInfo = new ArrayList<>(List.of(scanner.nextLine().trim().split(" ")));
            if(loginInfo.size() != 2){
                System.out.println("Sorry, please try again");
            }
            try{
                Statement st = conn.createStatement();
                ResultSet res = st.executeQuery("select * from user where username like "
                        + loginInfo.get(0) + " and password like " + loginInfo.get(1));
                int rowCount = getResultSetRowCount(res);
                res.first();
                if(rowCount == 1){
                    return res.getString(1);
                } else{
                    System.out.println("Sorry, we couldn't find a user with " +
                            "that username and password. Please try again");
                }
            }
            catch (SQLException e){
                System.err.println(e.getMessage());
            }
        }

    }

    private boolean commandParser(String cmd){
        cmd = cmd.toLowerCase(Locale.ROOT);
        cmd = cmd.trim();
        ArrayList<String> cmdArgs = new ArrayList<>(List.of(cmd.split(" ")));
        cmd = cmdArgs.get(0);
        if(cmd.equals("q") || cmd.equals("quit")){
            return false;
        }
        if(cmd.equals("add_friend")){
            if(cmdArgs.size() != 2){
                System.out.println("Usage: add_friend <username of friend you want>");
            }
            else{
                addFriend(cmdArgs.get(1));
            }
        }
        return true;
    }

    private void addFriend(String username){
        try{
            Statement st = this.conn.createStatement();
            st.executeUpdate("insert into friends_with values (" + this.currentUser + ", "+username+")");
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either that user does not exist, " +
                    "or another error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }

    }

    int getResultSetRowCount(ResultSet res) throws SQLException {
        int size = 0;
        while (res.next()) {
            size++;
        }
        return size;
    }
}
