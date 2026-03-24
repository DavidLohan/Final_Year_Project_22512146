package org.example;

public class TextToSpeechUtil {

    public static void speak(String text) {
        try {
            String safeText = text.replace("'", "''");

            String command =
                    "powershell -Command \"Add-Type -AssemblyName System.Speech; " +
                            "$voice = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$voice.Speak('" + safeText + "');\"";

            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}