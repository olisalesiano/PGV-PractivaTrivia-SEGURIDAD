package net.salesianos.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionLoader {

    public static String[][] loadQuestionsFromJson() {
        List<String[]> questionsList = new ArrayList<>();

        try {
            InputStream is = QuestionLoader.class.getClassLoader().getResourceAsStream("questions.json");
            if (is == null) {
                System.out.println("No se encontró questions.json en resources");
                return new String[0][0];
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line.trim());
            }
            reader.close();

            String json = jsonContent.toString();
            Pattern questionPattern = Pattern.compile(
                    "\\{[^}]*\"pregunta\"\\s*:\\s*\"([^\"]*)\"[^}]*\"respuesta\"\\s*:\\s*\"([^\"]*)\"[^}]*\\}");
            Matcher matcher = questionPattern.matcher(json);

            while (matcher.find()) {
                String question = matcher.group(1);
                String answer = matcher.group(2);
                questionsList.add(new String[] { question, answer });
            }

            System.out.println("Cargadas " + questionsList.size() + " preguntas desde questions.json");

        } catch (Exception e) {
            System.out.println("Error al cargar questions.json: " + e.getMessage());
            return new String[0][0];
        }

        return questionsList.toArray(new String[0][]);
    }
}