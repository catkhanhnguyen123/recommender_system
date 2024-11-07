package com.hcmut.recommender.controller;

import com.hcmut.recommender.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/initialize")
    public String initializeModel() {
        recommendationService.initializeSVDPP();
        return "Model initialized and trained successfully!";
    }

    @GetMapping("/predict")
    public double predictRating(@RequestParam String userID, @RequestParam String livestreamID) {
        return recommendationService.predictRating(userID, livestreamID);
    }

    @GetMapping("/user/exist")
    public boolean checkUserExistence(@RequestParam String userID) {
        return recommendationService.userExists(userID);
    }

    @GetMapping("/users")
    public List<String> getAllUsers() {
        return recommendationService.getAllUsers();
    }

    @GetMapping("/recommend")
    public List<String> recommendLivestreams(@RequestParam String userID, @RequestParam int count) {
        return recommendationService.recommendLivestreams(userID, count);
    }
}
