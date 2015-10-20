package alternatives.components;

import java.util.*;

import alternatives.components.Point;
import alternatives.components.Rsu;

public class Segment implements Comparable<Segment> {
  public Point start;
  public Point end;
  public double vehicles_amount;
  public double distance;
  public int vehicles_covered;
  public Rsu rsu;

  public Segment(Point start, Point end, double distance, double vehicles_amount) {
    this.start = start;
    this.end = end;
    this.vehicles_amount = vehicles_amount;
    this.vehicles_covered = 0;
    this.distance = distance;
  }

  public Segment(Point start, Point end) {
    this.start = start;
    this.end = end;
  }

  public int compareTo(Segment segment) {
    double uncovered_vehicles = segment.vehicles_amount - segment.vehicles_covered;
    if (uncovered_vehicles > (vehicles_amount - vehicles_covered)) return -1;
    if (uncovered_vehicles == (vehicles_amount - vehicles_covered)) return 0;
    return 1;
  }

  public static double angleBetweenLines(Segment segment1, Segment segment2) {
    // Apply cosine theorem
    double a = Point.twoPointsDistance(segment1.start, segment1.end);
    double b = Point.twoPointsDistance(segment2.start, segment2.end);
    double c = Point.twoPointsDistance(segment1.start, segment2.end);

    if((a + b == c) || (a + c == b) || (c + b == a)) {
      return 0;
    }
    double cosine = (c*c - a*a - b*b) / (-2 * a * b);
    if (cosine > 1) {
      return 0;
    }

    return Math.acos(cosine);
    }

  public void print() {
    System.out.println("#####");

    System.out.print("start x: ");
    System.out.println(start.x);
    System.out.print("start y: ");
    System.out.println(start.y);
    System.out.print("end x: ");
    System.out.println(end.x);
    System.out.print("end y: ");
    System.out.println(end.y);
  }
}
