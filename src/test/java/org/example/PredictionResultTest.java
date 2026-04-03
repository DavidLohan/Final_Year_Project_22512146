package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PredictionResultTest {

    @Test
    void constructorStoresBasicValuesCorrectly() {
        PredictionResult result = new PredictionResult(
                5,
                "cat",
                0.92,
                "dog",
                "bird",
                "{json}"
        );

        assertEquals(5, result.getPredictionId());
        assertEquals(0.92, result.getConfidence(), 0.0001);
        assertEquals("{json}", result.getRawJson());
    }

    @Test
    void guessesAreStoredInCorrectOrder() {
        PredictionResult result = new PredictionResult(
                1,
                "apple",
                0.8,
                "banana",
                "pear",
                "{}"
        );

        List<String> guesses = result.getGuesses();

        assertEquals(3, guesses.size());
        assertEquals("apple", guesses.get(0));
        assertEquals("banana", guesses.get(1));
        assertEquals("pear", guesses.get(2));
    }

    @Test
    void duplicateGuessesAreIgnored() {
        PredictionResult result = new PredictionResult(
                1,
                "cat",
                0.9,
                "cat",   // duplicate
                "cat",   // duplicate
                "{}"
        );

        List<String> guesses = result.getGuesses();

        assertEquals(1, guesses.size());
        assertEquals("cat", guesses.get(0));
    }

    @Test
    void blankOrNullGuessesAreIgnored() {
        PredictionResult result = new PredictionResult(
                1,
                "dog",
                0.9,
                "",       // blank
                null,     // null
                "{}"
        );

        List<String> guesses = result.getGuesses();

        assertEquals(1, guesses.size());
        assertEquals("dog", guesses.get(0));
    }

    @Test
    void getCurrentGuessReturnsFirstInitially() {
        PredictionResult result = new PredictionResult(
                1,
                "car",
                0.7,
                "bus",
                "bike",
                "{}"
        );

        assertEquals("car", result.getCurrentGuess());
    }

    @Test
    void tryNextGuessMovesToNextGuess() {
        PredictionResult result = new PredictionResult(
                1,
                "car",
                0.7,
                "bus",
                "bike",
                "{}"
        );

        boolean moved = result.tryNextGuess();

        assertTrue(moved);
        assertEquals("bus", result.getCurrentGuess());
    }

    @Test
    void tryNextGuessReturnsFalseWhenNoMoreGuesses() {
        PredictionResult result = new PredictionResult(
                1,
                "car",
                0.7,
                null,
                null,
                "{}"
        );

        boolean moved = result.tryNextGuess();

        assertFalse(moved);
        assertEquals("car", result.getCurrentGuess());
    }

    @Test
    void getCurrentGuessReturnsUnknownWhenNoGuesses() {
        PredictionResult result = new PredictionResult(
                1,
                null,
                0.7,
                null,
                null,
                "{}"
        );

        assertEquals("unknown", result.getCurrentGuess());
    }

    @Test
    void tryNextGuessStopsAtLastGuess() {
        PredictionResult result = new PredictionResult(
                1,
                "a",
                0.7,
                "b",
                "c",
                "{}"
        );

        assertTrue(result.tryNextGuess()); // b
        assertTrue(result.tryNextGuess()); // c
        assertFalse(result.tryNextGuess()); // no more

        assertEquals("c", result.getCurrentGuess());
    }
}