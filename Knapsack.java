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

public class Knapsack {
  public static ArrayList<Segment> segments = new ArrayList<Segment>();
  public static ArrayList<RsuType> rsu_types = new ArrayList<RsuType>();
  public static double qos = 0;
  public static double coverage_acceptance = 0.8;
  public static int cost_interval_value = 200;
  public static int cost_intervals = 130;

  public static void main(String[] args) {
    loadSegments();
    loadRsuTypes();

    coverage_acceptance = Double.parseDouble(args[0]);

    double[][] qos_grid = new double[segments.size() + 1][cost_intervals + 1];
    ArrayList<Rsu>[][] rsus_grid = new ArrayList[segments.size() + 1][cost_intervals + 1];

    for (int i = 0; i <= segments.size(); i++) {
      qos_grid[i][0] = 0;
      rsus_grid[i][0] = new ArrayList<Rsu>();
    }

    for (int i = 0; i <= cost_intervals; i++) {
      qos_grid[0][i] = 0;
      rsus_grid[0][i] = new ArrayList<Rsu>();
    }

    for (int i = 1; i <= segments.size(); i++) {
      for (int j = 1; j <= cost_intervals; j++) {
        rsus_grid[i][j] = new ArrayList<Rsu>();
      }
    }

    for (int i = 1; i <= segments.size(); i++) {
      Segment segment = segments.get(i - 1);
      Random generator = new Random();
      double rsu_position = generator.nextDouble();
      Point rsu_center = rsuPosition(segment, rsu_position);
      for (int j = 1; j <= cost_intervals; j++) {
        int available_budget = j * cost_interval_value;
        if (rsu_types.get(0).cost > available_budget) {
          qos_grid[i][j] = qos_grid[i-1][j];
          rsus_grid[i][j].addAll(rsus_grid[i-1][j]);
        } else {
          double previous_qos = qos_grid[i-1][j];
          // System.out.print("iteration : ");
          // System.out.print(i);
          // System.out.print(" , ");
          // System.out.println(j);
          // System.out.print("available_budget: ");
          // System.out.println(available_budget);

          Rsu rsu_0 = new Rsu(rsu_center, rsu_types.get(0));
          Rsu rsu_1 = new Rsu(rsu_center, rsu_types.get(1));
          Rsu rsu_2 = new Rsu(rsu_center, rsu_types.get(2));

          int current_money_index = (int)((available_budget - rsu_0.rsu_type.cost) / cost_interval_value);
          ArrayList<Rsu> previous_rsus_0 = rsus_grid[i-1][current_money_index];
          double new_qos_0 = getQos(previous_rsus_0, rsu_0);

          current_money_index = (int)((available_budget - rsu_1.rsu_type.cost) / cost_interval_value);
          ArrayList<Rsu> previous_rsus_1 = rsus_grid[i-1][current_money_index];
          double new_qos_1 = rsu_types.get(1).cost < available_budget ? getQos(previous_rsus_1, rsu_1) : 0;

          current_money_index = (int)((available_budget - rsu_2.rsu_type.cost) / cost_interval_value);
          ArrayList<Rsu> previous_rsus_2 = rsus_grid[i-1][current_money_index];
          double new_qos_2 = rsu_types.get(2).cost < available_budget ? getQos(previous_rsus_2, rsu_2) : 0;

          double current_qos = max(previous_qos, new_qos_0, new_qos_1, new_qos_2);
          qos_grid[i][j] = current_qos;

          if (current_qos == new_qos_0) {
            rsus_grid[i][j].addAll(previous_rsus_0);
            rsus_grid[i][j].add(rsu_0);
          } else if (current_qos == new_qos_1) {
            rsus_grid[i][j].addAll(previous_rsus_1);
            rsus_grid[i][j].add(rsu_1);
          } else if (current_qos == new_qos_2)  {
            rsus_grid[i][j].addAll(previous_rsus_2);
            rsus_grid[i][j].add(rsu_2);
          } else {
            rsus_grid[i][j].addAll(rsus_grid[i-1][j]);
          }
        }
      }
    }

    saveResults("alternatives/qos_results.txt", qos_grid, rsus_grid);
    saveResultsForAE();

    System.out.println("RESULT: ");
    System.out.println(qos_grid[segments.size()][cost_intervals]);
    System.out.println("COST: ");
    double cost = 0;
    for (Rsu rsu : rsus_grid[segments.size()][cost_intervals]) {
      if (rsu.rsu_type != null)
        cost += rsu.rsu_type.cost;
    }
    System.out.println(String.valueOf(cost));
  }

  private static void saveResults(String file_location, double[][] qos_grid,  ArrayList<Rsu>[][] rsus_grid) {
    try {
      File file = new File(file_location);

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter file_writer = new FileWriter(file.getAbsoluteFile());
      BufferedWriter buffer = new BufferedWriter(file_writer);

      buffer.write("Cost,Quality of Service" + '\n');
      for (int i = 1; i <= cost_intervals; i++) {
        double cost = 0;
        for (Rsu rsu : rsus_grid[segments.size()][i]) {
          if (rsu.rsu_type != null)
            cost += rsu.rsu_type.cost;
        }
        buffer.write(String.valueOf((int)cost) + ',');
        double qos_for_cost = qos_grid[segments.size()][i];
        buffer.write(String.valueOf((int)qos_for_cost) + '\n');
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
      for (Rsu rsu : rsus_grid[segments.size()][cost_intervals]) {
        if (rsu.rsu_type != null)
          cost += rsu.rsu_type.cost;
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
    double lat_ini = segment.start.x;
    double lng_ini = segment.start.y;
    double lat_fin = segment.end.x;
    double lng_fin = segment.end.y;

    double a =(lng_fin-lng_ini)/(double)(lat_fin-lat_ini);
    double x;
    double y;

    if (lat_fin>lat_ini)
      x = lat_ini + Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*rsu_position/(double)Math.sqrt(1+(a*a));
    else
      x = lat_ini - Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*rsu_position/(double)Math.sqrt(1+(a*a));

    if (lng_fin>lng_ini)
      y = lng_ini + Math.abs(Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*a*rsu_position/(double)Math.sqrt(1+(a*a)));
    else
      y = lng_ini - Math.abs(Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*a*rsu_position/(double)Math.sqrt(1+(a*a)));

    return new Point(x, y);
  }

  private static double getQos(ArrayList<Rsu> previous_road_side_units, Rsu new_road_side_unit) {
    ArrayList<Segment> intersected_segments = new ArrayList<Segment>();
    qos = 0;
    ArrayList<Rsu> road_side_units = new ArrayList<Rsu>();
    if (previous_road_side_units != null) {
      road_side_units.addAll(previous_road_side_units);
    }
    road_side_units.add(new_road_side_unit);

    // System.out.println("QOS:");
    for (Segment segment : segments){

      double divitions = 10;
      double module_section = segment.distance() / divitions;
      double intersections = 0;

      double coverered_distance = 0;

      //System.out.println("SEGMENT");
      //segment.print();

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
        for (Rsu rsu : road_side_units) {
          if (rsu.belongsToCircle(aux_point)) {
            intersections++;
            break;
          }
        }
      }

      if (intersections > divitions * coverage_acceptance) {
        // Segment is covered above 80%
        intersected_segments.add(segment);
      }

      coverered_distance = intersections * module_section;
      // System.out.println(intersections);
      // System.out.println(segment.distance());
      qos += segment.importance * (coverered_distance);
    }
    // System.out.println("#######");
    return qos;
  }

  private static void loadSegments() {
    double ideal_qos = 0;

    try{
      // Load segments
      File file = new File("alternatives/instances/montevideo.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = null;
      String [] line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
          line_tokens = line.split(" ");

          double start_x = Double.parseDouble(line_tokens[1]);
          double start_y = Double.parseDouble(line_tokens[2]);
          double end_x = Double.parseDouble(line_tokens[3]);
          double end_y = Double.parseDouble(line_tokens[4]);
          int importance = Integer.parseInt(line_tokens[5]);

          Point start = new Point(start_x, start_y);
          Point end = new Point(end_x, end_y);
          Segment segment = new Segment(start, end, importance);
          segments.add(segment);

          ideal_qos += importance * Point.twoPointsDistance(start, end);
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

        rsu_types.add(new RsuType(Double.parseDouble(line_tokens[2]), Double.parseDouble(line_tokens[3])));
      }
      buffer.close();

    }catch(IOException e){
      System.out.println(e + " there was a problem reading the file");
    }
  }

  public static double max(double first, double... rest) {
    double ret = first;
    for (double val : rest) {
        ret = Math.max(ret, val);
    }
    return ret;
  }
}
