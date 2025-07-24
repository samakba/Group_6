package use_case.heatmap;

import entities.HeatmapPoint;
import java.util.List;
import java.util.function.Consumer;

public interface FetchEventsInputBoundary {
    void fetch(String category, String timeWindow, Consumer<List<HeatmapPoint>> callback);
}