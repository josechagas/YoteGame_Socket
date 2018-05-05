import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

/**
 * Created by joseLucas on 18/04/17.
 */



public class SocketThread extends Thread {
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private DataOutputStream dataOutput = null;
    private DataInputStream dataInput = null;


    private Function<DataInputStream,Boolean>  inputReadListener;
    private Function<Tuple<SocketStatus,Exception>,Void> connectionListener;

    private Boolean isClient = false;
    private String ipAddress = null;
    private int port;
    private SocketStatus status = SocketStatus.closed;
    //private Function<Void,Void> errorListener;

    /*aqui serao passados listeners para informar falha na conexao,
    conexao estabelecida, dado recebido
    *
    *
    * */
    /**port = port number to listen
     * inputDataReader = block of code executed to read the inputStream; returns true when read with success
     *      -
     * connectionListener = block of code that is executed when there is updates of socket connection
     *      - status of connection true connection ok false lost conection
     *
     * */

    public SocketThread(Function<DataInputStream,Boolean> inputDataReader
            ,Function<Tuple<SocketStatus,Exception>,Void> connectionListener){
        this.inputReadListener = inputDataReader;
        this.connectionListener = connectionListener;
        //just add the listeners
    }

    public Boolean getClient() {
        return isClient;
    }

    public DataOutputStream getDataOutput() {
        return dataOutput;
    }

    public int getPort() {
        if(!isClient && serverSocket != null){
            return serverSocket.getLocalPort();
        }
        return port;
    }

    public String getIpAddress() {
        if(!isClient && serverSocket != null){
            return serverSocket.getInetAddress().getHostAddress();
        }
        return ipAddress;
    }

    public SocketStatus getStatus() {
        return status;
    }

    private void initDataIO(Socket socket) throws IOException {
        dataOutput = new DataOutputStream(socket.getOutputStream());
        dataInput = new DataInputStream(socket.getInputStream());
    }

    public void startAsClient(String ipAddress,int port){
        isClient = true;
        this.ipAddress = ipAddress;
        this.port = port;
        this.start();
    }

    public Tuple<String,Integer> startAsServer(){
        isClient = false;
        try{
            serverSocket = new ServerSocket(0);
        }
        catch(Exception e){
            updateSocketStatus(SocketStatus.failureToConnect,e);
            //calls one of the listeners to inform the problem
            return null;
        }
        this.start();
        return new Tuple<String, Integer>(getIpAddress(),getPort());
    }

    synchronized
    public void closeConnection() {
        if(status == SocketStatus.connected || status == SocketStatus.connecting || status == SocketStatus.waiting){
            updateSocketStatus(SocketStatus.closed,null);
            try{
                if(socket != null){
                    socket.shutdownInput();
                    socket.shutdownOutput();
                    socket.close();
                    if(socket.isClosed()){socket = null;}
                }
                if(serverSocket != null) {
                    serverSocket.close();
                    //if (serverSocket.isClosed()){serverSocket = null;}
                }
                System.out.println("Connection closed with success");
            } catch(Exception e){
                System.out.println(e.getLocalizedMessage());
            }
        }
        else{
            System.out.println("Socket ja fechado");
        }
        socket = null;
        serverSocket = null;
    }


    private void serverSideSocket(){
        updateSocketStatus(SocketStatus.waiting,null);
        try{
            socket = serverSocket.accept();
            initDataIO(this.socket);
        }
        catch(Exception e){
            updateSocketStatus(SocketStatus.failureToConnect,e);
            //calls one of the listeners to inform the problem
            return;
        }
        updateSocketStatus(SocketStatus.connected,null);
    }

    synchronized
    private void clientSideSocket(){
        updateSocketStatus(SocketStatus.connecting,null);
        try{
            socket = new Socket(ipAddress,port);
            initDataIO(socket);
            updateSocketStatus(SocketStatus.connected,null);
        }
        catch(Exception e){
            updateSocketStatus(SocketStatus.failureToConnect,e);
            //calls one of the listeners to inform the problem
        }
    }
    //-----------Thread part

    @Override
    public void run() {
        if(isClient){
            clientSideSocket();
        }
        else{
            serverSideSocket();
        }
        Boolean keepReading = true;


        while(socket != null && status == SocketStatus.connected && keepReading){
            //
            keepReading = this.inputReadListener.apply(dataInput);
            //this.inputReadListener(dataInput);
            //call listener of inputStream
        }
        System.out.println("Thread de input encerrada");
    }

    public void updateSocketStatus(SocketStatus status, Exception e){

        if(status.getValue() >= 4){//some error status
            if(this.status != SocketStatus.closed){
                System.out.println(this.status.getStringValue());
                this.status = status;
                connectionListener.apply(new Tuple<SocketStatus,Exception>(status,e));
            }
        }
        else{
            this.status = status;
            connectionListener.apply(new Tuple<SocketStatus,Exception>(status,e));
        }

        System.out.println("Socket status: "+this.status.getStringValue());

    }

}
