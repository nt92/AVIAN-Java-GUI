package socketclientfx;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.peertopark.java.geocalc.Coordinate;
import com.peertopark.java.geocalc.DegreeCoordinate;
import com.peertopark.java.geocalc.EarthCalc;
import com.peertopark.java.geocalc.Point;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import socketfx.FxSocketClient;
import socketfx.SocketListener;

public class FXMLDocumentController implements Initializable {

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
    private TextField hostTextFieldSend;
    @FXML
    private TextField portTextFieldSend;
    @FXML
    private Label testLabel;
    @FXML
    private Label connectedLabel;
    @FXML
    private ToggleGroup group;

    private final static Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private boolean connected;
    private volatile boolean isAutoConnected;

    public enum ConnectionDisplayState { DISCONNECTED, ATTEMPTING, CONNECTED, AUTOCONNECTED, AUTOATTEMPTING }

    private FxSocketClient socket;

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
                Integer.valueOf(portTextField.getText()));
        socket.connect();
    }

    private void displayState(ConnectionDisplayState state) {
        switch (state) {
            case DISCONNECTED:
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
                connectedLabel.setText("Not connected");
                break;
            case ATTEMPTING:
            case AUTOATTEMPTING:
                connectButton.setDisable(true);
                disconnectButton.setDisable(true);
                connectedLabel.setText("Attempting connection");
                break;
            case CONNECTED:
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                connectedLabel.setText("Connected");
                break;
            case AUTOCONNECTED:
                connectButton.setDisable(true);
                disconnectButton.setDisable(true);
                connectedLabel.setText("Connected");
                break;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setIsConnected(false);
        isAutoConnected = false;
        displayState(ConnectionDisplayState.DISCONNECTED);

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
//            testLabel.setText("Working!");
            System.out.println("yee");
            processString(line);
        }

        //Algorithm that processes the given string and outputs a recommended MPH
        public void processString(String line){
            String[] list = line.split(",");
            for(String s : list){
                s.replace(" ", "");
            }

            int messageId = Integer.parseInt(list[0]);
            double firstLat = Double.parseDouble(list[1])/10000000;
            double firstLon = Double.parseDouble(list[2])/10000000;
            int firstMinMin = Integer.parseInt(list[3]);
            int firstMinMS = Integer.parseInt(list[4]);
            int offSetCount = Integer.parseInt(list[5]);

            double time = 0;

            Coordinate startLat = new DegreeCoordinate(firstLat);
            Coordinate startLon = new DegreeCoordinate(firstLon);
            Point start = new Point(startLat, startLon);

            double lastLat = firstLat;
            double lastLon = firstLon;

            for(int i = 6; i < (3 * offSetCount) + 6; i+=3){
                lastLat += (Double.parseDouble(list[i])/10000000);
                lastLon += (Double.parseDouble(list[i+1])/10000000);
                time += Double.parseDouble(list[i+2])/1000;
//                System.out.println(""+lastLat+ " " +lastLon);
            }

            Coordinate endLat = new DegreeCoordinate(lastLat);
            Coordinate endLon = new DegreeCoordinate(lastLon);
            Point end = new Point(endLat, endLon);

            double meters = EarthCalc.getDistance(start, end);
            double meters1 = EarthCalc.getBearing(start, end);
            double meters2 = EarthCalc.getHarvesineDistance(start, end);
            System.out.println(""+meters+" "+meters1+" "+meters2);

            int mph = (int) Math.round((meters/time) * 2.23694);
            if(mph > 30){
                mph = 30;
            }
            System.out.println(""+mph);
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
    public void handleSendLaneButton(ActionEvent actionEvent) {
        String s = (String) group.getSelectedToggle().toString();
        s = s.substring(s.indexOf("'") + 1);
        s = s.substring(0, s.indexOf("'"));
        System.out.println(hostTextFieldSend.getText()+portTextFieldSend.getText()+s);
        socket.send(hostTextFieldSend.getText(), portTextFieldSend.getText(), "LaneNumber,"+s);
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

    @FXML
    public void handleDoneTrajButton(ActionEvent event) {
        testLabel.setText("Wait");
    }
}
