package alternatives.components;

import java.util.*;

import alternatives.components.Point;
import alternatives.components.Rsu;

public class Segment implements Comparable<Segment> {
  public Point start;
  public Point end;
  public int importance;
  public double qos_potenciality;
  public Rsu rsu;

  public Segment(Point start, Point end, int importance) {
    this.start = start;
    this.end = end;
    this.importance = importance;

    this.qos_potenciality = importance * distance();
  }

  public double distance() {
    return Point.twoPointsDistance(start, end);
  }

  public int compareTo(Segment segment) {
    if (segment.qos_potenciality > qos_potenciality) return -1;
    if (segment.qos_potenciality == qos_potenciality) return 0;
    return 1;
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
