package use_case.heatmap;

import entities.HeatmapPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FetchEventsInteractor implements FetchEventsInputBoundary {
    @Override
    public void fetch(String category, String timeWindow, Consumer<List<HeatmapPoint>> callback) {
        // This is a placeholder implementation that returns dummy data.
        // In a real application, you would make a network API call here.
        System.out.println("FetchEventsInteractor: Fetching dummy data...");

        List<HeatmapPoint> dummyPoints = new ArrayList<>();
        // Add some test points around Toronto
        dummyPoints.add(new HeatmapPoint(43.6532, -79.3832, 10)); // Downtown
        dummyPoints.add(new HeatmapPoint(43.6629, -79.3957, 5));  // U of T
        dummyPoints.add(new HeatmapPoint(43.6426, -79.3871, 8));  // Rogers Centre
        dummyPoints.add(new HeatmapPoint(43.6709, -79.3935, 4));  // Yorkville
        dummyPoints.add(new HeatmapPoint(43.6577, -79.3788, 7));  // Dundas Square

        // Simulate a network delay of 1 second
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("FetchEventsInteractor: Returning " + dummyPoints.size() + " dummy points.");
                // Pass the data back to the view via the callback
                callback.accept(dummyPoints);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}