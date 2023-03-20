import java.util.Scanner;

public class Application {
    public Application(){
    }

    public void init(){
        Scanner scanner = new Scanner(System.in);
        boolean quit = true;
        while(quit){
            String nextCmd = scanner.nextLine();
            if(nextCmd.equals("q")){
                quit = false;
            }
        }
    }
}
