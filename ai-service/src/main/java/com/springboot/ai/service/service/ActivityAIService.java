package com.springboot.ai.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ai.service.model.Activity;
import com.springboot.ai.service.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {

    private final GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);
        log.info("Response from AI: {}", aiResponse);

        return processAIResponse(activity, aiResponse);
    }

    private Recommendation processAIResponse(Activity activity, String aiResponse) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n","")
                    .replaceAll("\\n```","")
                    .trim();

//            log.info("Parsed Response Content: {}", jsonContent);

            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overall","Overall: ");
            addAnalysisSection(fullAnalysis, analysisNode, "pace","Pace: ");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate","Heart Rate: ");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned","Calories: ");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getActivityType())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdAt(LocalDateTime.now())
                    .build();


        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }

    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getActivityType())
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting a fitness professional"))
                .safety(Arrays.asList(
                        "Always warm up before exercise",
                        "Stay hydrated",
                        "Listen to your body"
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyGuidelines) {

        List<String> safetyList = new ArrayList<>();
        if (safetyGuidelines.isArray()) {
            safetyGuidelines.forEach(safetyNode -> safetyList.add(safetyNode.asText()));
        }
        return safetyList.isEmpty() ?
                Collections.singletonList("Follow general safety guidelines")
                : safetyList;

    }

    private List<String> extractSuggestions(JsonNode suggestions) {

        List<String> suggestionList = new ArrayList<>();
        if (suggestions.isArray()) {
            suggestions.forEach(suggestionNode -> {
                String workout = suggestionNode.path("workout").asText();
                String description = suggestionNode.path("description").asText();
                suggestionList.add(String.format("%s (%s)", description, workout));
            });
        }
        return suggestionList.isEmpty() ?
                Collections.singletonList("No specific suggestions provided")
                : suggestionList;
    }

    private List<String> extractImprovements(JsonNode improvements) {

        List<String> improvementList = new ArrayList<>();

        if (improvements.isArray()) {
            improvements.forEach(improvementNode -> {
                String area = improvementNode.path("area").asText();
                String detail = improvementNode.path("recommendation").asText();
                improvementList.add(String.format("%s: %s", area, detail));
            });
        }
        return improvementList.isEmpty() ?
                Collections.singletonList("No specific improvements provided")
                : improvementList;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {

        if (!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity activity) {
        return String.format(""" 
                  Analyze this fitness activity and provide detailed recommendations in the following format
                  {
                      "analysis" : {
                          "overall": "Overall analysis here",
                          "pace": "Pace analysis here",
                          "heartRate": "Heart rate analysis here",
                          "caloriesBurned": "Calories Burned here"
                      },
                      "improvements": [
                          {
                              "area": "Area name",
                              "recommendation": "Detailed Recommendation"
                          }
                      ],
                      "suggestions" : [
                          {
                              "workout": "Workout name",
                              "description": "Detailed workout description"
                          }
                      ],
                      "safety": [
                          "Safety point 1",
                          "Safety point 2"
                      ]
                  }
                
                  Analyze this activity:
                  Activity Type: %s
                  Duration: %d minutes
                  Calories Burned: %d
                  Additional Metrics: %s
                
                  Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines
                  Ensure the response follows the EXACT JSON format shown above.
                """,
                        activity.getActivityType(),
                        activity.getDuration(),
                        activity.getCaloriesBurned(),
                        activity.getAdditionalMetrics()
        );
    }
}
