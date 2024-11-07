package com.hcmut.recommender.model; 

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SVDPP {

    private Map<String, Map<String, Double>> ratingData = new HashMap<>();
    private Map<String, Double> BU = new HashMap<>(); // User bias term
    private Map<String, Double> BI = new HashMap<>(); // Item bias term
    private Map<String, Double[]> Y = new HashMap<>(); // Factors for items user has rated
    private double sumMean = 0.0; // Overall rating mean
    private int F;
    private double alpha;
    private double lambda;
    private int max_iter;
    private Map<String, Double[]> U = new HashMap<>();
    private Map<String, Double[]> I = new HashMap<>();

    public SVDPP() {
        loadDataFromDatabase();
    }

    public static void main(String[] args) {
        SVDPP svdpp = new SVDPP();
        svdpp.initParam(30, 0.02, 0.01, 50); // Initialize parameters
        svdpp.train();

        System.out.println("Input userID...");
        Scanner in = new Scanner(System.in);
        while (true) {
            String userID = in.nextLine();
            System.out.println("Predicted rating for userID " + userID + ": ");
            in.close();
        }
    }

    private void loadDataFromDatabase() {
        String url = "jdbc:postgresql://100.85.204.98:5432/streamconnect";
        String user = "stream_admin";
        String password = "abc12345";
        String query = "SELECT user_id, livestream_id, rating FROM recommender_rating";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String userId = rs.getString("user_id");
                String livestreamId = rs.getString("livestream_id");
                double rating = rs.getDouble("rating");

                ratingData.computeIfAbsent(userId, k -> new HashMap<>()).put(livestreamId, rating);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initParam(int F, double alpha, double lambda, int max_iter) {
        this.F = F;
        this.alpha = alpha;
        this.lambda = lambda;
        this.max_iter = max_iter;
        int itemCount = 0;

        Double[] randomUValue;
        Double[] randomIValue;
        Double[] randomYValue;

        for (Map.Entry<String, Map<String, Double>> entry : ratingData.entrySet()) {
            String userID = entry.getKey();
            this.BU.put(userID, 0.0);
            itemCount += entry.getValue().size();
            randomUValue = new Double[F];
            for (int i = 0; i < F; i++) {
                randomUValue[i] = Math.random() / Math.sqrt(F);
            }
            U.put(userID, randomUValue);

            for (Map.Entry<String, Double> entryItem : entry.getValue().entrySet()) {
                this.sumMean += entryItem.getValue();
                String itemID = entryItem.getKey();
                this.BI.put(itemID, 0.0);
                if (I.containsKey(itemID))
                    continue;

                randomIValue = new Double[F];
                randomYValue = new Double[F];
                for (int i = 0; i < F; i++) {
                    randomIValue[i] = Math.random() / Math.sqrt(F);
                    randomYValue[i] = Math.random() / Math.sqrt(F);
                }
                I.put(itemID, randomIValue);
                Y.put(itemID, randomYValue);
            }
        }
        this.sumMean /= itemCount;
    }

    public void train() {
        for (int step = 0; step < this.max_iter; step++) {
            System.out.println("Iteration " + (step + 1) + "...");
            for (Map.Entry<String, Map<String, Double>> entry : this.ratingData.entrySet()) {
                double[] z_Item = new double[this.F];
                for (String item : entry.getValue().keySet()) {
                    Double[] Y_Item = this.Y.get(item);
                    for (int f = 0; f < this.F; f++) {
                        z_Item[f] += Y_Item[f];
                    }
                }
                double itemLength_Sqrt = 1.0 / Math.sqrt(1.0 * entry.getValue().size());
                double[] s = new double[this.F];

                String userID = entry.getKey();
                for (Map.Entry<String, Double> itemRatingEntry : entry.getValue().entrySet()) {
                    String itemID = itemRatingEntry.getKey();
                    double pui = this.predictRating(userID, itemID);
                    double err = itemRatingEntry.getValue() - pui;
                    double bu = this.BU.get(userID);
                    bu += this.alpha * (err - this.lambda * bu);
                    this.BU.put(userID, bu);
                    double bi = this.BI.get(itemID);
                    bi += this.alpha * (err - this.lambda * bi);
                    this.BI.put(itemID, bi);
                    Double[] userValue = this.U.get(userID);
                    Double[] itemValue = this.I.get(itemID);
                    for (int i = 0; i < this.F; i++) {
                        s[i] += itemValue[i] * err;
                        userValue[i] += this.alpha * (err * itemValue[i] - this.lambda * userValue[i]);
                        itemValue[i] += this.alpha
                                * (err * (userValue[i] + z_Item[i] * itemLength_Sqrt) - this.lambda * itemValue[i]);
                    }
                }
                for (String item : entry.getValue().keySet()) {
                    Double[] Y_Item = this.Y.get(item);
                    for (int f = 0; f < this.F; f++) {
                        Y_Item[f] += this.alpha * (s[f] * itemLength_Sqrt - this.lambda * Y_Item[f]);
                    }
                }
            }
            this.alpha *= 0.9;
        }
    }

    public double predictRating(String userID, String itemID) {
        Map<String, Double> ratingItem = this.ratingData.get(userID);
        if (ratingItem == null) {
            throw new IllegalArgumentException("User ID not found: " + userID);
        }

        double[] z_Item = new double[this.F];
        for (String item : ratingItem.keySet()) {
            Double[] Y_Item = this.Y.get(item);
            if (Y_Item != null) {
                for (int f = 0; f < this.F; f++) {
                    z_Item[f] += Y_Item[f];
                }
            }
        }
        double p = 0.0;
        Double[] userValue = this.U.get(userID);
        Double[] itemValue = this.I.get(itemID);
        if (userValue == null || itemValue == null) {
            throw new IllegalArgumentException("User or item not found for prediction: " + userID + ", " + itemID);
        }
        for (int i = 0; i < this.F; i++) {
            p += (userValue[i] + z_Item[i] / Math.sqrt(1.0 * ratingItem.size())) * itemValue[i];
        }
        return p + this.BU.get(userID) + this.BI.get(itemID) + this.sumMean;
    }

    // public List<String> recommendLivestreams(String userID, int count) {
    //     List<String> recommendedLivestreams = new ArrayList<>();

    //     // Logic để gợi ý livestream dựa trên rating đã dự đoán
    //     Map<String, Double> predictedRatings = new HashMap<>();

    //     for (String itemID : this.I.keySet()) { // Giả định bạn có danh sách livestreams trong I
    //         double predictedRating = predictRating(userID, itemID);
    //         predictedRatings.put(itemID, predictedRating);
    //     }

    //     // Sắp xếp và lấy top N livestreams
    //     predictedRatings.entrySet().stream()
    //             .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
    //             .limit(count)
    //             .forEach(entry -> recommendedLivestreams.add(entry.getKey()));

    //     return recommendedLivestreams;
    // }


    public List<String> recommendLivestreams(String userID, int count) {
        Map<String, Double> predictedRatings = new HashMap<>();
    
        // Dự đoán rating cho mỗi livestream
        for (String itemID : this.I.keySet()) {
            double predictedRating = predictRating(userID, itemID);
            predictedRatings.put(itemID, predictedRating);
        }
    
        // Sắp xếp theo thứ tự giảm dần và giới hạn theo số lượng yêu cầu
        return predictedRatings.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .toList(); 
    }

    public void clearRatingData() {
        ratingData.clear();
    }
    
    public void addRatingData(String userId, String itemId, double rating) {
        ratingData.computeIfAbsent(userId, k -> new HashMap<>()).put(itemId, rating);
    }
}
