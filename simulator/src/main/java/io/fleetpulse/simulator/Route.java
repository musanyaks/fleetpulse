package io.fleetpulse.simulator;

import java.util.List;

/**
 * A highway corridor as an ordered waypoint list. Positions are interpolated
 * along cumulative segment distances — vehicles follow the road, not a
 * random walk. Coordinates are real Kenyan highway waypoints (approximate,
 * suitable for demo visualization, not turn-by-turn navigation).
 */
public final class Route {

    public record Leg(String id, String from, String to, List<double[]> points) {}

    private final String id;
    private final String from;
    private final String to;
    private final List<double[]> pts;          // [lat, lon]
    private final double[] cumKm;              // cumulative distance at each point
    private final double totalKm;

    public Route(String id, String from, String to, List<double[]> pts) {
        if (pts.size() < 2) throw new IllegalArgumentException("Route needs >= 2 points");
        this.id = id; this.from = from; this.to = to; this.pts = pts;
        this.cumKm = new double[pts.size()];
        double acc = 0;
        for (int i = 1; i < pts.size(); i++) {
            acc += haversineKm(pts.get(i - 1), pts.get(i));
            cumKm[i] = acc;
        }
        this.totalKm = acc;
    }

    public String id()   { return id; }
    public String from() { return from; }
    public String to()   { return to; }
    public double totalKm() { return totalKm; }

    /** Position at distance d (km) from the start, interpolated between waypoints. */
    public double[] positionAtKm(double d) {
        if (d <= 0) return pts.get(0);
        if (d >= totalKm) return pts.get(pts.size() - 1);
        int i = 1;
        while (cumKm[i] < d) i++;
        double segLen = cumKm[i] - cumKm[i - 1];
        double t = segLen == 0 ? 0 : (d - cumKm[i - 1]) / segLen;
        double[] a = pts.get(i - 1), b = pts.get(i);
        return new double[]{ a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t };
    }

    private static double haversineKm(double[] a, double[] b) {
        final double R = 6371, r = Math.PI / 180;
        double dLa = (b[0] - a[0]) * r, dLo = (b[1] - a[1]) * r;
        double h = Math.sin(dLa / 2) * Math.sin(dLa / 2)
                 + Math.cos(a[0] * r) * Math.cos(b[0] * r) * Math.sin(dLo / 2) * Math.sin(dLo / 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }
}
