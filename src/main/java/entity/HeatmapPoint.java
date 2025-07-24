package entities;

public class HeatmapPoint {
    private final double lat;
    private final double lng;
    private final double intensity;

    public HeatmapPoint(double lat, double lng, double intensity) {
        this.lat = lat;
        this.lng = lng;
        this.intensity = intensity;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getIntensity() {
        return intensity;
    }
}