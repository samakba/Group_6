package interface_adapter.heatmap;

import entities.HeatmapPoint;
import use_case.heatmap.FetchEventsInputBoundary;
import java.util.List;
import java.util.function.Consumer;

public class HeatmapController {

    private final FetchEventsInputBoundary fetchEventsInteractor;

    public HeatmapController(FetchEventsInputBoundary fetchEventsInteractor, HeatmapView view) { // Corrected typo here
        this.fetchEventsInteractor = fetchEventsInteractor;
    }

    /**
     * This method will be called by the HeatmapView to start the data fetching process.
     * It passes the request to the interactor.
     */
    public void fetch(String category, String timeWindow, Consumer<List<HeatmapPoint>> onDataReadyCallback) {
        fetchEventsInteractor.fetch(category, timeWindow, onDataReadyCallback);
    }
}