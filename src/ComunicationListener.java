/**
 * Created by joseLucas on 21/04/17.
 */
public interface ComunicationListener {

    public void receivedNewChatMessage(String playerName,String message,MessageType type);
    //public void receivedNewGameAction();
    public void showAlertWith(String title,String message);
    public void showChatAlertMessage(String title,String message);
    ///public void receivedSocketUpdates(SocketStatus status,SocketThread socket);
    public void updateInfoLabel(String text);

    //public void prepareUIForNewMatch();

    //public void updateUIForGameStatus(GameStatus status);
}