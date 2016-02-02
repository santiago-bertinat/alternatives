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

public class Knapsack {
  public static ArrayList<Segment> segments = new ArrayList<Segment>();
  public static ArrayList<RsuType> rsu_types = new ArrayList<RsuType>();
  public static double qos = 0;
  public static int cost_interval_value = 200;
  public static int cost_intervals = 200;
  public static int coordinates_amount = 124;
  public static ArrayList<Rsu>[][] rsus_grid;

  public static void main(String[] args) {
    loadSegments();
    loadRsuTypes();

    double[][] qos_grid = new double[segments.size() + 1][cost_intervals + 1];
    rsus_grid = new ArrayList[segments.size() + 1][cost_intervals + 1];

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

          Rsu rsu_0 = new Rsu(rsu_center, rsu_types.get(0), i - 1);
          Rsu rsu_1 = new Rsu(rsu_center, rsu_types.get(1), i - 1);
          Rsu rsu_2 = new Rsu(rsu_center, rsu_types.get(2), i - 1);

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

    saveResults(qos_grid, rsus_grid);
    saveResultsForAE(rsus_grid);

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

  private static void saveResults(double[][] qos_grid,  ArrayList<Rsu>[][] rsus_grid) {
    try {
      File file = new File("alternatives/effective_range/results/knapsack_max_result.txt");

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter file_writer = new FileWriter(file.getAbsoluteFile());
      BufferedWriter buffer = new BufferedWriter(file_writer);

      for (Rsu rsu : rsus_grid[segments.size()][cost_intervals]) {
        buffer.write(String.valueOf(rsu.center.x) + ',');
        buffer.write(String.valueOf(rsu.center.y) + ',');
        buffer.write(String.valueOf(rsu.radius) + ',');
        buffer.write(String.valueOf(rsu.rsu_type.cost) + '\n');
      }

      buffer.close();

      file = new File("alternatives/effective_range/results/knapsack_results.txt");

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      file_writer = new FileWriter(file.getAbsoluteFile());
      buffer = new BufferedWriter(file_writer);

      for (int i = 0; i <= cost_intervals; i++) {
        buffer.write(String.valueOf(qos_grid[segments.size()][i]) + " , ");
        double cost = 0;
        for (Rsu rsu : rsus_grid[segments.size()][i]) {
          if (rsu.rsu_type != null)
            cost += rsu.rsu_type.cost;
        }
        buffer.write(String.valueOf(cost) + '\n');
      }

      buffer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void saveResultsForAE(ArrayList<Rsu>[][] rsus_grid) {
    try {
      File file = new File("alternatives/effective_range/results/knapsack_AE_format.txt");

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter file_writer = new FileWriter(file.getAbsoluteFile());
      BufferedWriter buffer = new BufferedWriter(file_writer);


      for (int i = 0; i <= cost_intervals; i++) {
        Rsu[] sorted_rsus = new Rsu[segments.size()];
        for (Rsu rsu : rsus_grid[segments.size()][i]) {
          sorted_rsus[rsu.segment_id] = rsu;
        }

        for (int j = 0; j < segments.size(); j++) {
          Segment segment = segments.get(j);
          Rsu rsu = sorted_rsus[j];
          if (rsu != null) {
            RsuType rsu_type = rsu.rsu_type;
            int index_of_type = rsu_types.indexOf(rsu_type) + 1;
            double position = Point.twoPointsDistance(segment.start, rsu.center) / segment.distance;
            buffer.write(String.valueOf((float)(index_of_type + position)) + ",");
          } else {
            buffer.write("0,");
          }
        }
        buffer.write('\n');
      }

      buffer.close();

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

  private static double getQos(ArrayList<Rsu> previous_road_side_units, Rsu new_road_side_unit) {
    qos = 0;

    ArrayList<Rsu> road_side_units = new ArrayList<Rsu>();
    if (previous_road_side_units != null) {
      road_side_units.addAll(previous_road_side_units);
    }
    if (new_road_side_unit != null) {
      road_side_units.add(new_road_side_unit);
    }

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
        for (Rsu rsu : road_side_units) {
          if (rsu.belongsToCircle(aux_point)) {
            intersections++;
            break;
          }
        }
      }

      qos += segment.vehicles_amount * (intersections / divitions);

    }
    return qos;
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

  public static double max(double first, double... rest) {
    double ret = first;
    for (double val : rest) {
        ret = Math.max(ret, val);
    }
    return ret;
  }
}
