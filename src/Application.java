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

    PreparedStatement st;
    ResultSet res;


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
        if(cmd.equals("search_game")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game <game title to search>");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 0);
            }
        }
        if(cmd.equals("search_game_by_platform")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game <platform to search>");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 1);
            }
        }
        if(cmd.equals("search_game_by_release_date")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game <release date to search>");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 2);
            }
        }
        if(cmd.equals("search_game_by_developer")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game <developer to search>");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 3);
            }
        }
        if(cmd.equals("search_game_by_price")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game <price to search>");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 4);
            }
        }
        if(cmd.equals("search_game_by_genre")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game <genre to search>");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 5);
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

    //0 = Title
    //1 = Platform
    //2 = Release date
    //3 = Developers
    //4 = Price
    //5 = Genre
    private void searchGame(List<String> toSearch, int searchType)
    {
        //int vgId;
        ResultSet gameInfo;
        try {
            /*PreparedStatement gameInfoCall = this.conn.prepareStatement(
                    "select VG.title, platname, devname, pubname, playtime, VG.esrb_rating, star_rating from " +
                            "select title, esrb_rating from \"video_game\" as VG inner join" +
                            "select "
            );*/

            PreparedStatement gameInfoCall = this.conn.prepareStatement(
                "SELECT DISTINCT VG.title, plat.name, cdev.name, cpub.name, play.total_playtime, VG.esrb_rating FROM" +
                        " \"video_game\" as VG INNER JOIN" +
                        " \"develops\" as dev ON VG.vg_id = dev.vg_id INNER JOIN" +
                        " \"creator\" as cdev ON cdev.creator_id = dev.creator_id INNER JOIN" +
                        " \"publishes\" as pub ON VG.vg_id = pub.vg_id INNER JOIN" +
                        " \"creator\" as cpub ON cpub.creator_id = pub.creator_id INNER JOIN" +
                        " \"plays\" as play ON VG.vg_id = play.vg_id INNER JOIN" +
                        " \"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id INNER JOIN" +
                        " \"platform\" as plat ON plat.platform_id = vgplat.platform_id" +
                        " WHERE VG.vg_id = ? AND plat.platform_id = vgplat.platform_id"
            );

            switch (searchType) {
                case 0:
                        StringBuilder search = new StringBuilder();
                        for (int i = 0; i < toSearch.size(); i++) {
                            search.append(toSearch.get(i));
                            if (i < toSearch.size() - 1) {
                                search.append(" ");
                            }
                        }
                        //String formattedEmail = "%"+toSearch+"%";
                        PreparedStatement st = this.conn.prepareStatement(
                                "select vg_id,title,esrb_rating from \"video_game\" where title like ?"
                        );
                        st.setString(1, search.toString());

                        ResultSet res = st.executeQuery();
                        if (res != null) {
                            res.next();
                            while (!res.isAfterLast()) {
                                //Set the vg_id
                                gameInfoCall.setInt(1, res.getInt("vg_id"));
                                gameInfo = gameInfoCall.executeQuery();
                                res.next();
                            }
                        }
                        System.out.println("Games that match your search:");
                        //printResultSet(res);
                        st.close();
                        gameInfoCall.close();
                    break;
            /*case 1:
                try{

                }
                catch (SQLException e)
                {
                    System.out.println("We are sorry, something went wrong. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
                break;
            case 2:
                try{

                }
                catch (SQLException e)
                {
                    System.out.println("We are sorry, something went wrong. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
                break;
            case 3:
                try{

                }
                catch (SQLException e)
                {
                    System.out.println("We are sorry, something went wrong. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
                break;
            case 4:
                try{

                }
                catch (SQLException e)
                {
                    System.out.println("We are sorry, something went wrong. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
                break;
            case 5:
                try{

                }
                catch (SQLException e)
                {
                    System.out.println("We are sorry, something went wrong. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
                break;*/
            }
        }
        catch (SQLException e)
        {
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
