package Assignment.Recipe_Generator.controller;

import Assignment.Recipe_Generator.dto.IngredientRecognition;
import Assignment.Recipe_Generator.service.CustomOAuth2User;
import Assignment.Recipe_Generator.service.OpenRouterService;
import Assignment.Recipe_Generator.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ingredients", description = "Ingredient recognition API")
public class IngredientController {

    private final OpenRouterService openRouterService;
    private final IngredientService ingredientService;

    @PostMapping("/recognize")
    @Operation(summary = "Recognize ingredients from uploaded image")
    public ResponseEntity<IngredientRecognition> recognizeIngredients(
            @Parameter(description = "Image file (PNG, JPG, WEBP)")
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomOAuth2User user) {
        
        try {
            // Validate file type
            String contentType = image.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate file size (max 10MB)
            if (image.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().build();
            }
            
            byte[] imageBytes = image.getBytes();
            String userId = user != null ? user.getUserId() : null;
            
            IngredientRecognition result = openRouterService.recognizeIngredients(imageBytes, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing image for ingredient recognition", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/master-list")
    @Operation(summary = "Get complete master ingredients list for frontend autocomplete")
    public ResponseEntity<List<String>> getMasterIngredients() {
        
        try {
            Set<String> masterIngredients = ingredientService.getMasterIngredientsList();
            List<String> ingredientsList = new ArrayList<>(masterIngredients);
            
            // Return all ingredients sorted alphabetically
            ingredientsList.sort(String::compareToIgnoreCase);
            
            return ResponseEntity.ok(ingredientsList);
            
        } catch (Exception e) {
            log.error("Error getting master ingredients list", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/supported-formats")
    @Operation(summary = "Get supported image formats")
    public ResponseEntity<Map<String, Object>> getSupportedFormats() {
        Map<String, Object> response = Map.of(
            "supportedFormats", new String[]{"image/png", "image/jpeg", "image/webp"},
            "maxSize", "10MB",
            "maxSizeBytes", 10 * 1024 * 1024
        );
        
        return ResponseEntity.ok(response);
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/png") || 
               contentType.equals("image/jpeg") || 
               contentType.equals("image/jpg") ||
               contentType.equals("image/webp");
    }
}



