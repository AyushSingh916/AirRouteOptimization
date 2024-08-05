import java.io.*;
import java.util.*;

class Airport {
    String iataCode;
    double lat;
    double lon;

    public Airport(String iataCode, double lat, double lon) {
        this.iataCode = iataCode;
        this.lat = lat;
        this.lon = lon;
    }
}

class Route {
    String flightNo;
    String source;
    String destination;
    int frequency;
    double distance;

    public Route(String flightNo, String source, String destination, int frequency) {
        this.flightNo = flightNo;
        this.source = source;
        this.destination = destination;
        this.frequency = frequency;
        this.distance = 0.0;
    }
}

public class FlightDistanceCalculator {
    private static final double EARTH_RADIUS = 6371; // in kilometers

    public static void main(String[] args) {
        Map<String, Airport> airports = loadAirports("in-airports.csv");
        List<Route> routes = loadRoutes("in-air-routes.csv");

        for (Route route : routes) {
            Airport src = airports.get(route.source);
            Airport dest = airports.get(route.destination);
            if (src != null && dest != null) {
                route.distance = calculateDistance(src.lat, src.lon, dest.lat, dest.lon);
            }
        }

        updateRoutesFile(routes, "updated-routes.csv");
    }

    private static Map<String, Airport> loadAirports(String filename) {
        Map<String, Airport> airports = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 17) {
                    String iataCode = parts[16].replaceAll("\"", "");
                    if (!iataCode.isEmpty()) {
                        try {
                            double lat = Double.parseDouble(parts[4].replaceAll("\"", ""));
                            double lon = Double.parseDouble(parts[5].replaceAll("\"", ""));
                            airports.put(iataCode, new Airport(iataCode, lat, lon));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing coordinates for airport: " + iataCode);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return airports;
    }

    private static List<Route> loadRoutes(String filename) {
        List<Route> routes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String flightNo = parts[0];
                    String destination = parts[1];
                    String source = parts[2];
                    try {
                        int frequency = Integer.parseInt(parts[3]);
                        routes.add(new Route(flightNo, source, destination, frequency));
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing frequency for flight: " + flightNo);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return routes;
    }

    private static void updateRoutesFile(List<Route> routes, String outputFilename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilename))) {
            bw.write("fltno,dest,source,freq,route,distance\n");
            for (Route route : routes) {
                bw.write(String.format("%s,%s,%s,%d,%s,%.2f\n",
                        route.flightNo, route.destination, route.source, route.frequency,
                        route.source + "-" + route.destination, route.distance));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}
