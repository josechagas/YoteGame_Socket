/**
 * Created by joseLucas on 19/04/17.
 */
public enum SocketStatus {

    connecting(0),connected(1),closed(2),waiting(3),connectionFailure(4),failureToConnect(5),failureToClose(6),failureToSend(7),failureToRead(8);

    private final int value;

    SocketStatus(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    public String getStringValue(){
        switch (value){
            case 0:
                return "Conectando";
            case 1:
                return "Conectado";
            case 2:
                return "Encerrada";
            case 3:
                return "Aguardando";
            case 4:
                return "Falha na conexão";
            case 5:
                return "Falha ao conectar";
            case 6:
                return "Falha ao fechar";
            case 7:
                return "Erro no envio";
            case 8:
                return "Erro na leitura";
            default:
                return "";
        }
    }

}
