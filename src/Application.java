import org.postgresql.gss.GSSOutputStream;

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
            if(cmdArgs.size() < 2){
                System.out.println("Usage: create_collection <name of collection>");
            }
            else{
                createCollection(cmdArgs.subList(1, cmdArgs.size()));
            }

        }
        if(cmd.equals("add_to_collection")){
            if(cmdArgs.size() <= 2){
                System.out.println("Usage: add_to_collection [<game name>] [<collection name>]");
            }
            else {
                addToCollection(cmdArgs.subList(1, cmdArgs.size()));
            }
        }
        if(cmd.equals("delete_from_collection")){
            if(cmdArgs.size() <= 2){
                System.out.println("Usage: delete_from_collection [<game name>] [<collection name>]");
            }
            else{
                deleteFromCollection(cmdArgs.subList(1, cmdArgs.size()));
            }
        }
        return true;
    }

    private void addFriend(String username){
        try{
            String checkFriendQuery = "select * from friends_with where uid like ? and fid like ?";
            String addFriendUpdate = "insert into friends_with values (?, ?)";
            PreparedStatement checkFriend = conn.prepareStatement(checkFriendQuery);
            checkFriend.setString(1, currentUser);
            checkFriend.setString(2, username);
            ResultSet res = checkFriend.executeQuery();
            if(!res.next()){
                PreparedStatement addFriend = conn.prepareStatement(addFriendUpdate);
                addFriend.setString(1, currentUser);
                addFriend.setString(2, username);
                addFriend.executeUpdate();
                System.out.println("Success! You are now friends with " + username);
                addFriend.close();
            }
            else{
                System.out.println("Sorry, you are already friends with that user");
            }
            checkFriend.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either that user does not exist, " +
                    "or another error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private void removeFriend(String username){
        try{
            String checkFriendQuery = "select * from friends_with where uid like ? and fid like ?";
            String removeFriendUpdate = "delete from friends_with where uid like ? and fid like ?";
            PreparedStatement checkFriend = conn.prepareStatement(checkFriendQuery,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            checkFriend.setString(1, currentUser);
            checkFriend.setString(2, username);
            ResultSet res = checkFriend.executeQuery();
            if(res.first()){
                PreparedStatement removeFriend = conn.prepareStatement(removeFriendUpdate);
                removeFriend.setString(1, currentUser);
                removeFriend.setString(2, username);
                removeFriend.executeUpdate();
                System.out.println("Success! You are no longer friends with " + username);
                removeFriend.close();
            }
            else{
                System.out.println("Sorry, you aren't friends with that user");
            }
            checkFriend.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either you aren not friends with that user, " +
                    "or another error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private void searchFriends(String email){
        try{
            String formattedEmail = "%"+email+"%";
//            PreparedStatement st = this.conn.prepareStatement("select \"username\" from \"user\" where \"email\" like '%'?'%'");
            PreparedStatement st = this.conn.prepareStatement("select username from \"user\" where email like ?");
            st.setString(1, formattedEmail);

            ResultSet res = st.executeQuery();
            System.out.println("Users with emails that match your search:");
            printResultSet(res);
            st.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private void createCollection(List<String> nameList) {
        try{
            //check for duplicate collection
            StringBuilder name = new StringBuilder();
            for(int i = 0; i < nameList.size(); i++){
                name.append(nameList.get(i));
                if(i < nameList.size()-1){
                    name.append(" ");
                }
            }
            PreparedStatement pst = conn.prepareStatement("select * from collection where username = ? and name = ?");
            pst.setString(1, this.currentUser);
            pst.setString(2, name.toString());
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
                    statement.setString(3, name.toString());
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

    private void addToCollection(List<String> args){
        String[] names = parseAddDeleteToCollection(args);
        String gameName = names[0];
        String collectionName = names[1];
        try{
            PreparedStatement queryCollectionExists = conn.prepareStatement("select collection_id from collection " +
                    "where username like ? and name like ?");
            queryCollectionExists.setString(1, currentUser);
            queryCollectionExists.setString(2, collectionName);
            ResultSet res = queryCollectionExists.executeQuery();
            if(res.next()){ //check if collection exists
                int collection_id = res.getInt("collection_id");
                PreparedStatement queryGameExists = conn.prepareStatement("select vg_id from video_game " +
                        "where title like ?");
                queryGameExists.setString(1, gameName);
                res = queryGameExists.executeQuery();
                if(res.next()){ //check if game exists
                    int vg_id = res.getInt("vg_id");
                    //if we get here, game and collection exist
                    //must check if game is already in collection
                    PreparedStatement checkDuplicate = conn.prepareStatement("(select * from collection_contains" +
                            " where collection_id = ? and vg_id = ?)");
                    checkDuplicate.setInt(1, collection_id);
                    checkDuplicate.setInt(2, vg_id);
                    ResultSet exists = checkDuplicate.executeQuery();
                    if(!exists.next()){
                        //Now we need to check if that game is on a system they own or not, and warn them if not
                        PreparedStatement onOwnedSystem = conn.prepareStatement("select * from " +
                                "\"video_game_on/has_platform\" as vp join \"user_has/owns_platform\" as up " +
                                "on vp.platform_id = up.platform_id where " +
                                "vp.vg_id = ? and up.username like ?");
                        onOwnedSystem.setInt(1, vg_id);
                        onOwnedSystem.setString(2, currentUser);
                        ResultSet systemOwned = onOwnedSystem.executeQuery();
                        if(systemOwned.next()){
                            PreparedStatement addToCollection = conn.prepareStatement("insert into " +
                                    "collection_contains values (?, ?)");
                            addToCollection.setInt(1, collection_id);
                            addToCollection.setInt(2, vg_id);
                            addToCollection.executeUpdate();
                            System.out.println("Success, added " + gameName + " to " + collectionName);
                        } else {
                            while(true){
                                System.out.println("WARNING: you do not own any systems that this game can run on." +
                                        "\n Would you still like to add it? (y/n)");
                                String response = scanner.nextLine();
                                if(response.toLowerCase(Locale.ROOT).charAt(0) == 'y'){
                                    PreparedStatement addToCollection = conn.prepareStatement("insert into collection_contains " +
                                            "values (?, ?)");
                                    addToCollection.setInt(1, collection_id);
                                    addToCollection.setInt(2, vg_id);
                                    addToCollection.executeUpdate();
                                    System.out.println("Success, added " + gameName + " to " + collectionName);
                                    break;
                                } else if(response.toLowerCase(Locale.ROOT).charAt(0) == 'n'){
                                    System.out.println("Ok, we won't add it!");
                                    break;
                                }
                            }
                        }
                    } else{
                        System.out.println("Sorry, that game is already in that collection");
                    }

                } else{
                    System.out.println("Sorry, that game does not exist");
                }
                queryGameExists.close();
            } else{
                System.out.println("Sorry, that collection does not exist");
            }
            queryCollectionExists.close();
        } catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Either that game or collection does not exist, or" +
                    " an internal error occurred. Please see error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private void deleteFromCollection(List<String> args){
        String[] names = parseAddDeleteToCollection(args);
        String gameName = names[0];
        String collectionName = names[1];
        try{
            PreparedStatement queryCollectionExists = conn.prepareStatement("select collection_id from collection " +
                    "where username like ? and name like ?");
            queryCollectionExists.setString(1, currentUser);
            queryCollectionExists.setString(2, collectionName);
            ResultSet res = queryCollectionExists.executeQuery();
            if(res.next()) { //check if collection exists
                int collection_id = res.getInt("collection_id");
                PreparedStatement queryGameExists = conn.prepareStatement("select vg_id from video_game " +
                        "where title like ?");
                queryGameExists.setString(1, gameName);
                res = queryGameExists.executeQuery();
                if (res.next()) { //check if game exists
                    int vg_id = res.getInt("vg_id");
                    //if we get here, game and collection exist
                    //must check if game is already in collection
                    PreparedStatement checkDuplicate = conn.prepareStatement("(select * from collection_contains" +
                            " where collection_id = ? and vg_id = ?)");
                    checkDuplicate.setInt(1, collection_id);
                    checkDuplicate.setInt(2, vg_id);
                    ResultSet exists = checkDuplicate.executeQuery();
                    if(exists.next()) { //if it is in the collection, remove it
                        PreparedStatement deleteGame = conn.prepareStatement("delete from collection_contains " +
                                "where collection_id = ? and vg_id = ?");
                        deleteGame.setInt(1, collection_id);
                        deleteGame.setInt(2, vg_id);
                        deleteGame.executeUpdate();
                        System.out.println("Successfully delete " + gameName + " from " + collectionName);
                    } else {
                        System.out.println("Sorry, " + gameName +" is not in " + collectionName);
                    }
                    checkDuplicate.close();
                } else {
                    System.out.println("Sorry, that game does not exist");
                }
                queryGameExists.close();
            } else{
                System.out.println("Sorry, you have no such collection");
            }
            queryCollectionExists.close();
        } catch (SQLException e){
            System.out.println("error");
            System.err.println(e.getMessage());
        }
    }

    private String[] parseAddDeleteToCollection(List<String> args){
        StringBuilder both = new StringBuilder();
        for(int i = 0; i < args.size(); i++){
            both.append(args.get(i));
            if(i != args.size()-1){
                both.append(" ");
            }
        }
        String[] split = both.toString().split("] \\[");

        split[0] = split[0].substring(1);
        split[1] = split[1].substring(0, split[1].length()-1);
        return split;
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
