package interface_adapter.heatmap;

import entities.HeatmapPoint;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

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
     * The constructor is now a no-argument constructor again.
     */
    public HeatmapView() {
        super(new BorderLayout());
        initGui();
    }

    /**
     * Add a setter so the AppBuilder can provide the controller after initialization.
     */
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

        // Set a custom user-agent string to mimic a standard browser
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                isMapReady = true;
                System.out.println("Map loaded successfully!");

                SwingUtilities.invokeLater(() -> {
                    removeLoadingLabel();
                    // Now that the map is ready, fetch the initial data.
                    fetchAndDisplayData("all", "last_day");
                });

            } else if (newState == Worker.State.FAILED) {
                System.err.println("Failed to load map: " + webEngine.getLoadWorker().getException());
            }
        });
        
        URL url = getClass().getResource("/heatmap.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("Could not find heatmap.html resource!");
        }

        jfxPanel.setScene(new Scene(webView));
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

    public void fetchAndDisplayData(String category, String timeWindow) {
        if (heatmapController != null) {
            // Use the controller to fetch data asynchronously.
            heatmapController.fetch(category, timeWindow, this::setHeatmapData);
        } else {
            System.err.println("Error: HeatmapController has not been set on the HeatmapView.");
        }
    }

    public void setHeatmapData(List<HeatmapPoint> points) {
        if (!isMapReady) {
            Timer timer = new Timer(1000, e -> setHeatmapData(points));
            timer.setRepeats(false);
            timer.start();
            return;
        }

        String jsonString = points.stream()
                .map(p -> String.format(java.util.Locale.US, "{\"lat\":%f,\"lng\":%f,\"weight\":%f}",
                        p.getLat(), p.getLng(), Math.min(p.getIntensity() / 10.0, 1.0)))
                .collect(Collectors.joining(",", "[", "]"));

        Platform.runLater(() -> webEngine.executeScript("updateHeatmap('" + jsonString + "');"));
    }
}