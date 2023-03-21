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

    public void init(){
        //login first
        this.currentUser = login();

        //Start processing commands
        boolean quit = true;
        while(quit){
            String nextCmd = scanner.nextLine();
            quit = commandParser(nextCmd);

        }
    }

    private boolean commandParser(String cmd){
//        cmd = cmd.toLowerCase(Locale.ROOT);
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
        if(cmd.equals("remove_friend")){
            if(cmdArgs.size() != 2){
                System.out.println("Usage: remove_friend <username of friend you would like to remove>");
            }
            else{
                removeFriend(cmdArgs.get(1));
            }
        }
        if(cmd.equals("search_friends")){
            if(cmdArgs.size() != 2){
                System.out.println("Usage: search_friends <email to search>");
            }
            else{
                searchFriends(cmdArgs.get(1));
            }
        }
        if(cmd.equals("rate_game")){
            if(cmdArgs.size() != 3){
                System.out.println("Usage: rate_game <video game> <star rating: 1-5>");
            } else {
                int rating = Integer.parseInt(cmdArgs.get(2));
                rate_game(cmdArgs.get(1), rating);
            }
        }
        return true;
    }

    private void addFriend(String username){
        try{
            Statement st = this.conn.createStatement();
            st.executeUpdate("insert into friends_with values (" + this.currentUser + ", "+username+")");
            st.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either that user does not exist, " +
                    "or another error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private void removeFriend(String username){
        try{
            Statement st = this.conn.createStatement();
            st.executeUpdate("delete from friends_with where UID like " + this.currentUser +
                    " and FID like " + username);
            st.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either you aren not friends with that user, " +
                    "or another error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private void searchFriends(String email){
        try{
            Statement st = this.conn.createStatement();
            ResultSet res = st.executeQuery("select username from user where email like %" + email + "%");
            System.out.println("Users with emails that match your search:");
            printResultSet(res);
            st.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either you aren not friends with that user, " +
                    "or another error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private int getResultSetRowCount(ResultSet res) throws SQLException {
        int size = 0;
        while (res.next()) {
            size++;
        }
        return size;
    }

    private void printResultSet(ResultSet res) throws SQLException{
        while(res.next()){
            for(int i = 1; i <= res.getMetaData().getColumnCount(); i++){
                System.out.print(res.getString(i) + " ");
            }
            System.out.println();
        }
    }

    private void rate_game(String game, int rating){
        if(!(rating <= 5 && rating >= 1)){
            System.out.println("Please enter a valid rating number 1-5.");
        } else {
            try {
                Statement st = this.conn.createStatement();
                st.executeUpdate("update rates set rating = " + rating + "where username = "
                    + this.currentUser + " and where title = " + game);
                st.close();
            } catch (SQLException e) {
                System.out.println("We are sorry, something went wrong. You may not have that video game " +
                        "or something else went wrong. Please see error output for more detail");
                System.err.println(e.getMessage());
            }
        }
    }
}
