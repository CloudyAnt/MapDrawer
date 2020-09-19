package cn.itscloudy.geo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class MapDrawer {
    JFrame frame = new JFrame();
    IPanel canvas = new IPanel();

    public MapDrawer() {
        frame.setBounds(200, 30, 1020, 1020);
        frame.setLayout(new GridLayout(1, 1));
        frame.add(canvas);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void show() {
        frame.setVisible(true);
    }

    public void print(JSONArray coordinates) {
        float westernmostLat = Integer.MAX_VALUE;
        float easternmostLat = Integer.MIN_VALUE;
        float northernmostLong = Integer.MIN_VALUE;
        float southernmostLong = Integer.MAX_VALUE;

        List<Point> rawPoints = new ArrayList<>();

        for (Object o : coordinates) {
            List<BigDecimal> point = null;
            if (o instanceof List) {
                try {
                    point = (List<BigDecimal>) o;
                } catch (Exception e) {
                    continue;
                }
            }

            if (point == null || point.size() != 2) {
                continue;
            }

            float x=  point.get(0).floatValue();
            float y=  point.get(1).floatValue();

            if (x < westernmostLat) {
                westernmostLat = x;
            }
            if (x > easternmostLat) {
                easternmostLat = x;
            }
            if (y > northernmostLong) {
                northernmostLong = y;
            }
            if (y < southernmostLong) {
                southernmostLong = y;
            }

            rawPoints.add(new Point(x, y));
        }

        float longitudeLong = northernmostLong - southernmostLong;
        float latitudeLong = easternmostLat - westernmostLat;
        float rate;
        if (longitudeLong > latitudeLong) {
            rate = longitudeLong / 1000;
        } else {
            rate = latitudeLong / 1000;
        }

        drawMap(westernmostLat, southernmostLong, rawPoints, longitudeLong, rate);
    }

    private void drawMap(float westernmostLat, float southernmostLong, List<Point> rawPoints, float longitudeLong, float rate) {
        int intLongitudeLong = (int)(longitudeLong / rate);

        Polygon polygon = new Polygon();
        for (Point point : rawPoints) {
            float x = point.x - westernmostLat;
            float y = point.y - southernmostLong;

            polygon.addPoint((int)(x / rate), intLongitudeLong - (int)(y/ rate));
        }
        canvas.print(graphics -> graphics.drawPolygon(polygon));
    }

    private void close() {
        frame.dispose();
        System.exit(0);
    }

    private static void repaint(MapDrawer mapDrawer, String distractCode) throws IOException {
        JSONArray coordinates = dataVCoordinatesOf(distractCode);
        if (coordinates != null) {
            mapDrawer.print(coordinates);
        }
        System.out.printf("Map %s printed\n", distractCode);
    }

    private static JSONArray dataVCoordinatesOf(String distractCode) throws IOException {
        URL url = new URL("https://geo.datav.aliyun.com/areas_v2/bound/" + distractCode + ".json");
        System.out.println("Fetching: " + url.toString());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            return null;
        } catch (Exception e) {
            System.out.println("Unknown error: " + e.getMessage());
            return null;
        }
        System.out.println("Printing map: " + distractCode);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        JSONObject info = JSONObject.parseObject(content.toString());
        return info.getJSONArray("features").getJSONObject(0).getJSONObject("geometry")
                .getJSONArray("coordinates").getJSONArray(0).getJSONArray(0);
    }

    private static class IPanel extends JPanel {
        Consumer<Graphics2D> consumer;

        public void print(Consumer<Graphics2D> consumer) {
            this.consumer = consumer;
            this.repaint();
        }

        @Override
        public void paint(Graphics graphics) {
            graphics.setColor(Color.RED);
            super.paint(graphics);
            if (consumer != null) {
                consumer.accept((Graphics2D)graphics);
            }
        }
    }

    private static class Point {
        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws IOException {
        MapDrawer mapDrawer = new MapDrawer();
        mapDrawer.show();

        // wait for json
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.equals("exit")) {
                System.out.println("Bye");
                break;
            } else {
                repaint(mapDrawer, line);
            }
        }
        mapDrawer.close();
    }
}
