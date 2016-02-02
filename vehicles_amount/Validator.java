package alternatives.vehicles_amount;

import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;

import alternatives.components.Rsu;
import alternatives.components.RsuType;
import alternatives.components.Segment;
import alternatives.components.Point;

public class Validator {
  public static ArrayList<Segment> segments = new ArrayList<Segment>();
  public static ArrayList<RsuType> rsu_types = new ArrayList<RsuType>();
  public static ArrayList<Rsu> road_side_units = new ArrayList<Rsu>();
  public static double qos = 0;

  public static void main(String[] args) {
    loadSegments();
    loadRsuTypes();
    loadResult();

    double expected_qos = Double.parseDouble(args[0]);
    double expected_cost = Double.parseDouble(args[1]);

    calculateQos();

    System.out.println("EXPECTED RESULT");
    System.out.println("qos: ");
    System.out.println(expected_qos);
    System.out.println("cost: ");
    System.out.println(expected_cost);

    System.out.println("RESULT");
    System.out.println("qos: ");
    System.out.println(qos);
    System.out.println("cost: ");

    double cost = 0;
    for (Segment segment : segments) {
      if (segment.rsu != null) {
        cost += segment.rsu.rsu_type.cost;
      }
    }
    System.out.println(cost);
  }

  private static Point rsuPosition(Segment segment, double rsu_position) {
    double x_length = Math.abs(segment.start.x - segment.end.x) * rsu_position;
    double y_length = Math.abs(segment.start.y - segment.end.y) * rsu_position;

    double x = segment.start.x;
    if (segment.start.x < segment.end.x) {
      x = segment.start.x + x_length;
    }else if (segment.start.x > segment.end.x) {
      x = segment.start.x - x_length;
    }

    double y = segment.start.y;
    if (segment.start.y < segment.end.y) {
      y = segment.start.y + y_length;
    }else if (segment.start.y > segment.end.y) {
      y = segment.start.y - y_length;
    }

    return new Point(x, y);
  }

  private static void calculateQos() {
    qos = 0;

    // Remove previous calculations
    for (Segment segment : segments){
      if (segment.rsu != null) {
        segment.rsu.current_vehicles = 0;
      }
      segment.vehicles_covered = 0;
    }
    // Iterate through the segments
    for (Segment rsu_segment : segments){
      if (rsu_segment.rsu != null) {

        Rsu rsu = rsu_segment.rsu;
        for (Segment segment : segments){
          double segment_coverage = 0;
          double segment_length = segment.distance;

          boolean start_inside = rsu.radius > Point.twoPointsDistance(rsu.center, segment.start);
          boolean end_inside = rsu.radius > Point.twoPointsDistance(rsu.center, segment.end);

          if (start_inside && end_inside){
            segment_coverage = 1;
          }
          else if (start_inside || end_inside){
            //Hay un punto adentro y uno afuera
            double alpha;
            double center_extreme_distance;

            if (start_inside){
              alpha = Segment.angleBetweenLines(new Segment(rsu.center, segment.start), segment);
              center_extreme_distance = Point.twoPointsDistance(rsu.center, segment.start);
            }else{
              alpha = Segment.angleBetweenLines(new Segment(rsu.center, segment.end), new Segment(segment.end, segment.start));
              center_extreme_distance = Point.twoPointsDistance(rsu.center, segment.end);
            }

            if (alpha != 0){
              double beta = Math.asin(center_extreme_distance * Math.sin(alpha) / (double)rsu.radius);
              segment_coverage = (Math.sin(Math.PI - alpha - beta) * rsu.radius / Math.sin(alpha)) / segment_length;
            }
            else{
              //Los 3 puntos están alineados
              if (start_inside){
                segment_coverage = (rsu.radius - Point.twoPointsDistance(rsu.center, segment.start)) / segment_length;
              }
              else{
                segment_coverage = (rsu.radius - Point.twoPointsDistance(rsu.center, segment.end)) / segment_length;
              }
            }
          }
          else if (rsu.center.pointToSegmentDistance(segment) < rsu.radius){
            //La recta intersecta el circulo, falta ver si el segmento tambien
            double m = rsu.center.pointToSegmentDistance(segment);
            double dAC = Point.twoPointsDistance(rsu.center, segment.start);
            double dBC = Point.twoPointsDistance(rsu.center, segment.end);
            double dAB = segment.distance;
            double dAQ = Math.sqrt(Math.pow(dAC, 2) - Math.pow(m, 2));
            double dQB = Math.sqrt(Math.pow(dBC, 2) - Math.pow(m, 2));
            if (dAQ < dAB && dQB < dAB){
                //El segmento intersecta el circulo
                double lambda = Math.sqrt(Math.pow(rsu.radius,2) - Math.pow(m,2));
                segment_coverage = (2 * lambda) / segment_length;
            }
          }

          int covered_vehicles_by_rus = (int)(segment_coverage * segment.vehicles_amount);

          if (rsu.current_vehicles < rsu.getCapacity() && segment.vehicles_covered < segment.vehicles_amount) {
            double uncovered_vehicles = 0;
            double rsu_actual_capacity = rsu.getCapacity() - rsu.current_vehicles;
            double segment_uncovered_vehicles = segment.vehicles_amount - segment.vehicles_covered;

            uncovered_vehicles = Math.min(Math.min(rsu_actual_capacity, segment_uncovered_vehicles), covered_vehicles_by_rus);
            rsu.current_vehicles  += uncovered_vehicles;
            segment.vehicles_covered += uncovered_vehicles;
          }
        }
      }
    }

    for (Segment segment : segments) {
        qos += segment.vehicles_covered;
    }
  }

  private static void loadSegments() {
    double ideal_qos = 0;

    try{
      // Load segments
      File file = new File("alternatives/instances/normal.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = null;
      String [] line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
          line_tokens = line.split(",");

          Point start = new Point(Double.parseDouble(line_tokens[1]), Double.parseDouble(line_tokens[0]));
          Point end = new Point(Double.parseDouble(line_tokens[3]), Double.parseDouble(line_tokens[2]));
          double vehicles_amount = Double.parseDouble(line_tokens[5]);
          double distance = Point.twoPointsDistance(start, end);

          ideal_qos += vehicles_amount;

          Segment segment = new Segment(start, end, distance, vehicles_amount);
          segments.add(segment);
      }
      buffer.close();


      System.out.println("IDEAL RESULT:");
      System.out.println(ideal_qos);
    }catch(IOException e){
      System.out.println(e + " there was a problem reading the file");
    }
  }

  private static void loadRsuTypes() {
    try{
      File file = new File("alternatives/rsu_types.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = null;
      String [] line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
        line_tokens = line.split(" ");

        RsuType rsu_type = new RsuType(Double.parseDouble(line_tokens[2]), Double.parseDouble(line_tokens[3]), Double.parseDouble(line_tokens[4]));
        rsu_types.add(rsu_type);
      }
      buffer.close();

    }catch(IOException e){
      System.out.println(e + " there was a problem reading the file");
    }
  }

  private static void loadResult() {
    try{
      File file = new File("alternatives/vehicles_amount/validator_input.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = line = buffer.readLine();
      String [] line_tokens = line.split(",");
      for (int i = 0; i < line_tokens.length; i++){
        double rsu_data = Double.parseDouble(line_tokens[i]);
        int rsu_type_id = (int)Math.floor(rsu_data);
        if (rsu_type_id != 0){
            // There is a RSU on the segment
            RsuType rsu_type = rsu_types.get(rsu_type_id - 1);
            Segment segment = segments.get(i);

            Point rsu_center = rsuPosition(segment, rsu_data - rsu_type_id);
            Rsu rsu = new Rsu(rsu_center, rsu_type);
            segment.rsu = rsu;
            road_side_units.add(rsu);
        }
      }
      buffer.close();

    }catch(IOException e){
      System.out.println(e + " there was a problem reading the file");
    }
  }
}
