import java.awt.*;

/**
 * Created by joseLucas on 21/04/17.
 */
public interface SendDataManager {

    public Boolean sendNewChatMessage(String playerName,String message);

    public Boolean sendNewMoveActionMessage(String playerName, Point fromPos, Point toPos);

    public Boolean sendPlayerActionMessage(String playerName,int action);
}
