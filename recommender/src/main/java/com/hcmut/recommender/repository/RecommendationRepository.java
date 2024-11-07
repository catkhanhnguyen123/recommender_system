package com.hcmut.recommender.repository;

import org.springframework.stereotype.Repository;
import com.hcmut.recommender.model.SVDPP;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RecommendationRepository {

    public void loadDataFromDatabase(SVDPP svdpp) {
        // Code load dữ liệu từ DB
        String url = "jdbc:postgresql://100.85.204.98:5432/streamconnect";
        String user = "stream_admin";
        String password = "abc12345";
        String query = "SELECT user_id, livestream_id, rating FROM recommender_rating";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery()) {

            svdpp.clearRatingData();  // Xóa dữ liệu rating cũ trong model
            while (rs.next()) {
                String userId = rs.getString("user_id");
                String livestreamId = rs.getString("livestream_id");
                double rating = rs.getDouble("rating");
                svdpp.addRatingData(userId, livestreamId, rating);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean userExists(String userID) {
        String url = "jdbc:postgresql://100.85.204.98:5432/streamconnect";
        String user = "stream_admin";
        String password = "abc12345";
        String query = "SELECT COUNT(*) FROM recommender_rating WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Kiểm tra xem có bất kỳ bản ghi nào không
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getAllUsers() {
        List<String> userIds = new ArrayList<>();
        String url = "jdbc:postgresql://100.85.204.98:5432/streamconnect";
        String user = "stream_admin";
        String password = "abc12345";
        String query = "SELECT user_id FROM recommender_rating";
    
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
    
            while (rs.next()) {
                String userId = rs.getString("user_id");
                userIds.add(userId);
                System.out.println("Found user ID: " + userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userIds;
    }    
    
}
