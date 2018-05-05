import java.awt.*;
import java.io.DataInputStream;

/**
 * Created by joseLucas on 19/04/17.
 */
public class ComunicationManager implements SendDataManager {
    private static ComunicationManager ourInstance = new ComunicationManager();

    public static ComunicationManager getInstance() {
        return ourInstance;
    }
    private SocketThread gameSocket;

    private String playerName="";
    public Boolean isHost=false;
    ComunicationListener newsListener;


    //The listener responsable to execute some action when some data is received, for example a new chat message

    private ComunicationManager() {
        prepareSocketThread();
    }

    private void prepareSocketThread(){
        gameSocket = new SocketThread(dataInputStream -> {
            return this.readInputStream(dataInputStream);
        },connectionStatusExceptionTuple ->  {
            SocketStatus status = connectionStatusExceptionTuple.one;
            Exception e = connectionStatusExceptionTuple.two;
            this.socketStatusUpdatedTo(status);
            return null;
        });
    }

    //region ComunicationManager methods


    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void closeConnection(Boolean sendMessage){
        if(sendMessage){this.sendCloseCMessage();}
        gameSocket.closeConnection();
        prepareSocketThread();
    }

    /**This method initialize the sockets as client to make a connection the specified ipAddress
     *      - ipAddress = the IPAddress of server
     * */
    public void connectWith(Tuple<String,Integer> data){
        isHost = false;
        if(gameSocket.getStatus() == SocketStatus.connected || gameSocket.getStatus() == SocketStatus.connecting
                || gameSocket.getStatus() == SocketStatus.waiting){
            System.out.println("Connection open, so closing it");
            closeConnection(false);
        }
        this.playerName = this.playerName.isEmpty() ? "Player 2" : this.playerName;
        System.out.println("Connecting to server with IP "+data.one+" and port "+data.two);
        gameSocket.startAsClient(data.one,data.two);
    }

    /**This method initialize the sockets as server to wait a new connection
     * */
    public Tuple<String,Integer> waitConnection(){
        isHost = true;
        if(gameSocket.getStatus() == SocketStatus.connected || gameSocket.getStatus() == SocketStatus.connecting
                || gameSocket.getStatus() == SocketStatus.waiting){
            System.out.println("Connection open, so closing it");
            closeConnection(false);
        }
        this.playerName = this.playerName.isEmpty() ? "Player 1" : this.playerName;
        System.out.println("Connect as a server");
        Tuple<String,Integer> data = gameSocket.startAsServer();
        return data;
    }
    //endregion



    //region Protocols methods and Connection Status listener

    /**This method will get the received data of chat inputStrem and accordingly to my protocol prepare to give to ReceivedDataListener
     * help links http://www.vogella.com/tutorials/JavaRegularExpressions/article.html
     * http://regexr.com
     * */
    private Boolean readInputStream(DataInputStream inputS){
        try{
            String input = inputS.readUTF();
            System.out.println(input);

            readChatData(input);
            readGameData(input);
            readPlayerMove(input);
            Boolean keepReading = readPlayerConnectionMessage(input);
            return keepReading;
        }
        catch(Exception e){
            System.out.println("Erro ao tentar ler "+e.getLocalizedMessage());
            gameSocket.updateSocketStatus(SocketStatus.failureToRead,e);
        }
        return false;
    }

    /**
     * This method reads messages of chat and update newsListener
     */
    private void readChatData(String input){
        YoteSocketProtocol.readChatData(input,(playerName, message) -> {
            newsListener.receivedNewChatMessage(playerName,message,MessageType.otherPlayer);
            return null;
        });
    }

    /**
     * Return true if there is some close message,so it should stop reading inputStream
     * */
    private Boolean readPlayerConnectionMessage(String input){
        return YoteSocketProtocol.readPlayerConnectionData(input,(playerName, status) ->{
            Boolean keepReading = true;
            if(status.equalsIgnoreCase("1")){//other player entered on match
                keepReading = true;
                newsListener.showAlertWith("ALERTA",playerName+" entrou na partida !");
                //show an alert to user
            }
            else if(status.equalsIgnoreCase("2")){//other player leaving the match
                newsListener.showAlertWith("ALERTA",playerName+" deixou a partida !");
                //show an alert to user
                keepReading = false;
                this.closeConnection(false);
            }
            return keepReading;
        });
    }

    private void readPlayerMove(String input){
        YoteSocketProtocol.readPlayerMoveData(input,(playerName, action) ->{

            switch (action){
                case 1://give up
                    Main.gManager.updateMatchStatusTo(MatchStatus.finished);
                    newsListener.showAlertWith("VITORIA","Parabéns "+this.playerName+" a vitória é sua\n"+playerName+" desistiu da partida !");
                    break;
                case 2://play again

                    if(Main.gManager.getMatchStatus() == MatchStatus.waitingToStart){//receiving confirmation of other player
                        Main.gManager.updateMatchStatusTo(MatchStatus.inProgress);
                        newsListener.showAlertWith("INFO",playerName+" aceitou jogar novamente");
                    }
                    else{//sending request to other player
                        Main.gManager.rmtPlayerIsReady = true;
                        newsListener.showAlertWith("Jogar Novamente",playerName+" está pronto para jogar novamente");
                    }
                    break;
            }

            return null;
        } );
    }

    private void readGameData(String input){
        readAddBlockMessage(input);
    }

    private void readAddBlockMessage(String input){
        YoteSocketProtocol.readGameAddBlockData(input,point -> {
            Main.gManager.addNewBlockAt(point,false);
            return null;
        });
    }

    //endregion


    private void socketStatusUpdatedTo(SocketStatus status){
        if(status == SocketStatus.waiting){
            Main.gManager.updateGameStatusTo(GameStatus.lookingForPlayer);
            newsListener.updateInfoLabel("IP: "+this.gameSocket.getIpAddress()+" PORTA: "+this.gameSocket.getPort());
        }
        else if(status == SocketStatus.connecting){
            Main.gManager.updateGameStatusTo(GameStatus.lookingForPlayer);
            newsListener.updateInfoLabel("Entrando na sala");
        }
        else if(status == SocketStatus.connected){
            Main.gManager.updateGameStatusTo(GameStatus.onParty);
            newsListener.updateInfoLabel("Conectado");
            sendPlayerConnectionMessage(1);//entered on match
        }
        else if(status == SocketStatus.closed){
            newsListener.updateInfoLabel("");
            Main.gManager.updateGameStatusTo(GameStatus.none);
        }
        else if(status.getValue() >= 4){// erros
            newsListener.updateInfoLabel("");
            if(status == SocketStatus.failureToConnect){
                newsListener.showAlertWith("ALERTA","Não foi possivel estabelecer conexão");
            }
            else if(status == SocketStatus.failureToRead || status == SocketStatus.failureToSend){
                newsListener.showAlertWith("ALERTA","Erro de Conexão !");
            }

            Main.gManager.updateGameStatusTo(GameStatus.none);
            closeConnection(false);
            //newsListener.showAlertWith("ERRO","Ocorreu um erro de conexão");
        }
    }

    //region SendDataManager and other Comunication  methods

    private Boolean sendPlayerConnectionMessage(int status){//1 on match 2 leaving match
        try{
            String data = YoteSocketProtocol.preparePlayerConnectionStatus(playerName,status);
            this.gameSocket.getDataOutput().writeUTF(data);
            this.gameSocket.getDataOutput().flush();
        }
        catch(Exception e){
            if(status == 1){//trying to get in match
                gameSocket.updateSocketStatus(SocketStatus.failureToSend,e);
            }
            return false;
        }
        return true;
    }

    private void sendCloseCMessage(){
        sendPlayerConnectionMessage(2);//leaving a match
    }

    @Override
    public Boolean sendPlayerActionMessage(String playerName,int action) {//1-give up, 2-jogar novamente
        String data = YoteSocketProtocol.preparePlayerMoveData(playerName,action);
        if(gameSocket.getStatus() == SocketStatus.connected){
            try{
                this.gameSocket.getDataOutput().writeUTF(data);
                this.gameSocket.getDataOutput().flush();
            }
            catch (Exception e){
                gameSocket.updateSocketStatus(SocketStatus.failureToSend,e);
                return false;
            }
        }
        else{
            return false;
        }
        return true;
    }

    @Override
    public Boolean sendNewChatMessage(String playerName, String message){
        String data = YoteSocketProtocol.prepareChatMessage(playerName,message);

        if(gameSocket.getStatus() == SocketStatus.connected){
            try{
                this.gameSocket.getDataOutput().writeUTF(data);
                this.gameSocket.getDataOutput().flush();
            }
            catch (Exception e){
                System.out.println("Erro ao enviar a mensagem "+message);
                gameSocket.updateSocketStatus(SocketStatus.failureToSend,e);
                return false;
            }
        }
        else{
            return false;
        }
        return true;
    }

    @Override
    public Boolean sendNewMoveActionMessage(String playerName, Point fromPos, Point toPos) {
        String data = "";
        if(toPos == null){
            data = YoteSocketProtocol.prepareGameAddBlockData(playerName,fromPos);
        }
        else{
            data = YoteSocketProtocol.prepareGameMoveBlockData(playerName,fromPos,toPos);
        }

        if(gameSocket.getStatus() == SocketStatus.connected){
            try{
                this.gameSocket.getDataOutput().writeUTF(data);
                this.gameSocket.getDataOutput().flush();
            }
            catch (Exception e){
                System.out.println("Erro ao enviar a movimento");
                gameSocket.updateSocketStatus(SocketStatus.failureToSend,e);
                return false;
            }
            return true;
        }
        return false;
    }



    //endregion




}
