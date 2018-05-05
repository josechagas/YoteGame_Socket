import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joseLucas on 23/04/17.
 */
public class YoteSocketProtocol {

    //region Decode received data methods
    public static void readChatData(String input, BiFunction<String,String,Void> completion){
        //each data (\_{1}(N){1}\_{1}){1}[A-CE-Za-z0-9\s\_\/]*(\_{1}(N){1}\/{1}){1}
        String dataRegex = "(_{1}C_{1})(.*?)(_{1}C\\/{1})";
        String playerRegex = "(_{1}N_{1})(.*?)(_{1}N\\/{1})";
        String messageRegex = "(_{1}M_{1})(.*?)(_{1}M\\/{1})";

        //playerName on Data
        Pattern newP = Pattern.compile(dataRegex);
        Matcher m = newP.matcher(input);

        while(m.find()){
            String data = m.group(2);
            Matcher playerM = Pattern.compile(playerRegex).matcher(data);
            playerM.find();
            Matcher messageM = Pattern.compile(messageRegex).matcher(data);
            messageM.find();
            String playerName = playerM.group(2);
            String message = messageM.group(2);

            completion.apply(playerName,message);
        }
    }

    public  static Boolean readPlayerConnectionData(String input,BiFunction<String,String,Boolean>completion){
        String dataRegex = "(_{1}PC_{1})(.*?)(_{1}PC\\/{1})";
        String playerRegex = "(_{1}N_{1})(.*?)(_{1}N\\/{1})";
        String statusRegex = "(_{1}S_{1})(.*?)(_{1}S\\/{1})";

        Pattern newP = Pattern.compile(dataRegex);
        Matcher m = newP.matcher(input);
        Boolean keepReading = true;
        while(m.find()){
            String data = m.group(2);
            Matcher playerM = Pattern.compile(playerRegex).matcher(data);
            Matcher statusM = Pattern.compile(statusRegex).matcher(data);
            playerM.find();
            statusM.find();

            String playerName = playerM.group(2);
            String status = statusM.group(2);
            keepReading = completion.apply(playerName,status);
        }
        return keepReading;
    }

    //Not in use
    /*public static void readGameMoveBlockData(String input,BiFunction<String,Point,Void>completion){

    }*/

    public static void readGameAddBlockData(String input, Function<Point, Void>completion){
        String dataRegex = "(_{1}GM_{1})(.*?)(_{1}GM\\/{1})";
        String pointRegex = "(_{1}A_{1})(.*?)(_{1}A\\/{1})";

        Pattern newP = Pattern.compile(dataRegex);
        Matcher m = newP.matcher(input);
        while(m.find()){
            String data = m.group(2);
            Matcher pointM = Pattern.compile(pointRegex).matcher(data);
            pointM.find();

            String pointString = pointM.group(2);
            String x = pointString.substring(pointString.indexOf("(")+1,pointString.indexOf(","));
            String y = pointString.substring(pointString.indexOf(",")+1,pointString.indexOf(")"));

            Point point = new Point(new Integer(x),new Integer(y));
            System.out.println(point.x);
            completion.apply(point);
        }
    }

    public static void readPlayerMoveData(String input,BiFunction<String,Integer,Void>completion){
        String dataRegex = "(_{1}PM_{1})(.*?)(_{1}PM\\/{1})";
        String playerRegex = "(_{1}N_{1})(.*?)(_{1}N\\/{1})";
        String actionRegex = "(_{1}AC_{1})(.*?)(_{1}AC\\/{1})";

        Pattern newP = Pattern.compile(dataRegex);
        Matcher m = newP.matcher(input);

        while(m.find()){
            String data = m.group(2);
            Matcher playerM = Pattern.compile(playerRegex).matcher(data);
            Matcher actionM = Pattern.compile(actionRegex).matcher(data);
            playerM.find();
            actionM.find();
            String playerName = playerM.group(2);
            Integer action = new Integer(actionM.group(2));
            completion.apply(playerName,action);
        }
    }

    //endregion




    //region Prepare data to send methods

    public static String preparePlayerMoveData(String playerName,int status){// 1=giveUp / 2=jogar novamente
        String dataStart = "_PM_";//player move
        String dataEnd = "_PM/";//player move
        String nameStart = "_N_";
        String nameEnd = "_N/";
        String actionStart = "_AC_";
        String actionEnd = "_AC/";
        String data = dataStart+nameStart+playerName+nameEnd+actionStart+status+actionEnd+dataEnd;
        return data;
    }

    public static String preparePlayerConnectionStatus(String playerName,int status){
        String nameStart = "_N_";
        String nameEnd = "_N/";
        String statusStart = "_S_";
        String statusEnd = "_S/";
        String data = "_PC_"+nameStart+playerName+nameEnd+statusStart+status+statusEnd+"_PC/";
        return data;
    }

    public static String prepareChatMessage(String playerName,String message){
        String dataStart = "_C_";//chat
        String dataEnd = "_C/";//chat
        String nameStart = "_N_";
        String nameEnd = "_N/";
        String messStart = "_M_";
        String messEnd = "_M/";

        String data = dataStart+nameStart+playerName+nameEnd+messStart+message+messEnd+dataEnd;
        return data;
    }

    public static String prepareGameMoveBlockData(String playerName, Point fromPos, Point toPos){
        String dataStart = "_GM_";//player move
        String dataEnd = "_GM/";//player move
        String nameStart = "_N_";
        String nameEnd = "_N/";
        String moveStart = "_M_";
        String moveEnd = "_M/";

        String fromString = "("+fromPos.x+","+fromPos.y+")";
        String toString = "("+toPos.x+","+toPos.y+")";;
        fromString+="->"+toString;

        String data = dataStart+moveStart+fromString+moveEnd+dataEnd;

        return data;

    }

    public static String prepareGameAddBlockData(String playerName,Point toPos){
        String dataStart = "_GM_";//player move
        String dataEnd = "_GM/";//player move
        String nameStart = "_N_";
        String nameEnd = "_N/";
        String addStart = "_A_";
        String addEnd = "_A/";

        String toString = "("+toPos.x+","+toPos.y+")";

        String data = dataStart+addStart+toString+addEnd+dataEnd;

        return data;

    }

    //endregion

}
