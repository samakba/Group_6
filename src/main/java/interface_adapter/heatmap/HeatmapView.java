package interface_adapter.heatmap;

import entities.HeatmapPoint;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class HeatmapView extends JPanel {
    private final JFXPanel jfxPanel = new JFXPanel();
    private WebEngine webEngine;
    private volatile boolean isMapReady = false;

    // This will hold the reference to the controller
    private HeatmapController heatmapController;

    /**
     * A bridge object to print JavaScript console messages to the Java console.
     */
    public class JavaConsole {
        public void log(String message) {
            System.out.println("[JS Console] " + message);
        }
    }

    public HeatmapView() {
        super(new BorderLayout());
        jfxPanel.setPreferredSize(new Dimension(800, 600));
        jfxPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
        initGui();
    }

    public void setController(HeatmapController controller) {
        this.heatmapController = controller;
    }

    private void initGui() {
        JLabel loadingLabel = new JLabel("Loading map...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Serif", Font.BOLD, 24));
        add(loadingLabel, BorderLayout.CENTER);
        add(jfxPanel, BorderLayout.CENTER);
        Platform.runLater(this::initFX);
    }

    private void initFX() {
        WebView webView = new WebView();
        webEngine = webView.getEngine();

        // Enable JavaScript console logging
        webEngine.setOnAlert(event -> System.out.println("[JS Alert] " + event.getData()));

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                isMapReady = true;
                System.out.println("HeatmapView: Page load SUCCEEDED.");

                // Bridge Java console to JavaScript console.log
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConsole", new JavaConsole());
                webEngine.executeScript("console.log = function(message) { javaConsole.log(message); };");

                // Set an error handler for any uncaught JavaScript exceptions
                webEngine.executeScript("window.onerror = function(msg, url, line) { console.log('ERROR: ' + msg + ' at ' + url + ':' + line); return true; };");

                // Confirm the bridge is working
                webEngine.executeScript("console.log('JavaScript console bridge initiated.')");

                SwingUtilities.invokeLater(() -> {
                    removeLoadingLabel();
                    fetchAndDisplayData("all", "week");
                });

            } else if (newState == Worker.State.FAILED) {
                System.err.println("--- HeatmapView: Page load FAILED. ---");
                if (webEngine.getLoadWorker().getException() != null) {
                    webEngine.getLoadWorker().getException().printStackTrace();
                }
                SwingUtilities.invokeLater(this::showErrorMessage);
            }
        });

        // Try to load the OpenStreetMap version first, fallback to Google Maps
        URL url = getClass().getResource("/web/heatmap_osm.html");
        if (url == null) {
            url = getClass().getResource("/web/heatmap.html");
        }

        if (url != null) {
            System.out.println("HeatmapView: Attempting to load resource: " + url.toExternalForm());
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("--- HeatmapView: CRITICAL ERROR: Could not find heatmap HTML files ---");
            showErrorMessage();
        }

        jfxPanel.setScene(new Scene(webView));
    }

    private void showErrorMessage() {
        removeAllComponents();
        JLabel errorLabel = new JLabel("<html><center>Error loading heatmap.<br/>Please check your internet connection.</center></html>", SwingConstants.CENTER);
        errorLabel.setFont(new Font("Serif", Font.BOLD, 16));
        errorLabel.setForeground(Color.RED);
        add(errorLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void removeLoadingLabel() {
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                remove(comp);
                revalidate();
                repaint();
                break;
            }
        }
    }

    private void removeAllComponents() {
        removeAll();
        add(jfxPanel, BorderLayout.CENTER);
    }

    public void fetchAndDisplayData(String category, String timeWindow) {
        if (heatmapController != null) {
            System.out.println("HeatmapView: Fetching data for category=" + category + ", timeWindow=" + timeWindow);
            heatmapController.fetch(category, timeWindow, this::setHeatmapData);
        } else {
            System.err.println("Error: HeatmapController has not been set.");
        }
    }

    public void setHeatmapData(List<HeatmapPoint> points) {
        if (!isMapReady) {
            System.out.println("HeatmapView: Map not ready, delaying data set.");
            Timer timer = new Timer(1000, e -> setHeatmapData(points));
            timer.setRepeats(false);
            timer.start();
            return;
        }

        if (points == null || points.isEmpty()) {
            System.out.println("HeatmapView: No data points to display");
            return;
        }

        String jsonString = points.stream()
                .map(p -> String.format(java.util.Locale.US, "{\"lat\":%f,\"lng\":%f,\"weight\":%f}",
                        p.getLat(), p.getLng(), Math.max(0.1, Math.min(1.0, p.getIntensity() / 10.0))))
                .collect(Collectors.joining(",", "[", "]"));

        System.out.println("HeatmapView: Pushing " + points.size() + " points to JavaScript view.");
        System.out.println("HeatmapView: JSON data: " + jsonString);

        Platform.runLater(() -> {
            try {
                webEngine.executeScript("updateHeatmap('" + jsonString + "');");
                System.out.println("HeatmapView: Successfully called updateHeatmap");
            } catch (Exception e) {
                System.err.println("HeatmapView: Error executing JavaScript: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}