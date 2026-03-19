package org.example;

import java.util.ArrayList;
import java.util.List;

public class PredictionResult {
    private final int predictionId;
    private final List<String> guesses = new ArrayList<>();
    private final double confidence;
    private final String rawJson;
    private int currentIndex = 0;

    public PredictionResult(int predictionId, String first, double confidence, String second, String third, String rawJson) {
        this.predictionId = predictionId;

        if (first != null && !first.isBlank()) guesses.add(first);
        if (second != null && !second.isBlank() && !guesses.contains(second)) guesses.add(second);
        if (third != null && !third.isBlank() && !guesses.contains(third)) guesses.add(third);

        this.confidence = confidence;
        this.rawJson = rawJson;
    }

    public int getPredictionId() {
        return predictionId;
    }

    public String getCurrentGuess() {
        if (guesses.isEmpty() || currentIndex >= guesses.size()) {
            return "unknown";
        }
        return guesses.get(currentIndex);
    }

    public boolean tryNextGuess() {
        if (currentIndex + 1 < guesses.size()) {
            currentIndex++;
            return true;
        }
        return false;
    }

    public double getConfidence() {
        return confidence;
    }
    public String getRawJson() {
        return rawJson;
    }
    public List<String> getGuesses() {
        return guesses;
    }
}