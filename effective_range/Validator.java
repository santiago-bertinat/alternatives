package alternatives.effective_range;

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

    for (Segment segment : segments){

      double divitions = 10;
      double module_section = segment.distance / divitions;
      double intersections = 0;

      double coverered_distance = 0;

      double x_length = Math.abs(segment.start.x - segment.end.x) / divitions;
      double y_length = Math.abs(segment.start.y - segment.end.y) / divitions;

      for (int j = 0; j < divitions; j++) {
        double x = segment.start.x;
        if (segment.start.x < segment.end.x) {
          x = segment.start.x + j * x_length;
        }else if (segment.start.x > segment.end.x) {
          x = segment.start.x - j * x_length;
        }

        double y = segment.start.y;
        if (segment.start.y < segment.end.y) {
          y = segment.start.y + j * y_length;
        }else if (segment.start.y > segment.end.y) {
          y = segment.start.y - j * y_length;
        }

        Point aux_point = new Point(x, y);
        for (Segment aux_segment : segments) {
          Rsu rsu = aux_segment.rsu;
          if (rsu != null && rsu.belongsToCircle(aux_point)) {
            intersections++;
            break;
          }
        }
      }

      qos += segment.vehicles_amount * (intersections / divitions);
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
      File file = new File("alternatives/effective_range/validator_input.txt");
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
