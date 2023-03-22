import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Application {
    private final Connection conn;
    Scanner scanner = new Scanner(System.in);
    private String currentUser;

    public Application(Connection conn){
        this.conn = conn;
    }



    private String login(){
        while(true){
            try{
                System.out.println("Please enter your username and password separated by a space\n" +
                        "OR create an account by entering \"create\"");
                ArrayList<String> loginInfo = new ArrayList<>(List.of(scanner.nextLine().trim().split(" ")));
                if(loginInfo.size() != 2){
                    if(loginInfo.size() == 1){
                        if(loginInfo.get(0).equals("create")){
                            return createAccount();
                        }
                        else{
                            System.out.println("Sorry, please try again");
                        }
                    }
                    else {
                        System.out.println("Sorry, please try again");
                    }
                }
                else{
                    //st must be scrollable to use getRows and still reset to the beginning
                    //st must be updatable if you must use it to update a table
                    Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    ResultSet res = st.executeQuery("select * from \"user\" where \"username\" like "
                            + "'" + loginInfo.get(0) + "'" + " and \"password\" like " + "'" + loginInfo.get(1) + "'");
                    int rowCount = getResultSetRowCount(res);
                    res.first(); //reset the result iterator to the first row
                    if(rowCount == 1){
                        String username = res.getString(1);
                        st.executeUpdate("update \"user\" set \"last_access_date\" = '" + getCurrentDateTime() + "' where " +
                                "\"username\" like '" + username + "'");
                        st.close();
                        return username;
                    } else{
                        System.out.println("Sorry, we couldn't find a user with " +
                                "that username and password. Please try again");
                    }
                    st.close();
                }

            }
            catch (SQLException e){
                System.err.println(e.getMessage());
            }
        }

    }

    private String createAccount(){
        while(true) {
            System.out.println("Please enter a username, password,  email, first name, and last name separated by spaces");
            ArrayList<String> accountInfo = new ArrayList<>(List.of(scanner.nextLine().trim().split(" ")));
            if (accountInfo.size() != 5) {
                System.out.println("Sorry, please try again");
            } else {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("insert into \"user\" values('" + accountInfo.get(0) + "', '" + accountInfo.get(1) + "', '"
                            + accountInfo.get(2) + "', '" + getCurrentDateTime() + "', '" + getCurrentDateTime() + "', '" +
                            accountInfo.get(3) + "', '" + accountInfo.get(4) + "')");
                    return accountInfo.get(0);
                } catch (SQLException e) {
                    System.out.println("We are sorry, something went wrong. Either that user is already in use, " +
                            "or another error occurred. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public void init(){
        //login first
        this.currentUser = login();
        System.out.println("Welcome to Polybius");

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
            System.out.println("Hope you enjoyed!");
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
        if(cmd.equals("create_collection")){
            if(cmdArgs.size() != 2){
                System.out.println("Usage: create_collection <name of collection>");
            }
            else{
                createCollection(cmdArgs.get(1));
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

    private void createCollection(String name) {
        try{
            //check for duplicate collection
            PreparedStatement pst = conn.prepareStatement("select * from collection where username = ? and name = ?");
            pst.setString(1, this.currentUser);
            pst.setString(2, name);
            ResultSet res = pst.executeQuery();
            if(!res.next()) {
                // calculate next collection id
                Statement st = this.conn.createStatement();
                res = st.executeQuery("select collection_id from collection order by collection_id desc limit 1;");
                if (res.next()) {
                    int id = 1 + res.getInt("collection_id");
                    // insert the new collection
                    String query = "insert into collection (collection_id, username, name) VALUES (?, ?, ?)";
                    PreparedStatement statement = conn.prepareStatement(query);
                    statement.setInt(1, id);
                    statement.setString(2, this.currentUser);
                    statement.setString(3, name);
                    statement.executeUpdate();
                    System.out.println("New Collection " + name + " created!");
                }
            } else {
                System.err.println("Collection with the name " + name + " already exists.");
            }
        }
        catch (SQLException e) {
            System.out.println("We are sorry, something went wrong. Please see error output for more detail");
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

    private String getCurrentDateTime(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
}
