package socketclientfx;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.peertopark.java.geocalc.Coordinate;
import com.peertopark.java.geocalc.DegreeCoordinate;
import com.peertopark.java.geocalc.EarthCalc;
import com.peertopark.java.geocalc.Point;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import socketfx.Constants;
import socketfx.FxSocketClient;
import socketfx.SocketListener;

public class FXMLDocumentController implements Initializable {

    @FXML
    private ListView<String> rcvdMsgsListView;
    @FXML
    private Button sendButton;
    @FXML
    private TextField sendTextField;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;
    @FXML
    private Button done_traj_button;
    @FXML
    private TextField hostTextField;
    @FXML
    private TextField portTextField;
    @FXML
    private Label testLabel;
    @FXML
    private Label connectedLabel;

    private final static Logger LOGGER
            = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private ObservableList<String> rcvdMsgsData;
    private ObservableList<String> sentMsgsData;

    private boolean connected;
    private volatile boolean isAutoConnected;

    public enum ConnectionDisplayState { DISCONNECTED, ATTEMPTING, CONNECTED, AUTOCONNECTED, AUTOATTEMPTING }

    private FxSocketClient socket;

    private synchronized void waitForDisconnect() {
        while (connected) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private synchronized void notifyDisconnected() {
        connected = false;
        notifyAll();
    }

    private synchronized void setIsConnected(boolean connected) {
        this.connected = connected;
    }

    private synchronized boolean isConnected() {
        return (connected);
    }

    private void connect() {
        socket = new FxSocketClient(new FxSocketListener(),
                hostTextField.getText(),
                Integer.valueOf(portTextField.getText()),
                Constants.instance().DEBUG_ALL);
        socket.connect();
    }

    private void displayState(ConnectionDisplayState state) {
        switch (state) {
            case DISCONNECTED:
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
                sendButton.setDisable(true);
                sendTextField.setDisable(true);
                connectedLabel.setText("Not connected");
                break;
            case ATTEMPTING:
            case AUTOATTEMPTING:
                connectButton.setDisable(true);
                disconnectButton.setDisable(true);
                sendButton.setDisable(true);
                sendTextField.setDisable(true);
                connectedLabel.setText("Attempting connection");
                break;
            case CONNECTED:
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                sendButton.setDisable(false);
                sendTextField.setDisable(false);
                connectedLabel.setText("Connected");
                break;
            case AUTOCONNECTED:
                connectButton.setDisable(true);
                disconnectButton.setDisable(true);
                sendButton.setDisable(false);
                sendTextField.setDisable(false);
                connectedLabel.setText("Connected");
                break;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setIsConnected(false);
        isAutoConnected = false;
        displayState(ConnectionDisplayState.DISCONNECTED);

        sentMsgsData = FXCollections.observableArrayList();

//        rcvdMsgsData = FXCollections.observableArrayList();
//        rcvdMsgsListView.setItems(rcvdMsgsData);
//        rcvdMsgsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        Runtime.getRuntime().addShutdownHook(new ShutDownThread());
    }

    class ShutDownThread extends Thread {

        @Override
        public void run() {
            if (socket != null) {
                    LOGGER.info("ShutdownHook: Shutting down Server Socket");
                socket.shutdown();
            }
        }
    }

    class FxSocketListener implements SocketListener {

        @Override
        public void onMessage(String line) {
            if (line != null && !line.equals("")) {
                displayState(ConnectionDisplayState.CONNECTED);
            }
            testLabel.setText("Working!");
            System.out.println("yee");
//            processString(line);
        }

        public void processString(String line){
            String[] list = line.split(",");
            for(String s : list){
                s.replace(" ", "");
            }

            int messageId = Integer.parseInt(list[0]);
            double firstLat = Double.parseDouble(list[1])/1000000;
            double firstLon = Double.parseDouble(list[2])/1000000;
            int firstMinMin = Integer.parseInt(list[3]);
            int firstMinMS = Integer.parseInt(list[4]);
            int offSetCount = Integer.parseInt(list[5]);

            int time = 0;

            Coordinate startLat = new DegreeCoordinate(firstLat);
            Coordinate startLon = new DegreeCoordinate(firstLon);
            Point start = new Point(startLat, startLon);

            double lastLat = firstLat;
            double lastLon = firstLon;

            for(int i = 6; i < (3 * offSetCount) + 6; i+=3){
                lastLat += (Double.parseDouble(list[i])/1000000);
                lastLon += (Double.parseDouble(list[i+1])/1000000);
                time += Integer.parseInt(list[i+2]);
//                System.out.println(""+lastLat+ " " +lastLon);
            }

            Coordinate endLat = new DegreeCoordinate(lastLat);
            Coordinate endLon = new DegreeCoordinate(lastLon);
            Point end = new Point(endLat, endLon);

            double meters = EarthCalc.getVincentyDistance(start, end);
            System.out.println(""+meters);

            int mph = (int) Math.round((meters/time) * 2.23694);
            testLabel.setText(""+mph);
        }

        @Override
        public void onClosedStatus(boolean isClosed) {
            if (isClosed) {
                notifyDisconnected();
                if (isAutoConnected) {
                    displayState(ConnectionDisplayState.AUTOATTEMPTING);
                } else {
                    displayState(ConnectionDisplayState.DISCONNECTED);
                }
            } else {
                setIsConnected(true);
                if (isAutoConnected) {
                    displayState(ConnectionDisplayState.AUTOCONNECTED);
                } else {
                    displayState(ConnectionDisplayState.CONNECTED);
                }
            }
        }
    }

    @FXML
    private void handleSendMessageButton(ActionEvent event) {
        if (!sendTextField.getText().equals("")) {
            socket.sendMessage(sendTextField.getText());
            sentMsgsData.add(sendTextField.getText());
        }
    }

    @FXML
    private void handleConnectButton(ActionEvent event) {
        displayState(ConnectionDisplayState.ATTEMPTING);
        connect();
    }

    @FXML
    private void handleDisconnectButton(ActionEvent event) {
        socket.shutdown();
    }

    public void handleDoneTrajButton(ActionEvent actionEvent) {
        testLabel.setText("Wait...");
    }
}
