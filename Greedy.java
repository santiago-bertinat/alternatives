package alternatives;

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

public class Greedy {
  public static ArrayList<Segment> sorted_segments = new ArrayList<Segment>();
  public static ArrayList<Segment> segments = new ArrayList<Segment>();
  public static ArrayList<RsuType> rsu_types = new ArrayList<RsuType>();
  public static double qos = 0;
  public static double coverage_stop = 0.9;
  public static int coordinates_amount = 121;

  public static void main(String[] args) {
    loadSegments();
    loadRsuTypes();

    coverage_stop = Double.parseDouble(args[0]);

    boolean every_segment_covered = false;
    while (!every_segment_covered) {
      // Ascending order
      int i = sorted_segments.size() - 1;
      Collections.sort(sorted_segments);
      Segment segment = sorted_segments.get(i);

      every_segment_covered = true;
      while((segment.rsu != null || segment.vehicles_covered == segment.vehicles_amount) && i > 0) {
        i--;
        segment = sorted_segments.get(i);
      }

      if (i > 0) {
        Random generator = new Random();
        double rsu_position = generator.nextDouble();

        Point rsu_center = rsuPosition(segment, rsu_position);
        Rsu rsu = new Rsu(rsu_center, rsu_types.get(rsu_types.size() - 1));
        segment.rsu = rsu;

        calculateQos();
        every_segment_covered = false;
      }
    }

    saveResults("alternatives/qos_results.txt");
    saveResultsForAE();

    System.out.println("RESULT: ");
    System.out.println(qos);
    System.out.println("COST: ");
    double cost = 0;
    for (Segment segment : sorted_segments) {
      if (segment.rsu != null) {
        cost += segment.rsu.rsu_type.cost;
      }
    }
    System.out.println(cost);

    Collections.sort(sorted_segments);
    double qos_found = qos;

    while (qos >= qos_found * coverage_stop) {
      double[] qos_loss = new double[segments.size()];
      double old_qos = qos;

      System.out.println("WHILE");
      for (int i = 0; i < sorted_segments.size(); i++) {
        Segment segment = sorted_segments.get(i);

        Rsu aux_rsu = segment.rsu;
        if (segment.rsu == null || segment.rsu.rsu_type == null) {
          qos_loss[i] = 20000;
        }else {
          RsuType rsu_type = segment.rsu.rsu_type;
          int index_of_type = rsu_types.indexOf(rsu_type);

          if (index_of_type == 0) {
            segment.rsu = null;
          }else {
            segment.rsu.setRsuType(rsu_types.get(index_of_type - 1));
          }

          calculateQos();
          qos_loss[i] = old_qos - qos;

          if (index_of_type == 0) {
            segment.rsu = aux_rsu;
          }else {
            segment.rsu.setRsuType(rsu_type);
          }
        }
      }

      int min = 0;
      for (int i = 1; i < sorted_segments.size(); i++) {
        // System.out.println(qos_loss[i]);
        if (qos_loss[i] < qos_loss[min]) {
          min = i;
        }
      }

      System.out.println(min);
      Segment segment = sorted_segments.get(min);
      RsuType rsu_type = segment.rsu.rsu_type;
      int index_of_type = rsu_types.indexOf(rsu_type);

      if (index_of_type == 0) {
        System.out.println("sacamos antena");
        segment.rsu = null;
      }else {
        System.out.println("bajamos");
        segment.rsu.setRsuType(rsu_types.get(index_of_type - 1));
      }

      calculateQos();
      System.out.println(qos);
    }

    System.out.println("RESULT: ");
    System.out.println(qos);
    System.out.println("COST: ");
    cost = 0;
    for (Segment segment : segments) {
      if (segment.rsu != null) {
        cost += segment.rsu.rsu_type.cost;
      }
    }
    System.out.println(cost);

    saveResults("alternatives/cost_results.txt");
    saveResultsForAE();
  }

  private static void saveResults(String file_location) {
    try {
      File file = new File(file_location);

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter file_writer = new FileWriter(file.getAbsoluteFile());
      BufferedWriter buffer = new BufferedWriter(file_writer);
      for (Segment segment : segments) {
        if (segment.rsu != null) {
          Rsu rsu = segment.rsu;
          buffer.write(String.valueOf(rsu.center.x) + ',');
          buffer.write(String.valueOf(rsu.center.y) + ',');
          buffer.write(String.valueOf(rsu.radius) + ',');
          buffer.write(String.valueOf(rsu.rsu_type.cost) + '\n');
        }
      }

      buffer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void saveResultsForAE() {
    try {

      BufferedWriter buffer_initializations = new BufferedWriter(new FileWriter("alternatives/AE_initialization.txt", true));
      BufferedWriter buffer_results = new BufferedWriter(new FileWriter("alternatives/objectives_results.txt", true));

      double cost = 0;
      for (Segment segment : segments) {
        if (segment.rsu != null) {
          cost += segment.rsu.rsu_type.cost;
        }
      }
      buffer_results.write(qos + ", " + cost + "\n");
      buffer_results.close();

      for (Segment segment : segments) {
        if (segment.rsu != null) {
          RsuType rsu_type = segment.rsu.rsu_type;
          int index_of_type = rsu_types.indexOf(rsu_type) + 1;
          double position = Point.twoPointsDistance(segment.start, segment.rsu.center) / segment.distance();
          buffer_initializations.write(String.valueOf((float)(index_of_type + position)) + ",");
        } else {
          buffer_initializations.write("0,");
        }
      }

      buffer_initializations.write("\n");
      buffer_initializations.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
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
          double segment_length = segment.distance();

          // If rsu i belongs to k segment
          if (rsu_segment == segment){
            segment_coverage = segment_length / rsu.radius;
          }else {
            boolean start_inside = rsu.radius > Point.twoPointsDistance(rsu.center, segment.start);
            boolean end_inside = rsu.radius > Point.twoPointsDistance(rsu.center, segment.end);

            if (start_inside && end_inside){
              segment_coverage = segment_length;
            }
            else if (start_inside || end_inside){
              //Hay un punto adentro y uno afuera
              double alpha;
              double center_extreme_distance;

              if (start_inside){
                alpha = Segment.angleBetweenLines(new Segment(rsu.center, segment.start), segment);
                center_extreme_distance = Point.twoPointsDistance(rsu.center, segment.start);
              }else{
                alpha = Segment.angleBetweenLines(new Segment(rsu.center, segment.end), segment);
                center_extreme_distance = Point.twoPointsDistance(rsu.center, segment.end);
              }

              if (alpha != 0){
                double beta = Math.asin(center_extreme_distance * Math.sin(alpha) / (double)rsu.radius);
                segment_coverage = segment_length / (Math.sin(Math.PI - alpha - beta) * rsu.radius / Math.sin(alpha));
              }
              else{
                //Los 3 puntos están alineados
                if (start_inside){
                  segment_coverage = segment_length / (rsu.radius - Point.twoPointsDistance(rsu.center, segment.start));
                }
                else{
                  segment_coverage = segment_length / (rsu.radius - Point.twoPointsDistance(rsu.center, segment.end));
                }
              }
            }
            else if (rsu.center.pointToSegmentDistance(segment) < rsu.radius){
              //La recta intersecta el circulo, falta ver si el segmento tambien
              double m = rsu.center.pointToSegmentDistance(segment);
              double dAC = Point.twoPointsDistance(rsu.center, segment.start);
              double dBC = Point.twoPointsDistance(rsu.center, segment.end);
              double dAB = segment.distance();
              double dAQ = Math.sqrt(Math.pow(dAC, 2) - Math.pow(m, 2));
              double dQB = Math.sqrt(Math.pow(dBC, 2) - Math.pow(m, 2));
              if (dAQ < dAB && dQB < dAB){
                  //El segmento intersecta el circulo
                  double lambda = Math.sqrt(Math.pow(rsu.radius,2) - Math.pow(m,2));
                  segment_coverage = segment_length / (2 * lambda);
              }
            }

            int covered_vehicles_by_rus = (int)(segment_coverage * segment.vehicles_amount);
            // System.out.println("%%%%");
            // System.out.println(covered_vehicles_by_rus);
            // System.out.println(rsu.getCapacity());
            if (rsu.current_vehicles < rsu.getCapacity() && segment.vehicles_covered < segment.vehicles_amount) {
              double uncovered_vehicles = 0;
              if ((rsu.getCapacity() - rsu.current_vehicles) < (segment.vehicles_amount - segment.vehicles_covered)) {
                uncovered_vehicles = rsu.getCapacity() - rsu.current_vehicles;
              }else {
                uncovered_vehicles = segment.vehicles_amount - segment.vehicles_covered;
              }

              if (uncovered_vehicles > covered_vehicles_by_rus){
                rsu.current_vehicles  += covered_vehicles_by_rus;
                segment.vehicles_covered += covered_vehicles_by_rus;
              }else {
                rsu.current_vehicles  += uncovered_vehicles;
                segment.vehicles_covered += uncovered_vehicles;
              }
            }
          }
        }
      }
    }

    for (Segment segment : segments) {
        qos += segment.vehicles_covered;
    }
  }

  private static void loadSegments() {
    Point[] coordinates = new Point[coordinates_amount];
    double ideal_qos = 0;

    try{
      // Load coordinates
      File file = new File("alternatives/coordinates.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = null;
      String [] line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
        line_tokens = line.split(" ");

        Point point = new Point(Double.parseDouble(line_tokens[1]), Double.parseDouble(line_tokens[2]));
        coordinates[Integer.parseInt(line_tokens[0])] = point;
      }
      buffer.close();

      // Load segments
      file = new File("alternatives/instances/normal.txt");
      input_stream = new FileInputStream(file);
      buffer = new BufferedReader(new InputStreamReader(input_stream));

      line = null;
      line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
          line_tokens = line.split(" ");

          Point start = coordinates[Integer.parseInt(line_tokens[0])];
          Point end = coordinates[Integer.parseInt(line_tokens[1])];
          double vehicles_amount = Double.parseDouble(line_tokens[2]);


          ideal_qos += vehicles_amount;

          Segment segment = new Segment(start, end, vehicles_amount);
          segments.add(segment);
          sorted_segments.add(segment);
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
}
