import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Created by joseLucas on 15/04/17.
 */
public class GameWindow implements ComunicationListener,GameUpdatesListener {

    private JPanel contentPanel;
    private JPanel yotePanel;

    private JTextField messageTF;
    private JTextPane chatTextPane;
    private JButton createPartyButton;
    private JLabel socketLabel;
    private JTextField playerNameTF;
    private JLabel playerTurnLabel;
    private JButton giveUpButton;


    public  GameWindow(){
        setUpMessageTF();
        setUpCreatePartyButton();
        setUpPlayerNameTF();
        setUpGiveUpButton();

    }

    public JPanel getContentPanel() {
        return contentPanel;
    }


    private void createUIComponents() {
        yotePanel = new YoteBoardPanel();
        // TODO: place custom component creation code here
    }


    //region MessageTF methods

    private void setUpMessageTF(){
        messageTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);

                if(e.getKeyChar() == '\n'){//send the message
                    //chatTextPane.setText(chatTextPane.getText()+"\n\n"+messageTF.getText());
                    //call messages socket to send the message
                    Boolean success = Main.gManager.sendDM.sendNewChatMessage(ComunicationManager.getInstance().getPlayerName(),messageTF.getText());
                    if(success){
                        showMessage(ComunicationManager.getInstance().getPlayerName(),messageTF.getText(),MessageType.localPlayer);
                        messageTF.setText(null);
                    }
                    else{
                        showMessage("ERRO","Não foi possivel enviar sua mensagem !",MessageType.error);
                    }
                }
            }
        });
    }

    private Boolean showMessage(String playerName, String message,MessageType type) {
        StyledDocument doc = chatTextPane.getStyledDocument();

        Style titleStyle = chatTextPane.addStyle("title", null);
        Style messageStyle = chatTextPane.addStyle("message", null);

        //color is accordingly to player
        Color titleColor = type.titleColor();
        Color messageColor = type.messageColor();

        try {
            //title
            int lenght = doc.getLength();
            StyleConstants.setForeground(titleStyle, titleColor);
            String text;
            if(playerName != null){//Its an error message
                text = (lenght > 0 ? "\n\n" : "") + playerName + ": ";
            }
            else{
                text = (lenght > 0 ? "\n\n" : "");
            }

            if(type == MessageType.system){
                text = text.toUpperCase();
                message = message.toUpperCase();
            }

            doc.insertString(lenght, text, titleStyle);

            //message
            StyleConstants.setForeground(messageStyle, messageColor);
            doc.insertString(doc.getLength(), message, messageStyle);
            return true;
        } catch (BadLocationException e) {
            return false;
        }
    }

    //endregion

    private void setUpPlayerNameTF(){
        if(playerNameTF.getText().isEmpty()){
            playerNameTF.setText(ComunicationManager.getInstance().getPlayerName());
        }

        playerNameTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if(e.getKeyChar() == '\n'){
                    ComunicationManager.getInstance().setPlayerName(playerNameTF.getText());
                    createPartyButton.requestFocus();
                }
            }
        });
    }

    //region NewMatchButton methods
    private void setUpCreatePartyButton(){
        createPartyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(Main.gManager.getGameStatus() == GameStatus.onParty) {
                    //its on a match
                    String[] options = new String[] {"Sair", "Cancelar"};

                    String message = "Deseja Continuar ?";

                    int response = JOptionPane.showConfirmDialog(contentPanel,message,"Abandonar Partida",JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);

                    if(response == 0){
                        ComunicationManager.getInstance().closeConnection(true);
                    }
                }
                else if(Main.gManager.getGameStatus() == GameStatus.lookingForPlayer){
                    ComunicationManager.getInstance().closeConnection(true);
                }
                else{
                    requestConnection(null);
                }
            }
        });
    }

    //endregion

    private void updateGiveUpButtonFor(MatchStatus status){
        giveUpButton.setForeground(status == MatchStatus.inProgress ? Color.red : Color.black);
        giveUpButton.setText(status != MatchStatus.finished ? "DESISTIR" : "Jogar Novamente");
        giveUpButton.setVisible(status != MatchStatus.none ? true : false);
        giveUpButton.setEnabled(status != MatchStatus.none ? true : false);
    }

    private void setUpGiveUpButton(){
        giveUpButton.setVisible(false);
        giveUpButton.setEnabled(false);
        giveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(Main.gManager.getMatchStatus() == MatchStatus.inProgress) {//give up
                    String[] options = new String[] {"Sair", "Cancelar"};

                    String message = "Deseja Continuar ?";

                    int response = JOptionPane.showConfirmDialog(contentPanel,message,"Desistir",JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);
                    if(response == 0){
                        //send some message to user and updates matchStatus
                        Main.gManager.sendDM.sendPlayerActionMessage(ComunicationManager.getInstance().getPlayerName(),1);//1 give up, 2 jogar novamente
                        Main.gManager.updateMatchStatusTo(MatchStatus.finished);
                    }
                }
                else{//play again
                    Main.gManager.sendDM.sendPlayerActionMessage(ComunicationManager.getInstance().getPlayerName(),2);//1 give up, 2 jogar novamente
                    Main.gManager.updateMatchStatusTo(Main.gManager.rmtPlayerIsReady ? MatchStatus.inProgress : MatchStatus.waitingToStart);
                }
            }
        });
    }

    //region ComunicationListener methods

    @Override
    public void receivedNewChatMessage(String playerName, String message,MessageType type) {
        System.out.println("Chat Message received on GameWindow by ReceivedDataListener");
        showMessage(playerName,message,type);
    }

    @Override
    public void showChatAlertMessage(String title, String message) {
        this.showMessage(title,message,MessageType.error);
    }

    @Override
    public void showAlertWith(String title, String message) {
        JOptionPane.showMessageDialog(this.contentPanel,message,title,JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void updateInfoLabel(String text) {
        this.socketLabel.setText(text);
    }

    //endregion

    //region GameUpdatesListener methods


    @Override
    public void updateBoard() {
        YoteBoardPanel panel = (YoteBoardPanel)yotePanel;
        panel.redrawUI();
    }

    @Override
    public void matchStatusChangedTo(MatchStatus matchStatus) {
        updateGiveUpButtonFor(matchStatus);

        if(matchStatus == MatchStatus.inProgress){
            updateBoard();
            if(playerNameTF.getText().isEmpty()){
                playerNameTF.setText(ComunicationManager.getInstance().getPlayerName());
            }
            playerNameTF.setEnabled(false);
            yotePanel.setEnabled(Main.gManager.isMyTurn());

            updateForLocalUserTurn(Main.gManager.isMyTurn());//to show turn label
        }
        else if(matchStatus == MatchStatus.waitingToStart){
            updateBoard();
            if(playerNameTF.getText().isEmpty()){
                playerNameTF.setText(ComunicationManager.getInstance().getPlayerName());
            }
            playerNameTF.setEnabled(false);
            yotePanel.setEnabled(false);
            playerTurnLabel.setText("Esperando por confirmação");
        }
        else{
            playerNameTF.setEnabled(true);
            this.playerTurnLabel.setText("");
            yotePanel.setEnabled(false);
        }
    }

    @Override
    public void gameStatusChangedTo(GameStatus gameStatus) {
        if(!(playerNameTF.getText().toLowerCase().equalsIgnoreCase(ComunicationManager.getInstance().getPlayerName().toLowerCase()))){
            playerNameTF.setText(ComunicationManager.getInstance().getPlayerName());
        }

        if(gameStatus == GameStatus.onParty || gameStatus == GameStatus.lookingForPlayer){
            this.chatTextPane.setText("");
            createPartyButton.setText(gameStatus == GameStatus.lookingForPlayer && !ComunicationManager.getInstance().isHost ? "Cancelar":"Sair");
            //createPartyButton.setEnabled(gameStatus == GameStatus.lookingForPlayer && !ComunicationManager.getInstance().isHost? false : true);
        }
        else{
            this.playerTurnLabel.setText("");
            this.chatTextPane.setText("");
            playerNameTF.setEnabled(true);
            yotePanel.setEnabled(false);
            updateBoard();
            createPartyButton.setText("Criar Sala");
            createPartyButton.setEnabled(true);
        }
    }

    @Override
    public void updateForLocalUserTurn(Boolean turn) {
        yotePanel.setEnabled(turn);
        String message = turn ? "Sua vez" : "Aguardando outro jogador";
        playerTurnLabel.setText(message);
    }
    //endregion

    //region Connection UI methods

    /**
     * This method call the dialog that asks if you want create a new party or connect to one
     * */
    public void requestConnection(String message){
        ConnectionDialog.presentRelativeTo(contentPanel,message);
    }

    //endregion

    /*
    public void showOptionPaneWith(String title, String message) {
        JOptionPane.showMessageDialog(this.contentPanel,message,title,JOptionPane.OK_OPTION);
        //int result = JOptionPane.showConfirmDialog(this.contentPanel,null, "ScreenPreview", JOptionPane.OK_OPTION,JOptionPane.PLAIN_MESSAGE);
    }
    */
}
