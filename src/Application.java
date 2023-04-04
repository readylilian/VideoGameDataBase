import java.sql.Date;
import java.text.SimpleDateFormat;
import javax.xml.transform.Result;
import java.util.*;
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
    ResultSet res = null;
    ResultSet gameInfo;
    List<ResultSet> allGames = new ArrayList<>();
    String lastTitle = "";
    Integer lastId = -1;
    StringBuilder search;
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
                System.out.println("Usage: search_game [<game title to search>] [<a/d>_<sort_type>]");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 0);
            }
        }
        if(cmd.equals("search_game_by_platform")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game_by_platform [<platform to search>] [<a/d>_<sort_type>]");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 1);
            }
        }
        if(cmd.equals("search_game_by_release_date")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game_by_release_date [<YYYY-MM-DD>] [<a/d>_<sort_type>]");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 2);
            }
        }
        if(cmd.equals("search_game_by_developer")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game_by_developer [<developer to search>] [<a/d>_<sort_type>]");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 3);
            }
        }
        if(cmd.equals("search_game_by_price")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game_by_price [<price to search>] [<a/d>_<sort_type>]");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 4);
            }
        }
        if(cmd.equals("search_game_by_genre")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: search_game_by_genre [<genre to search>] [<a/d>_<sort_type>]");
            }
            else{
                searchGame(cmdArgs.subList(1, cmdArgs.size()), 5);
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
        if(cmd.equals("list_collections")) {
            if (cmdArgs.size() != 1) {
                System.out.println("Usage: list_collections");
            } else {
                listCollections();
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

        if(cmd.equals("modify_collection")){
            if(cmdArgs.size() <= 2){
                System.out.println("Usage: modify_collection [<old colllection name>] [<new collection name>]");
            }
            else{
                modifyCollection(cmdArgs.subList(1, cmdArgs.size()));
            }
        }
        if(cmd.equals("delete_collection")){
            if(cmdArgs.size() < 2){
                System.out.println("Usage: delete_collection <collection name>");
            }
            else{
                deleteCollection(cmdArgs.subList(1, cmdArgs.size()));
            }
        }

        if (cmd.equals("play_game_random")) {
            if (cmdArgs.size() < 2) {
                System.out.print("Usage: play_game_random <collection name>");
            } else {
                try {
                    List<String> col_name = cmdArgs.subList(1, cmdArgs.size());

                    // get collection name as String!
                    StringBuilder name = new StringBuilder();
                    for (int i = 0; i < col_name.size(); i++) {
                        name.append(col_name.get(i));
                        if (i < col_name.size() - 1) {
                            name.append(" ");
                        }
                    }

                    String collection_name_string = name.toString();

                    try {
                        String vg_name_string = randomVideoGameFromCollection(collection_name_string);

                        play_game(vg_name_string);

                    } catch (SQLException e) {
                        System.out.println(
                                "Something went wrong! This collection might not" +
                                        "exist, or an internal error has occurred. See error output for more detail");
                        if(e.getMessage() != null){System.err.println(e.getMessage());}
                    }
                }catch(NumberFormatException nfe) {
                    System.out.println(
                            "Usage: play_game_random <collection name>" +
                                    " Please see error output for more detail");
                    System.err.println(nfe.getMessage());

                }
            }
        }

        if (cmd.equals("play_game")) {
            if (cmdArgs.size() < 2) {
                System.out.print("Usage: play_game <video game title>");
            } else {
                try {
                    List<String> vg_name = cmdArgs.subList(1, cmdArgs.size());

                    // get video game name as String! This is what is fed to play_game function!
                    StringBuilder name = new StringBuilder();
                    for (int i = 0; i < vg_name.size(); i++) {
                        name.append(vg_name.get(i));
                        if (i < vg_name.size() - 1) {
                            name.append(" ");
                        }
                    }

                    String vg_name_string = name.toString();

                    play_game(vg_name_string);

                } catch (NumberFormatException nfe) {
                    System.out.println(
                            "Usage: play_game <video game title>" +
                            " Please see error output for more detail");
                    System.err.println(nfe.getMessage());
                }
            }
        }

        if(cmd.equals("rate_game")){
            if(cmdArgs.size() < 3){
                System.out.println("Usage: rate_game <star rating: 1-5> <video game title>");
            } else {
                int rating;
                try {
                    rating = Integer.parseInt(cmdArgs.get(1));
                    List<String> vg_name = cmdArgs.subList(2, cmdArgs.size());

                    StringBuilder name = new StringBuilder();
                    for(int i = 0; i < vg_name.size(); i++){
                        name.append(vg_name.get(i));
                        if(i < vg_name.size()-1){
                            name.append(" ");
                        }
                    }
                    rate_game(name.toString(), rating);
                } catch (NumberFormatException nfe) {
                    System.out.println("Please enter a number rating. Usage: rate_game <star rating: 1-5> <video game title>." +
                            " Please see error output for more detail");
                    System.err.println(nfe.getMessage());
                }
            }
        }

        if(cmd.equals("profile")){
            if(cmdArgs.size() != 2){
                System.out.println("Usage: profile <sort type: either playtime, rating, or both>");
            }
            else{
                view_profile(cmdArgs.get(1));
            }
        }

        if(cmd.toLowerCase(Locale.ROOT).equals("help")){
            System.out.println("""
                    Here are the commands you can use:
                    add_friend <username of friend you want>
                    remove_friend <username of friend you would like to remove>
                    search_friends <email to search>
                    create_collection <name of collection>
                    list_collections
                    add_to_collection [<game name>] [<collection name>]
                    delete_from_collection [<game name>] [<collection name>]
                    modify_collection [<old colllection name>] [<new collection name>]
                    delete_collection <collection name>
                    rate_game <star rating: 1-5> <video game title>
                    help - see this message again
                    search_game <game title to search>
                    search_game_by_platform <platform to search>
                    search_game_by_release_date <YYYY-MM-DD>
                    search_game_by_developer <developer to search>
                    search_game_by_price <price to search>
                    search_game_by_genre <genre to search>""");
        }
        return true;
    }

    // play_game creates a new row in the PLAYS table. The information in this
    // new row states that the current user has played the given game at the
    // current timestamp for <60> minutes.
    //
    // @param vg_name_string, a String, the name of the game this user is playing
    //
    private void play_game(String vg_name_string){
        String time_played_string = "60"; // everyone plays for an hour at a time
        try{
            // get vg id from vg name
            PreparedStatement query_vgID_from_name = conn.prepareStatement("select vg_id from video_game " + "where title like ?");
            query_vgID_from_name.setString(1, vg_name_string);
            ResultSet res = query_vgID_from_name.executeQuery();
            res.next();
            String vg_id_string = res.getString(1);
            //insert new row in PLAYS
            PreparedStatement query_insert_plays_row = conn.prepareStatement("insert into plays values('" + getCurrentDateTime() + "', '" + currentUser + "', '"
                    + vg_id_string + "', '" + time_played_string + "')");
            query_insert_plays_row.executeUpdate();
        }catch (SQLException e) {
            System.out.println(
                    "Something went wrong! An internal error has occurred. See error output for more detail");
            System.err.println(e.getMessage());
        }
    }

    private String randomVideoGameFromCollection(String collection_name) throws SQLException {
        String vg_name = "";
        PreparedStatement query_collection_exists = conn.prepareStatement("select collection_id from collection " + "where username like ? and name like ?");
        query_collection_exists.setString(1, currentUser);
        query_collection_exists.setString(2, collection_name);
        ResultSet res = query_collection_exists.executeQuery();
        if (res.next()) {
            String collection_id = res.getString(1); //retreive first column from result set (should be collection_id)?
            // count number of video games in the collection
            PreparedStatement get_number_video_games_from_collection_query = conn.prepareStatement("select count(*) from collection_contains where collection_id = " + collection_id);
            res = get_number_video_games_from_collection_query.executeQuery();
            res.next();
            int num_games_in_collection = res.getInt(1); //retreive first column from result set (should be collection_id)?
            // grab all videogames from collection
            PreparedStatement video_games_from_collection_query = conn.prepareStatement("select vg_id from collection_contains " + "where collection_id = " + collection_id);
            res = video_games_from_collection_query.executeQuery();
            // select random;
            // generate random number within range of total video games in collection

            int range = ( num_games_in_collection - 1)  + 1;
            int randomNum = (int)((Math.random() * range) + 1);
            // grab vg_id at randomized column location
            res.next();
            String random_vg_id_string = res.getString(randomNum);
            // get name of randomized video game.
            PreparedStatement video_game_name_from_id = conn.prepareStatement("select title from video_game where vg_id = " + random_vg_id_string);
            res = video_game_name_from_id.executeQuery();
            res.next();
            vg_name = res.getString(1);
        }
        else{
            throw new SQLException();
        }
        return vg_name;
    }

    private void addFriend(String username){
        try{
            PreparedStatement checkUserExists = conn.prepareStatement("select * from \"user\" where username like ?");
            checkUserExists.setString(1, username);
            ResultSet friendExists = checkUserExists.executeQuery();
            if(friendExists.next()){
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
            } else {
                System.out.println("Sorry, that user does not exist!");
            }
            checkUserExists.close();
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

    private void searchGame(List<String> toSearch, int searchType)
    {
        String[] terms = parseAddDeleteToCollection(toSearch);
        String search = terms[0];
        String sortTerm = terms[1];
        String vgSearchOrder = " ORDER BY VG.title ASC, vgplat.release_date ASC";
        String searchCall = "SELECT DISTINCT  VG.title, plat.name, cdev.name, cpub.name, VG.esrb_rating, VG.vg_id, " +
                "vgplat.release_date, vgplat.price, genre.genre_name  FROM" +
                " \"video_game\" as VG INNER JOIN" +
                " \"has_genre\" as genre ON VG.vg_id = genre.vg_id INNER JOIN" +
                " \"develops\" as dev ON VG.vg_id = dev.vg_id INNER JOIN" +
                " \"creator\" as cdev ON cdev.creator_id = dev.creator_id INNER JOIN" +
                " \"publishes\" as pub ON VG.vg_id = pub.vg_id INNER JOIN" +
                " \"creator\" as cpub ON cpub.creator_id = pub.creator_id INNER JOIN" +
                " \"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id INNER JOIN" +
                " \"platform\" as plat ON plat.platform_id = vgplat.platform_id" +
                " WHERE VG.vg_id = ? AND plat.platform_id = vgplat.platform_id ";
        try {
            switch (sortTerm) {
                //Default
                case "":
                    vgSearchOrder = " ORDER BY VG.title ASC";
                    break;
                case "a_title":
                    vgSearchOrder = " ORDER BY VG.title ASC";
                    break;
                case "d_title":
                    vgSearchOrder = " ORDER BY VG.title DESC";
                    break;
                case "a_price":
                    searchCall += " ORDER BY vgplat.price ASC";
                    break;
                case "d_price":
                    searchCall += " ORDER BY vgplat.price DESC";
                    break;
                case "a_genre":
                    searchCall += " ORDER BY genre.genre_name ASC";
                    break;
                case "d_genre":
                    searchCall += " ORDER BY genre.genre_name DESC";
                    break;
                case "a_year":
                    searchCall += " ORDER BY vgplat.release_date ASC";
                    break;
                case "d_year":
                    searchCall += " ORDER BY vgplat.release_date DESC";
                    break;
            }
            PreparedStatement gameInfoCall = this.conn.prepareStatement(searchCall);

            switch (searchType) {
                //Title
                case 0:
                        //Get the vg_id from the title
                        st = this.conn.prepareStatement(
                                "select VG.vg_id from \"video_game\" as VG INNER JOIN " +
                                        "\"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id" +
                                        " where title like ? " + vgSearchOrder
                        );
                        st.setString(1, "%" +search.toString() + "%");
                    break;
            //Platform
            case 1:
                    //Get the vg_id from the platform
                    st = this.conn.prepareStatement(
                            "select VG.vg_id from \"video_game\" as VG INNER JOIN " +
                                    "\"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id INNER JOIN" +
                                    "\"platform\" as plat ON vgplat.platform_id = plat.platform_id " +
                                    "where plat.name like ? " + vgSearchOrder
                    );
                    st.setString(1, "%" +search.toString() + "%");
                break;

            //Release Date
            case 2:
                //Get the vg_id from the release date
                st = this.conn.prepareStatement(
                        "select VG.vg_id from \"video_game\" as VG INNER JOIN " +
                                "\"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id"+
                                " where CAST(vgplat.release_date AS DATE) = ? " + vgSearchOrder
                );
                //st.setString(1, search.toString() + "%");
                st.setDate(1, Date.valueOf(search.toString() + ""));
                break;
            //Developers
            case 3:
            //Get the vg_id from the developer
                    st = this.conn.prepareStatement(
                            "select VG.vg_id from \"video_game\" as VG INNER JOIN" +
                                    "\"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id INNER JOIN" +
                                    "\"develops\" as dev ON VG.vg_id = dev.vg_id INNER JOIN" +
                                    "\"creator\" as cdev ON cdev.creator_id = dev.creator_id" +
                                    " WHERE cdev.name like ? " + vgSearchOrder
                    );
                    st.setString(1, "%" +search.toString() + "%");
                break;
            //Price
            case 4:
                //Get the vg_id from the price
                st = this.conn.prepareStatement(
                        "select VG.vg_id from \"video_game\" as VG INNER JOIN " +
                                "\"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id" +
                                " where vgplat.price = ? "+ vgSearchOrder
                );
                st.setInt(1, Integer.parseInt(search.toString()));
                break;
            //Genre
            case 5:
                //Get the vg_id from the genre
                st = this.conn.prepareStatement(
                        "select VG.vg_id from \"video_game\" as VG INNER JOIN " +
                                "\"video_game_on/has_platform\" as vgplat ON VG.vg_id = vgplat.vg_id INNER JOIN" +
                                " \"has_genre\" as genre ON VG.vg_id = genre.vg_id" +
                                " where genre.genre_name LIKE ? " + vgSearchOrder
                );
                st.setString(1, search.toString());
                break;
            }

            res = st.executeQuery();
            //If anything is returned, print it
            if(!res.wasNull())
            {
                System.out.println("Games that match your search:");
                System.out.printf("------------------------------------------------------------------------------------------" +
                        "---------------------------------------------------------------%n");
                System.out.printf("| %-40s | %-20s | %-20s | %-20s | %-10s | %-10s | %-11s |%n",
                        "Title", "Platforms", "Developers", "Publishers","Playtime","Age Rating", "Your Rating");
                //print out each game in a nice way
                while(res.next()) {
                    //Set the vg_id
                    if(lastId != res.getInt("vg_id"))
                    {
                        gameInfoCall.setInt(1, res.getInt("vg_id"));
                        printGameSearchResults(gameInfoCall.executeQuery());
                        lastId = res.getInt("vg_id");
                    }
                }
            }
            else
            {
                System.out.println("There are currently no games that match your search");
            }
            lastId = -1;
            gameInfoCall.close();
            st.close();
        }
        catch (SQLException e)
        {
            System.out.println("We are sorry, something went wrong. Please see error output for more detail");
            System.err.println(e.getMessage());
        }

    }
    private void printGameSearchResults(ResultSet res) throws SQLException{
        HashSet<String> seen_info = new HashSet<>();
        while(res.next()){

            //Format the initial strings
            String formattedPlay = "0:00";
            String reviewString = "No rating";
            String title = res.getString(1);
            if(title.length() > 40)
            {
                title = title.substring(0,40);
            }
            //If title hasn't been printed yet, print first row items
            if(!res.getString(1).equals(lastTitle)) {
                System.out.printf("--------------------------------------------------------------------------------" +
                        "-------------------------------------------------------------------------%n");
                //Check for review
                PreparedStatement reviewCheck = this.conn.prepareStatement(
                        "SELECT rating FROM rates WHERE username LIKE ? AND vg_id = ?");
                reviewCheck.setString(1, this.currentUser);
                reviewCheck.setInt(2, res.getInt("vg_id"));
                ResultSet reviews = reviewCheck.executeQuery();
                //Check for played
                PreparedStatement playCheck = this.conn.prepareStatement(
                        "SELECT total_playtime FROM plays WHERE username LIKE ? AND vg_id = ?");
                playCheck.setString(1, this.currentUser);
                playCheck.setInt(2, res.getInt("vg_id"));
                ResultSet plays = playCheck.executeQuery();

                if(reviews.next())
                {
                    reviewString = reviews.getString(1);
                }
                if(plays.next())
                {
                    formattedPlay = String.format("%d:%02d",(plays.getInt(1)/60),(plays.getInt(1)%60));
                }
                //Then print nicely and close the resultsets
                System.out.printf("| %-40s | %-20s | %-20s | %-20s | %-10s | %-10s | %-11s |%n",
                        title, res.getString(2), res.getString(3),
                        res.getString(4),formattedPlay, res.getString(5),
                        reviewString);

                playCheck.close();
                reviewCheck.close();
                lastTitle = res.getString(1);
                seen_info.add(res.getString(2));
                seen_info.add(res.getString(3));
                seen_info.add(res.getString(4));

            }
            //Otherwise print out items that can change for every row
            else
            {
                if(!seen_info.contains(res.getString(2)) && !seen_info.contains(res.getString(3)) && !seen_info.contains(res.getString(4))){
                    System.out.printf("| %-40s | %-20s | %-20s | %-20s | %-10s | %-10s | %-11s |%n",
                            "", res.getString(2), res.getString(3),
                            res.getString(4), "", "", "");
//                    System.out.println(seen_info);
                    seen_info.add(res.getString(2));
                    seen_info.add(res.getString(3));
                    seen_info.add(res.getString(4));
                } else if(!seen_info.contains(res.getString(2))){
                    System.out.printf("| %-40s | %-20s | %-20s | %-20s | %-10s | %-10s | %-11s |%n",
                            "", res.getString(2), "",
                            "", "", "", "");
                    seen_info.add(res.getString(2));
                } else if(!seen_info.contains(res.getString(3))){
                    System.out.printf("| %-40s | %-20s | %-20s | %-20s | %-10s | %-10s | %-11s |%n",
                            "", "", res.getString(3),
                            "", "", "", "");
                    seen_info.add(res.getString(3));
                } else if(!seen_info.contains(res.getString(4))){
                    System.out.printf("| %-40s | %-20s | %-20s | %-20s | %-10s | %-10s | %-11s |%n",
                            "", "", "",
                            res.getString(4), "", "", "");
                    seen_info.add(res.getString(4));
                }
            }
        }
        lastTitle = "";
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

    private void listCollections() {
        try {
            PreparedStatement pst = conn.prepareStatement(
                        "SELECT c.name AS collection_name, COALESCE(COUNT(cc.vg_id), 0) AS number_of_video_games," +
                                "CONCAT_WS(':', COALESCE(SUM(p.total_playtime) / 60, 0)," +
                                    "COALESCE(SUM(p.total_playtime) % 60), 0) AS total_play_time " +
                            "FROM COLLECTION c " +
                            "LEFT JOIN COLLECTION_CONTAINS cc ON c.collection_id = cc.collection_id " +
                            "LEFT JOIN PLAYS p ON cc.vg_id = p.vg_id AND c.username = p.username " +
                            "WHERE c.username like ? " +
                            "GROUP BY c.collection_id, c.name ORDER BY c.name ASC;",

                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            pst.setString(1, this.currentUser);
            ResultSet res = pst.executeQuery();
            if (!res.isBeforeFirst()) {
                System.out.println("No collections found!");
            } else {
                printResultSet(res);
            }

        } catch (SQLException e) {
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

    private void modifyCollection(List<String> args){
        String[] names = parseAddDeleteToCollection(args);
        String oldName = names[0];
        String newName = names[1];
        try {
            PreparedStatement queryCollectionExists = conn.prepareStatement("select collection_id from collection " +
                    "where username like ? and name like ?");
            queryCollectionExists.setString(1, currentUser);
            queryCollectionExists.setString(2, oldName);
            ResultSet res = queryCollectionExists.executeQuery();
            if (res.next()) { //check if collection exists
                int collection_id = res.getInt("collection_id");
                PreparedStatement updateName = conn.prepareStatement("update collection set name = ? " +
                        "where username = ? and collection_id = ?");
                updateName.setString(1, newName);
                updateName.setString(2, currentUser);
                updateName.setInt(3, collection_id);
                updateName.executeUpdate();
                System.out.println("Successfully updated " + oldName + " to " + newName);
                updateName.close();
            } else{
                System.out.println("Sorry, you have no such collection");
            }
            queryCollectionExists.close();
        } catch (SQLException e){
            System.out.println("Sorry, something went wrong");
            System.err.println(e.getMessage());
        }

    }

    private void deleteCollection(List<String> args){
        StringBuilder name = new StringBuilder();
        for(int i = 0; i < args.size(); i++){
            name.append(args.get(i));
            if(i < args.size()-1){
                name.append(" ");
            }
        }
        try {
            PreparedStatement queryCollectionExists = conn.prepareStatement("select collection_id from collection " +
                    "where username like ? and name like ?");
            queryCollectionExists.setString(1, currentUser);
            queryCollectionExists.setString(2, name.toString());
            ResultSet res = queryCollectionExists.executeQuery();
            if (res.next()) { //check if collection exists
                int collection_id = res.getInt("collection_id");
                System.out.println(collection_id);
                PreparedStatement deleteCollection = conn.prepareStatement("delete from collection where name like ?");
                deleteCollection.setString(1, name.toString());
                PreparedStatement deleteCollectionContents = conn.prepareStatement("delete from collection_contains where collection_id = ?");
                deleteCollectionContents.setInt(1, collection_id);
                deleteCollection.executeUpdate();
                deleteCollectionContents.executeUpdate();
                System.out.println("Successfully deleted your collection " + name);
            } else{
                System.out.println("Sorry, you have no such collection");
            }
            queryCollectionExists.close();
        } catch (SQLException e){
            System.out.println("Sorry, something went wrong. please try again");
            System.err.println(e.getMessage());
        }
    }

    private void view_profile(String sortType){
        if(!(sortType.equals("playtime") || sortType.equals("rating") || sortType.equals("both"))){
            System.out.println("Usage: profile <sort type: either playtime, rating, or both>");
            return;
        }
        try{
            //no matter what they want to sort by, we always tell the how many collections, follower num, and follow num
            PreparedStatement collectionNumSt =
                    conn.prepareStatement("select count(name) from collection where username like ?");
            collectionNumSt.setString(1, currentUser);
            ResultSet res = collectionNumSt.executeQuery();
            if(res.next()){
                int numCollections = res.getInt("count");
                System.out.println("You have " + numCollections +" collections");
            } else{
                System.out.println("You have 0 collections");
            }
            collectionNumSt.close();

            PreparedStatement followersSt = conn.prepareStatement("select count(fid) from friends_with where fid like ?");
            followersSt.setString(1, currentUser);
            ResultSet res2 = followersSt.executeQuery();
            if(res2.next()){
                int numFollowers = res2.getInt("count");
                System.out.println("You have " + numFollowers + " followers");
            } else {
                System.out.println("You have 0 followers");
            }
            followersSt.close();

            PreparedStatement followSt = conn.prepareStatement("select count(uid) from friends_with where uid like ?");
            followSt.setString(1, currentUser);
            ResultSet res3 = followSt.executeQuery();
            if(res3.next()){
                int numFollow = res3.getInt("count");
                System.out.println("You follow " + numFollow +" users");
            } else{
                System.out.println("You follow 0 users");
            }
            followersSt.close();

            //TODO all of the sort options
            if(sortType.equals("playtime")){
                PreparedStatement topPlayed = conn.prepareStatement("select title, sum(total_playtime) " +
                        "as \"total playtime\" from " +
                        "plays natural join video_game " +
                        "where plays.username = ? " +
                        "group by title " +
                        "ORDER BY sum(plays.total_playtime) DESC limit 10",
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                topPlayed.setString(1, currentUser);
                ResultSet topGames = topPlayed.executeQuery();
                if(topGames.next()){
                    System.out.println("Your top played games:");
                    res.first();
                    printResultSet(topGames);
                } else {
                    System.out.println("You haven't played any games");
                }
                topPlayed.close();
            }
            else if(sortType.equals("rating")){
                PreparedStatement topRated = conn.prepareStatement("select title, rating from " +
                        "rates natural join video_game " +
                        "where rates.username = ? " +
                        "ORDER BY rates.rating DESC limit 10");
                topRated.setString(1, currentUser);
                ResultSet topGames = topRated.executeQuery();
                System.out.println("Your top rated games:");
                printResultSet(topGames);
                topRated.close();
            }
            else{
                PreparedStatement topRatedThenPlayed = conn.prepareStatement("select title, rating, " +
                        "sum(total_playtime) as \"total playtime\"" +
                        "from rates inner join plays on rates.username = plays.username and rates.vg_id = plays.vg_id" +
                        "inner join video_game on plays.vg_id = video_game.vg_id" +
                        "where rates.username = ? and plays.username = ?" +
                        "group by rating, title" +
                        "order by rates.rating desc, sum(plays.total_playtime) desc limit 10");
                topRatedThenPlayed.setString(1, currentUser);
                topRatedThenPlayed.setString(2, currentUser);
                ResultSet topGames = topRatedThenPlayed.executeQuery();
                System.out.println("Your top played games that you have rated in order of rating, then playtime:");
                printResultSet(topGames);
                topRatedThenPlayed.close();
            }
        } catch(SQLException e){
            System.out.println("Sorry, something went wrong. Please Try again.");
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
        ResultSetMetaData rsmd = res.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        int colWidth = 30; // this can be changed if we need to accommodate larger strings
        int tableWidth = (columnsNumber * colWidth) + (columnsNumber + 1) + (2 * columnsNumber);

        for (int i = 0; i < tableWidth; i++) { // print lines
            System.out.print("-");
        }
        System.out.println();
        if (res.isBeforeFirst()) { // print column names
            for (int i = 1; i <= columnsNumber; i++) {
                System.out.printf("| %-" + colWidth + "s ", rsmd.getColumnName(i));
            }
            System.out.println("|");
        }
        for (int i = 0; i < tableWidth; i++) { // print lines
            System.out.print("-");
        }
        System.out.println();

        while (res.next()) { // print rows
            for (int i = 1; i <= columnsNumber; i++) {
                System.out.printf("| %-" + colWidth + "s ", res.getString(i));
            }
            System.out.println("|");
        }
        for (int i = 0; i < tableWidth; i++) { // print lines
            System.out.print("-");
        }
        System.out.println();
    }

    private String getCurrentDateTime(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    private int getIdFromTitle(String title){
        int vg_id = 0;
        try{
            Statement st = this.conn.createStatement();
            ResultSet res = st.executeQuery("select \"vg_id\" from \"video_game\" where \"title\" like '%" + title + "%'");
            if(res.next()) {
                vg_id = res.getInt("vg_id");
            } else {
                System.out.println("This game does not exist");
            }
            st.close();
        }
        catch (SQLException e){
            System.out.println("We are sorry, something went wrong. Video game may not exist. Please see error output for more detail");
            System.err.println(e.getMessage());
            vg_id = 0; //returns zero on error
        }

        return vg_id;
    }

    private void rate_game(String game, int rating){
        if(!(rating <= 5 && rating >= 1)){
            System.out.println("Please enter a valid rating number 1-5.");
        } else {
            int vg_id = getIdFromTitle(game);
            if(vg_id == 0){
                System.out.println("Enter a valid video game.");
            } else {
                try {
                    PreparedStatement st2 = conn.prepareStatement("select rating from rates where vg_id = ?" +
                            "and username like ?");
                    st2.setInt(1, vg_id);
                    st2.setString(2, this.currentUser);
                    ResultSet res = st2.executeQuery();
                    if(res.next()){
                        PreparedStatement st3 = conn.prepareStatement("update rates set rating = ?" +
                                "where username = ? and vg_id = ?");
                        st3.setInt(1, rating);
                        st3.setString(2, this.currentUser);
                        st3.setInt(3, vg_id);
                        st3.executeUpdate();
                    } else {
                        PreparedStatement st4 = conn.prepareStatement("insert into rates values (?,?,?)");
                        st4.setString(1, this.currentUser);
                        st4.setInt(2, vg_id);
                        st4.setInt(3, rating);
                        st4.executeUpdate();
                    }
                    System.out.println("Game has been rated.");
                } catch (SQLException e) {
                    System.out.println("We are sorry, something went wrong. Please see error output for more detail");
                    System.err.println(e.getMessage());
                }
            }
        }
    }

}
