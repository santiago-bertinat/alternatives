package alternatives.components;

import alternatives.components.Segment;
import alternatives.components.Point;
import alternatives.components.RsuType;

public class Rsu {
  public Point center;
  public double radius;
  public RsuType rsu_type;
  public int current_vehicles;
  // For Knapsack AE Initialization
  public int segment_id;

  public Rsu(Point center, RsuType rsu_type) {
    this.center = center;
    this.radius = rsu_type.radius;
    this.rsu_type = rsu_type;
  }

  // For Knapsack AE Initialization
  public Rsu(Point center, RsuType rsu_type, int segment_id) {
    this.center = center;
    this.radius = rsu_type.radius;
    this.rsu_type = rsu_type;
    this.segment_id = segment_id;
  }

  public void setRsuType(RsuType rsu_type) {
    if (rsu_type == null) {
      this.rsu_type = null;
      this.radius = 0;
    }else {
      this.rsu_type = rsu_type;
      this.radius = rsu_type.radius;
    }
  }

  public boolean belongsToCircle(Point x) {
    return radius >= Point.twoPointsDistance(center, x);
  }

  public double getCapacity(){
    return rsu_type.capacity;
  }
}
