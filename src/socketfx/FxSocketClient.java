package socketfx;

//Implementation of GenericSocket that creates a specific listener client socket
public class FxSocketClient extends GenericSocket
        implements SocketListener {

    public String host;
    private SocketListener fxListener;

    //Implements the SocketListener onMessage and onClosedStatus methods to perform function those states
    @Override
    public void onMessage(final String line) {
        System.out.println(line);
        javafx.application.Platform.runLater(() -> fxListener.onMessage(line));
    }

    @Override
    public void onClosedStatus(final boolean isClosed) {
        javafx.application.Platform.runLater(() -> fxListener.onClosedStatus(isClosed));
    }
    
    public FxSocketClient(SocketListener fxListener,
            String host, int port) {
        super(port);
        this.host = host;
        this.fxListener = fxListener;
    }
}
