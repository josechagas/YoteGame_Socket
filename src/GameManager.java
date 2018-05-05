import java.awt.*;

/**
 * Created by joseLucas on 23/04/17.
 */
public class GameManager {
    private ComunicationManager comunicationM = ComunicationManager.getInstance();

    private int[][] boardMatrix = new int [6][5];

    private GameStatus gameStatus = GameStatus.none;
    private MatchStatus matchStatus = MatchStatus.none;


    public GameUpdatesListener gameListener;
    public SendDataManager sendDM;


    //region Local player variables
    public Player localPlayer;
    private Boolean myTurn = false;
    //endregion
    public Player remotePlayer;
    public Boolean rmtPlayerIsReady=false;

    //sempre o jogador local suas pecas tem o valor um o remoto tem valor 2 e casas vazias valor 0

    public GameManager(){
        boardMatrix = new int[6][5];
        sendDM = ComunicationManager.getInstance();

    }

    private void startMatch(){
        boardMatrix = new int[6][5];
        localPlayer = new Player(ComunicationManager.getInstance().isHost);
        updateMyTurnStatusTo(ComunicationManager.getInstance().isHost);
        remotePlayer = new Player(!ComunicationManager.getInstance().isHost);
    }

    private void endMatch(){
        boardMatrix = new int[6][5];
        localPlayer = null;
        remotePlayer = null;
        myTurn = false;
    }

    //region Getters and Setters

    public Boolean isMyTurn() {
        return myTurn;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    //endregion

    public void updateGameStatusTo(GameStatus gameStatus){
        if (this.gameStatus != gameStatus){

            MatchStatus s = MatchStatus.none;
            if(gameStatus == GameStatus.onParty){
                s = MatchStatus.inProgress;
            }
            else{
                s = MatchStatus.none;
            }

            this.updateMatchStatusTo(s);

            this.gameStatus = gameStatus;
            gameListener.gameStatusChangedTo(gameStatus);

        }
    }

    public void updateMatchStatusTo(MatchStatus status){
        if (this.matchStatus != status){

            if(status == MatchStatus.inProgress || status == MatchStatus.waitingToStart){
                startMatch();
            }
            else{
                endMatch();
            }

            this.matchStatus = status;
            gameListener.matchStatusChangedTo(this.matchStatus);
        }
    }

    public void updateMyTurnStatusTo(Boolean myTurn){
        if(this.myTurn != myTurn){
            this.myTurn = myTurn;
            gameListener.updateForLocalUserTurn(this.myTurn);
        }
    }


    //region Moves and verifications methods

    public int getBlockAtPoint(Point pos){
        return boardMatrix[pos.y][pos.x];
    }

    private void setBlockAt(Point pos,int value){
        boardMatrix[pos.y][pos.x] = value;
    }

    /**Returns true if the tapped blocks belongs to corresponding user
     * */
    public Boolean isAValidBlockToUser(Point pos,Boolean isLocal){
        Player player = isLocal ? localPlayer : remotePlayer;
        return getBlockAtPoint(pos) == player.blocksValue;
    }

    private Boolean isAFreeDestinyPos(Point pos){
        return getBlockAtPoint(pos) == 0;
    }

    //Check if its horizontal or vertical and correct distance move
    private Boolean isAValidDirectionMove(Point initialPos,Point lastPos){
        Boolean isAValidHozontal = initialPos.y == lastPos.y && Math.abs(initialPos.x - lastPos.x) == 1;
        Boolean isAValidVertical = initialPos.x == lastPos.x && Math.abs(initialPos.y - lastPos.y) == 1;

        return isAValidHozontal || isAValidVertical;
    }

    public Boolean canMoveBlockFrom(Point initialPoint,Point finalPoint,Boolean isLocal){
        if(isAValidBlockToUser(initialPoint,isLocal) && isAFreeDestinyPos(finalPoint)){
            return isAValidDirectionMove(initialPoint,finalPoint);
        }
        return false;
    }


    /**Gets one of the blocks available out of board and add it
     * returns true added it, false instead
     * */
    public Boolean addNewBlockAt(Point point,Boolean isLocal){
        Player player = isLocal ? localPlayer : remotePlayer;
        if(player.getBlocksToAdd() > 0 && isAFreeDestinyPos(point)){
            setBlockAt(point,player.blocksValue);
            player.addNewBlockOnBoard();

            if(isLocal){//sends the message to other player
                ComunicationManager c = ComunicationManager.getInstance();
                Boolean success = sendDM.sendNewMoveActionMessage(c.getPlayerName(),point,null);
                if(success){updateMyTurnStatusTo(false);};
                return success;
            }
            else{//updates the board cause of new user move
                updateMyTurnStatusTo(true);
                gameListener.updateBoard();
            }

            return true;
        }
        return false;
    }

    public void addBlockAt(Point point,Boolean isLocal){
        Player player = isLocal ? localPlayer : remotePlayer;
        if(isAFreeDestinyPos(point)){
            setBlockAt(point,player.blocksValue);
        }
    }
    //endregion

    /**
     * return true if need updates
     * */
    public Boolean didClickedOnPos(Point pos,Boolean isLocal){
        if(isAFreeDestinyPos(pos)){
            return addNewBlockAt(pos,isLocal);
        }
        else if(!isAValidBlockToUser(pos,isLocal)) {//tapped on other player block
            //check if can get this block based on last move
        }
        return false;
    }

}
