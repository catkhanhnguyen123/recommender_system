package com.hcmut.recommender.service;

import com.hcmut.recommender.model.SVDPP;
import com.hcmut.recommender.repository.RecommendationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class RecommendationService {

    private SVDPP svdpp;

    @Autowired
    private RecommendationRepository recommendationRepository;

    
    public void initializeSVDPP() {
        try {
            String pythonInterpreter = "C:\\Users\\HP\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";
            String scriptPath = "C:\\Users\\HP\\Downloads\\recmodel\\recommender\\src\\main\\java\\com\\hcmut\\recommender\\service\\prepare_data.py"; 

            // Gọi file Python sử dụng ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(pythonInterpreter, scriptPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Đọc kết quả từ đầu ra của tiến trình
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Chờ kết quả và kiểm tra mã thoát của script
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Python script failed with exit code: " + exitCode);
            } else {
                System.out.println("Python script executed successfully.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Sau khi gọi Python script, khởi tạo và huấn luyện mô hình SVD++
        this.svdpp = new SVDPP();
        reloadDataAndTrainModel();
        recommendationRepository.loadDataFromDatabase(svdpp);
        svdpp.initParam(30, 0.02, 0.01, 50);
        svdpp.train();
    }

    @Scheduled(fixedRate = 120000) 
    public void reloadDataAndTrainModel() {
        recommendationRepository.loadDataFromDatabase(svdpp);
        svdpp.initParam(30, 0.02, 0.01, 50);
        svdpp.train();
        System.out.println("Data reloaded and model retrained.");
    }

    public double predictRating(String userID, String livestreamID) {
        if (svdpp == null) {
            throw new IllegalStateException("Model has not been initialized. Please initialize the model first.");
        }
        return svdpp.predictRating(userID, livestreamID);
    }

    public boolean userExists(String userID) {
        return recommendationRepository.userExists(userID);
    }

    public List<String> getAllUsers() {
        return recommendationRepository.getAllUsers();
    }

    public List<String> recommendLivestreams(String userID, int count) {
        if (svdpp == null) {
            throw new IllegalStateException("Model has not been initialized. Please initialize the model first.");
        }
        return svdpp.recommendLivestreams(userID, count);
    }
}
