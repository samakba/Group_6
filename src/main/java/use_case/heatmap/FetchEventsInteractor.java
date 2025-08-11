package use_case.heatmap;

import entities.HeatmapPoint;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class FetchEventsInteractor implements FetchEventsInputBoundary {

    private static final String API_KEY = "RRZUfNORGsVObWaAOqaffqVdxZOURIsS";
    private static final String BASE = "https://app.ticketmaster.com/discovery/v2/events.json";

    // Toronto center; radius in KILOMETRES
    private static final double LAT = 43.6532, LON = -79.3832;
    private static final int RADIUS_KM = 75;
    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES = 5; // pages 0..4

    private static final OkHttpClient HTTP = new OkHttpClient();

    @Override
    public void fetch(String category, String timeWindow, Consumer<List<HeatmapPoint>> cb) {
        new Thread(() -> {
            try {
                cb.accept(fetchWeekWithPaging());
            } catch (Exception e) {
                e.printStackTrace();
                cb.accept(Collections.emptyList());
            }
        }).start();
    }

    private List<HeatmapPoint> fetchWeekWithPaging() throws IOException {
        String[] range = weekUtc();
        String base = BASE + "?apikey=" + API_KEY +
                "&latlong=" + LAT + "," + LON +
                "&radius=" + RADIUS_KM + "&unit=km" +
                "&size=" + PAGE_SIZE +
                "&startDateTime=" + range[0] +
                "&endDateTime=" + range[1];

        Map<String, Integer> countByVenue = new HashMap<>();
        Map<String, double[]> coordByVenue = new HashMap<>();

        int page = 0, totalPages = 1;
        while (page < totalPages && page < MAX_PAGES) {
            Request req = new Request.Builder().url(base + "&page=" + page).build();
            try (Response resp = HTTP.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) break;
                JSONObject root = new JSONObject(resp.body().string());

                JSONObject emb = root.optJSONObject("_embedded");
                JSONArray events = emb != null ? emb.optJSONArray("events") : null;
                if (events == null) break;

                for (int i = 0; i < events.length(); i++) {
                    JSONObject v = firstVenue(events.getJSONObject(i));
                    if (v == null) continue;
                    String vid = v.optString("id", null);
                    JSONObject loc = v.optJSONObject("location");
                    if (vid == null || loc == null) continue;

                    double la = toD(loc.optString("latitude", null));
                    double lo = toD(loc.optString("longitude", null));
                    if (Double.isNaN(la) || Double.isNaN(lo)) continue;

                    countByVenue.put(vid, countByVenue.getOrDefault(vid, 0) + 1);
                    coordByVenue.putIfAbsent(vid, new double[]{la, lo});
                }

                JSONObject pg = root.optJSONObject("page");
                if (pg != null) totalPages = pg.optInt("totalPages", totalPages);
            }
            page++;
        }

        List<HeatmapPoint> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : countByVenue.entrySet()) {
            double[] ll = coordByVenue.get(e.getKey());
            if (ll != null) out.add(new HeatmapPoint(ll[0], ll[1], e.getValue() * 2.0));
        }
        return out;
    }

    private static JSONObject firstVenue(JSONObject ev) {
        JSONObject emb = ev.optJSONObject("_embedded");
        JSONArray vs = (emb == null) ? null : emb.optJSONArray("venues");
        return (vs == null || vs.length() == 0) ? null : vs.optJSONObject(0);
    }

    private static double toD(String s) {
        if (s == null) return Double.NaN;
        try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
    }

    private static String[] weekUtc() {
        Instant now = Instant.now(), end = now.plusSeconds(7L * 24 * 3600);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC);
        return new String[]{ f.format(now), f.format(end) };
    }
}
