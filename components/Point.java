package alternatives.components;

public class Point {
  public double x;
  public double y;

  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public static double twoPointsDistance(Point point1, Point point2){
    double d2r = Math.PI / 180.0;
    double dLat = deg2rad((point1.x - point2.x));
    double dLon = deg2rad((point1.y - point2.y));

    double x = (dLon) * Math.cos(deg2rad((point1.x + point2.x)/2));
    double dist = Math.sqrt(x*x + dLat*dLat) * 6371000;

    return (dist);
  }

  public double pointToSegmentDistance(Segment segment) {
    // Using Heron formula
    double dps, dpe, dse;
    dps = twoPointsDistance(this, segment.start);
    dpe = twoPointsDistance(this, segment.end);
    dse = segment.distance;

    double s = (dps + dpe + dse) / (double)2;
    double area= Math.sqrt(s*(s-dps)*(s-dpe)*(s-dse));
    return 2*area/(double)dse;
  }

  private static double deg2rad(double deg) {
    return (deg * Math.PI / 180.0);
  }

  private static double rad2deg(double rad) {
    return (rad * 180.0 / Math.PI);
  }
}
