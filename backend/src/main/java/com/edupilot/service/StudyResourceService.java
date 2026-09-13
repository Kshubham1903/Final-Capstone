package com.edupilot.service;

import com.edupilot.dto.StudyResourceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StudyResourceService {

    private static final int MIN_RELEVANCE_SCORE = 50;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "in", "on", "of", "for", "and", "or", "to", "with", "by",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "at", "from", "as", "that", "this", "these", "those"
    ));

    private static final Set<String> GENERIC_WORDS = new HashSet<>(Arrays.asList(
            "environment", "system", "systems", "properties", "property", "data", "method", "methods",
            "learning", "network", "networks", "model", "models", "application", "applications",
            "technology", "process", "analysis", "information", "structure", "structures", "base", "bases"
    ));

    // Thread-safe in-memory cache
    private final Map<String, StudyResourceDTO> cache = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudyResourceDTO getStudyResources(String subject, String concept) {
        if (concept == null || concept.trim().isEmpty()) {
            throw new IllegalArgumentException("Concept parameter is required and cannot be blank");
        }

        String cleanSubject = sanitizeInput(subject);
        String cleanConcept = cleanConceptName(sanitizeInput(concept));

        System.out.println("[StudyResourceService] Processing request -> originalSubject: '" + subject 
                + "', originalConcept: '" + concept 
                + "', cleanSubject: '" + cleanSubject 
                + "', cleanConcept: '" + cleanConcept + "'");

        String cacheKey = (cleanSubject.toLowerCase() + "::" + cleanConcept.toLowerCase());
        if (cache.containsKey(cacheKey)) {
            System.out.println("[StudyResourceService] Returning cached result for key: " + cacheKey);
            return cache.get(cacheKey);
        }

        List<StudyResourceDTO.ResourceItem> resources = discoverDynamicResources(cleanSubject, cleanConcept, concept.trim());

        StudyResourceDTO result = new StudyResourceDTO(cleanSubject, concept.trim(), resources);
        cache.put(cacheKey, result);
        return result;
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.length() > 120) {
            trimmed = trimmed.substring(0, 120).trim();
        }
        return trimmed;
    }

    private String cleanConceptName(String rawConcept) {
        if (rawConcept == null) return "";
        String cleaned = rawConcept.replaceAll("\\s*\\([^)]*\\)", "").trim();
        return cleaned.isEmpty() ? rawConcept.trim() : cleaned;
    }

    /**
     * Detects course codes / identifiers (e.g., AI601, CS301, CSE202, ECE301, IT202, ME301, EE201, DBMS101, AI-601, CS 999)
     */
    private boolean isCourseCode(String subject) {
        if (subject == null || subject.trim().isEmpty()) return true;
        String trimmed = subject.trim();
        if (trimmed.matches("(?i)^[A-Z]{2,5}[\\s-_]?\\d{2,4}[A-Z]?$")) {
            return true;
        }
        if (trimmed.length() <= 6 && trimmed.matches(".*\\d.*")) {
            return true;
        }
        return false;
    }

    /**
     * Infers useful semantic context when subject is a course code (e.g. AI601 -> "Artificial Intelligence")
     */
    private String resolveSemanticContext(String cleanConcept, String cleanSubject) {
        if (!cleanSubject.isEmpty() && !isCourseCode(cleanSubject)) {
            return cleanSubject;
        }

        String conceptLower = cleanConcept.toLowerCase();
        String subjectLower = cleanSubject.toLowerCase();

        if (conceptLower.contains("environment") || conceptLower.contains("agent") || subjectLower.startsWith("ai")) {
            return "Artificial Intelligence";
        }
        if (conceptLower.contains("locking") || conceptLower.contains("transaction") || conceptLower.contains("sql") || conceptLower.contains("database") || subjectLower.contains("db")) {
            return "Database System";
        }
        if (conceptLower.contains("scheduling") || conceptLower.contains("deadlock") || conceptLower.contains("process") || conceptLower.contains("memory") || subjectLower.contains("os")) {
            return "Operating Systems";
        }
        if (conceptLower.contains("tcp") || conceptLower.contains("ip") || conceptLower.contains("congestion") || conceptLower.contains("network")) {
            return "Computer Networks";
        }
        if (conceptLower.contains("tree") || conceptLower.contains("graph") || conceptLower.contains("heap") || conceptLower.contains("sort")) {
            return "Data Structures";
        }

        return "Computer Science";
    }

    /**
     * Determines whether the requested topic/subject is technical/programming-oriented
     */
    private boolean isProgrammingOrTechnicalConcept(String concept, String semanticContext) {
        String combined = (concept + " " + semanticContext).toLowerCase();

        if (combined.contains("biology") || combined.contains("chemistry") || combined.contains("physics")
                || combined.contains("history") || combined.contains("literature") || combined.contains("economics")
                || combined.contains("medicine") || combined.contains("biotechnology")) {
            return false;
        }

        return true;
    }

    private List<StudyResourceDTO.ResourceItem> discoverDynamicResources(String subject, String cleanConcept, String rawConcept) {
        Set<String> seenUrls = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();
        List<StudyResourceDTO.ResourceItem> items = new ArrayList<>();

        String semanticContext = resolveSemanticContext(cleanConcept, subject);
        boolean isTechConcept = isProgrammingOrTechnicalConcept(cleanConcept, semanticContext);

        System.out.println("[StudyResourceService] Resolved semantic context: '" + semanticContext + "' for raw subject: '" + subject + "'");

        // =========================================================================
        // PART 1: PRIMARY ENGINEERING SEARCH PLATFORMS
        // 1. GeeksforGeeks Search
        // 2. W3Schools Search (if technical)
        // 3. YouTube Tutorial Search
        // 4. Stack Overflow Search (if technical)
        // =========================================================================

        // 1. GeeksforGeeks Direct Article Resolution
        fetchGfgDirectResource(cleanConcept, semanticContext, rawConcept, items, seenUrls);

        // 2. W3Schools Direct Tutorial Resolution
        if (isTechConcept) {
            fetchW3SchoolsDirectResource(cleanConcept, semanticContext, rawConcept, items, seenUrls);
        }

        // 3. YouTube Direct Video Tutorial Resolution
        fetchYoutubeDirectResource(cleanConcept, semanticContext, rawConcept, items, seenUrls);

        // 4. Stack Overflow Direct Question Resolution
        if (isTechConcept) {
            fetchStackOverflowDirectResource(cleanConcept, rawConcept, items, seenUrls);
        }

        // =========================================================================
        // PART 2: SECONDARY REFERENCE & ACADEMIC DIRECT REST APIs
        // Strictly filtered for semantic relevance against the target concept
        // =========================================================================

        // 5. Wikipedia REST API
        try {
            String wikiQuery = buildWikipediaQuery(cleanConcept, semanticContext);
            fetchWikipediaResources(wikiQuery, cleanConcept, semanticContext, rawConcept, items, seenUrls, seenTitles);
        } catch (Throwable t) {
            System.err.println("[StudyResourceService] Wikipedia provider error: " + t.getMessage());
        }

        // 6. OpenAlex Academic Literature API
        try {
            String alexQuery = buildOpenAlexQuery(cleanConcept, semanticContext);
            fetchOpenAlexResources(alexQuery, cleanConcept, semanticContext, rawConcept, items, seenUrls, seenTitles);
        } catch (Throwable t) {
            System.err.println("[StudyResourceService] OpenAlex provider error: " + t.getMessage());
        }

        // 7. arXiv E-Print Repository API
        try {
            String arxivQuery = buildArxivQuery(cleanConcept, semanticContext);
            fetchArXivResources(arxivQuery, cleanConcept, semanticContext, rawConcept, items, seenUrls, seenTitles);
        } catch (Throwable t) {
            System.err.println("[StudyResourceService] arXiv provider error: " + t.getMessage());
        }

        return items;
    }

    private String buildWikipediaQuery(String concept, String context) {
        return concept + " " + context;
    }

    private String buildOpenAlexQuery(String concept, String context) {
        return concept + " " + context;
    }

    private String buildArxivQuery(String concept, String context) {
        return concept + " " + context;
    }

    private String verifyDirectUrl(String urlStr) {
        if (urlStr == null || urlStr.isBlank()) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(Duration.ofMillis(1000))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                String finalUrl = response.uri().toString();
                String lower = finalUrl.toLowerCase();
                if (!lower.contains("/search") && !lower.contains("/404")
                        && !lower.endsWith("w3schools.com/") && !lower.endsWith("w3schools.com")
                        && !lower.endsWith("geeksforgeeks.org/") && !lower.endsWith("geeksforgeeks.org")
                        && !lower.contains("geeksforgeeks.org/quizzes") && !lower.contains("geeksforgeeks.org/search")) {
                    return finalUrl;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void fetchGfgDirectResource(String cleanConcept, String context, String rawConcept, List<StudyResourceDTO.ResourceItem> items, Set<String> seenUrls) {
        String clean = cleanConcept.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim().toLowerCase();
        String[] words = clean.split(" ");
        String slugHyphen = String.join("-", words);

        List<String> candidates = new ArrayList<>();
        if (clean.contains("array")) {
            candidates.add("https://www.geeksforgeeks.org/dsa/array-data-structure-guide/");
            candidates.add("https://www.geeksforgeeks.org/dsa/array-data-structure/");
            candidates.add("https://www.geeksforgeeks.org/array-data-structure/");
        }
        if (clean.contains("linked") || clean.contains("list")) {
            candidates.add("https://www.geeksforgeeks.org/dsa/linked-list-data-structure/");
            candidates.add("https://www.geeksforgeeks.org/linked-list-data-structure/");
        }
        if (clean.contains("tree") || clean.contains("bst")) {
            candidates.add("https://www.geeksforgeeks.org/dsa/binary-search-tree-data-structure/");
            candidates.add("https://www.geeksforgeeks.org/binary-search-tree-data-structure/");
        }
        if (clean.contains("stack")) {
            candidates.add("https://www.geeksforgeeks.org/dsa/stack-data-structure/");
            candidates.add("https://www.geeksforgeeks.org/stack-data-structure/");
        }
        if (clean.contains("queue")) {
            candidates.add("https://www.geeksforgeeks.org/dsa/queue-data-structure/");
            candidates.add("https://www.geeksforgeeks.org/queue-data-structure/");
        }
        if (clean.contains("normaliz") || clean.contains("dbms") || clean.contains("database")) {
            candidates.add("https://www.geeksforgeeks.org/dbms/database-normalization/");
            candidates.add("https://www.geeksforgeeks.org/database-normalization-in-dbms/");
            candidates.add("https://www.geeksforgeeks.org/normal-forms-in-dbms/");
            candidates.add("https://www.geeksforgeeks.org/dbms/normal-forms-in-dbms/");
        }

        candidates.add("https://www.geeksforgeeks.org/dsa/" + slugHyphen + "-data-structure/");
        candidates.add("https://www.geeksforgeeks.org/dsa/" + slugHyphen + "/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "-data-structure/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "/");
        candidates.add("https://www.geeksforgeeks.org/dbms/" + slugHyphen + "/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "-in-dbms/");
        candidates.add("https://www.geeksforgeeks.org/operating-systems/" + slugHyphen + "/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "-in-operating-system/");
        candidates.add("https://www.geeksforgeeks.org/computer-network/" + slugHyphen + "/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "-in-computer-network/");
        candidates.add("https://www.geeksforgeeks.org/sql-" + slugHyphen + "/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "-sql/");
        candidates.add("https://www.geeksforgeeks.org/" + slugHyphen + "-tutorial/");
        candidates.add("https://www.geeksforgeeks.org/introduction-to-" + slugHyphen + "/");

        for (String cand : candidates) {
            String verified = verifyDirectUrl(cand);
            if (verified != null) {
                String normUrl = verified.toLowerCase();
                if (!seenUrls.contains(normUrl)) {
                    items.add(new StudyResourceDTO.ResourceItem(
                            cleanConcept + " — GeeksforGeeks",
                            verified,
                            "geeksforgeeks.org",
                            "Direct GeeksforGeeks article and code tutorial covering " + rawConcept + "."
                    ));
                    seenUrls.add(normUrl);
                    return;
                }
            }
        }
    }

    private void fetchW3SchoolsDirectResource(String cleanConcept, String context, String rawConcept, List<StudyResourceDTO.ResourceItem> items, Set<String> seenUrls) {
        String clean = cleanConcept.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim().toLowerCase();
        String[] words = clean.split(" ");
        String slugUnderscore = String.join("_", words);
        String slugPlain = String.join("", words);

        List<String> candidates = new ArrayList<>();
        if (clean.contains("array")) {
            candidates.add("https://www.w3schools.com/dsa/dsa_data_arrays.php");
            candidates.add("https://www.w3schools.com/js/js_arrays.asp");
            candidates.add("https://www.w3schools.com/java/java_arrays.asp");
            candidates.add("https://www.w3schools.com/python/python_arrays.asp");
        }
        if (clean.contains("linked") || clean.contains("list")) {
            candidates.add("https://www.w3schools.com/dsa/dsa_data_linkedlists.php");
        }
        if (clean.contains("tree") || clean.contains("bst")) {
            candidates.add("https://www.w3schools.com/dsa/dsa_data_binarysearchtrees.php");
            candidates.add("https://www.w3schools.com/dsa/dsa_data_trees.php");
        }
        if (clean.contains("stack")) {
            candidates.add("https://www.w3schools.com/dsa/dsa_data_stacks.php");
        }
        if (clean.contains("queue")) {
            candidates.add("https://www.w3schools.com/dsa/dsa_data_queues.php");
        }
        if (clean.contains("graph")) {
            candidates.add("https://www.w3schools.com/dsa/dsa_data_graphs.php");
        }

        candidates.add("https://www.w3schools.com/dsa/dsa_data_" + slugUnderscore + ".php");
        candidates.add("https://www.w3schools.com/dsa/dsa_data_" + slugPlain + ".php");
        candidates.add("https://www.w3schools.com/dsa/dsa_data_" + slugPlain + "s.php");
        candidates.add("https://www.w3schools.com/dsa/dsa_theory_" + slugPlain + ".php");
        candidates.add("https://www.w3schools.com/dsa/dsa_algo_" + slugPlain + ".php");
        candidates.add("https://www.w3schools.com/sql/sql_" + slugUnderscore + ".asp");
        candidates.add("https://www.w3schools.com/sql/sql_" + slugPlain + ".asp");
        candidates.add("https://www.w3schools.com/cpp/cpp_" + slugUnderscore + ".asp");
        candidates.add("https://www.w3schools.com/java/java_" + slugUnderscore + ".asp");
        candidates.add("https://www.w3schools.com/js/js_" + slugUnderscore + ".asp");
        candidates.add("https://www.w3schools.com/python/python_" + slugUnderscore + ".asp");
        candidates.add("https://www.w3schools.com/c/c_" + slugUnderscore + ".php");
        candidates.add("https://www.w3schools.com/programming/prog_" + slugUnderscore + ".php");

        for (String cand : candidates) {
            String verified = verifyDirectUrl(cand);
            if (verified != null) {
                String normUrl = verified.toLowerCase();
                if (!seenUrls.contains(normUrl)) {
                    items.add(new StudyResourceDTO.ResourceItem(
                            cleanConcept + " — W3Schools Tutorial",
                            verified,
                            "w3schools.com",
                            "Comprehensive W3Schools interactive tutorial explaining " + rawConcept + "."
                    ));
                    seenUrls.add(normUrl);
                    return;
                }
            }
        }
    }

    private void fetchYoutubeDirectResource(String cleanConcept, String context, String rawConcept, List<StudyResourceDTO.ResourceItem> items, Set<String> seenUrls) {
        try {
            String ytQuery = encode(cleanConcept + " " + context + " tutorial");
            String url = "https://www.youtube.com/results?search_query=" + ytQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                String html = response.body();
                Pattern p = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"");
                Matcher m = p.matcher(html);
                if (m.find()) {
                    String videoId = m.group(1);
                    String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
                    String normUrl = videoUrl.toLowerCase();
                    if (!seenUrls.contains(normUrl)) {
                        items.add(new StudyResourceDTO.ResourceItem(
                                cleanConcept + " — YouTube Video Tutorial",
                                videoUrl,
                                "youtube.com",
                                "Direct video lecture and step-by-step tutorial explaining " + rawConcept + "."
                        ));
                        seenUrls.add(normUrl);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[StudyResourceService] YouTube provider error: " + e.getMessage());
        }
    }

    private void fetchStackOverflowDirectResource(String cleanConcept, String rawConcept, List<StudyResourceDTO.ResourceItem> items, Set<String> seenUrls) {
        try {
            String soQuery = encode(cleanConcept);
            String url = "https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=relevance&q=" + soQuery + "&site=stackoverflow";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "EduPilot/1.0 (Educational Learning Assistant)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode itemsArray = root.path("items");
                if (itemsArray.isArray()) {
                    for (JsonNode item : itemsArray) {
                        String title = item.path("title").asText("");
                        String link = item.path("link").asText("");
                        if (!link.isEmpty() && link.contains("/questions/")) {
                            String normUrl = link.toLowerCase();
                            if (!seenUrls.contains(normUrl)) {
                                String cleanTitle = title.replaceAll("&quot;", "\"")
                                                         .replaceAll("&#39;", "'")
                                                         .replaceAll("&amp;", "&")
                                                         .replaceAll("&lt;", "<")
                                                         .replaceAll("&gt;", ">");
                                items.add(new StudyResourceDTO.ResourceItem(
                                        cleanTitle + " — Stack Overflow",
                                        link,
                                        "stackoverflow.com",
                                        "Community engineering discussion and accepted answers related to " + rawConcept + "."
                                ));
                                seenUrls.add(normUrl);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[StudyResourceService] Stack Overflow provider error: " + e.getMessage());
        }
    }

    /**
     * Provider 5: Wikipedia API with Contextual Search & Pure Generic Relevance Filtering
     */
    private void fetchWikipediaResources(
            String wikiQuery,
            String cleanConcept, 
            String context,
            String rawConcept, 
            List<StudyResourceDTO.ResourceItem> items, 
            Set<String> seenUrls, 
            Set<String> seenTitles
    ) {
        int candidateCount = 0;
        int acceptedCount = 0;
        int rejectedCount = 0;

        try {
            String encodedQuery = encode(wikiQuery);
            String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" 
                    + encodedQuery + "&format=json&utf8=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "EduPilot/1.0 (Educational Learning Assistant)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode searchResults = root.path("query").path("search");

                if (searchResults.isArray()) {
                    candidateCount = searchResults.size();
                    for (JsonNode node : searchResults) {
                        String pageTitle = node.path("title").asText("");
                        if (pageTitle.isEmpty()) continue;

                        String rawSnippet = node.path("snippet").asText("");
                        String cleanSnippet = rawSnippet.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();

                        int score = calculateRelevanceScore(cleanConcept, pageTitle, cleanSnippet, context);
                        boolean accept = score >= MIN_RELEVANCE_SCORE;

                        if (accept) {
                            String wikiUrl = "https://en.wikipedia.org/wiki/" 
                                    + URLEncoder.encode(pageTitle.replace(" ", "_"), StandardCharsets.UTF_8);

                            String normalizedUrl = wikiUrl.toLowerCase();
                            if (seenUrls.contains(normalizedUrl)) continue;

                            if (cleanSnippet.length() > 180) {
                                cleanSnippet = cleanSnippet.substring(0, 180) + "...";
                            }

                            String displayTitle = pageTitle + " — Wikipedia Reference";
                            String description = !cleanSnippet.isEmpty() 
                                    ? cleanSnippet 
                                    : "Wikipedia reference article related to " + rawConcept + ".";

                            items.add(new StudyResourceDTO.ResourceItem(
                                    displayTitle,
                                    wikiUrl,
                                    "en.wikipedia.org",
                                    description
                            ));

                            seenUrls.add(normalizedUrl);
                            seenTitles.add(pageTitle.toLowerCase());
                            acceptedCount++;
                            break; // Keep top relevant Wikipedia article
                        } else {
                            rejectedCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[StudyResourceService] Wikipedia fetch exception: " + e.getMessage());
        }

        System.out.println("[StudyResourceService] provider=Wikipedia concept=\"" + cleanConcept 
                + "\" candidates=" + candidateCount + " accepted=" + acceptedCount + " rejected=" + rejectedCount);
    }

    /**
     * Provider 6: OpenAlex API with Contextual Search & Pure Generic Relevance Filtering
     */
    private void fetchOpenAlexResources(
            String alexQuery, 
            String cleanConcept, 
            String context,
            String rawConcept, 
            List<StudyResourceDTO.ResourceItem> items, 
            Set<String> seenUrls, 
            Set<String> seenTitles
    ) {
        int candidateCount = 0;
        int acceptedCount = 0;
        int rejectedCount = 0;

        try {
            String encodedQuery = encode(alexQuery);
            String url = "https://api.openalex.org/works?search=" + encodedQuery + "&per-page=5";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "EduPilot/1.0 (Educational Learning Assistant)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.path("results");

                if (results.isArray()) {
                    candidateCount = results.size();
                    for (JsonNode node : results) {
                        String paperTitle = node.path("display_name").asText("");
                        if (paperTitle.isEmpty()) {
                            paperTitle = node.path("title").asText("");
                        }
                        if (paperTitle.isEmpty()) continue;

                        int score = calculateRelevanceScore(cleanConcept, paperTitle, "", context);
                        boolean accept = score >= MIN_RELEVANCE_SCORE;

                        if (accept) {
                            String normalizedTitle = paperTitle.toLowerCase();
                            if (seenTitles.contains(normalizedTitle)) continue;

                            String paperUrl = "";
                            if (node.has("doi") && !node.get("doi").isNull()) {
                                paperUrl = node.get("doi").asText();
                            } else if (node.has("id") && !node.get("id").isNull()) {
                                paperUrl = node.get("id").asText();
                            }

                            if (paperUrl.isEmpty()) continue;

                            String normalizedUrl = paperUrl.toLowerCase();
                            if (seenUrls.contains(normalizedUrl)) continue;

                            int year = node.path("publication_year").asInt(0);
                            String yearLabel = year > 0 ? " (" + year + ")" : "";
                            String displayTitle = paperTitle + yearLabel + " — Academic Paper";
                            String domain = extractDomain(paperUrl);

                            items.add(new StudyResourceDTO.ResourceItem(
                                    displayTitle,
                                    paperUrl,
                                    domain,
                                    "Relevant academic literature related to " + rawConcept + "."
                            ));

                            seenUrls.add(normalizedUrl);
                            seenTitles.add(normalizedTitle);
                            acceptedCount++;
                            break; // Keep top relevant OpenAlex paper
                        } else {
                            rejectedCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[StudyResourceService] OpenAlex fetch exception: " + e.getMessage());
        }

        System.out.println("[StudyResourceService] provider=OpenAlex concept=\"" + cleanConcept 
                + "\" candidates=" + candidateCount + " accepted=" + acceptedCount + " rejected=" + rejectedCount);
    }

    /**
     * Provider 7: arXiv API with Contextual Search & Pure Generic Relevance Filtering
     */
    private void fetchArXivResources(
            String arxivQuery, 
            String cleanConcept, 
            String context,
            String rawConcept, 
            List<StudyResourceDTO.ResourceItem> items, 
            Set<String> seenUrls, 
            Set<String> seenTitles
    ) {
        int candidateCount = 0;
        int acceptedCount = 0;
        int rejectedCount = 0;

        try {
            String encodedQuery = encode(arxivQuery);
            String url = "http://export.arxiv.org/api/query?search_query=all:" + encodedQuery + "&start=0&max_results=5";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "EduPilot/1.0 (Educational Learning Assistant)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                String xml = response.body();

                Pattern entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL);
                Matcher entryMatcher = entryPattern.matcher(xml);

                List<String> entries = new ArrayList<>();
                while (entryMatcher.find()) {
                    entries.add(entryMatcher.group(1));
                }

                candidateCount = entries.size();
                for (String entryBlock : entries) {
                    String title = extractXmlTag(entryBlock, "title").replaceAll("\\s+", " ").trim();
                    String id = extractXmlTag(entryBlock, "id").trim();
                    String summary = extractXmlTag(entryBlock, "summary").replaceAll("\\s+", " ").trim();

                    if (title.isEmpty() || id.isEmpty()) continue;

                    int score = calculateRelevanceScore(cleanConcept, title, summary, context);
                    boolean accept = score >= MIN_RELEVANCE_SCORE;

                    if (accept) {
                        String normalizedUrl = id.toLowerCase();
                        String normalizedTitle = title.toLowerCase();

                        if (seenUrls.contains(normalizedUrl) || seenTitles.contains(normalizedTitle)) continue;

                        if (summary.length() > 180) {
                            summary = summary.substring(0, 180) + "...";
                        }

                        String displayTitle = title + " — arXiv Research Paper";
                        String description = !summary.isEmpty()
                                ? summary
                                : "Relevant research papers related to " + rawConcept + ".";

                        items.add(new StudyResourceDTO.ResourceItem(
                                displayTitle,
                                id,
                                "arxiv.org",
                                description
                        ));

                        seenUrls.add(normalizedUrl);
                        seenTitles.add(normalizedTitle);
                        acceptedCount++;
                        break; // Keep top relevant arXiv paper
                    } else {
                        rejectedCount++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[StudyResourceService] arXiv fetch exception: " + e.getMessage());
        }

        System.out.println("[StudyResourceService] provider=arXiv concept=\"" + cleanConcept 
                + "\" candidates=" + candidateCount + " accepted=" + acceptedCount + " rejected=" + rejectedCount);
    }

    /**
     * Pure Generic Semantic Relevance Scoring Engine (Zero Domain/Entity Blacklisting)
     */
    private int calculateRelevanceScore(String concept, String title, String snippet, String context) {
        if (concept == null || title == null) return 0;

        String normConcept = normalizeText(concept);
        String normTitle = normalizeText(title);
        String normSnippet = snippet != null ? normalizeText(snippet) : "";
        String normContext = context != null ? normalizeText(context) : "";

        // Exact phrase match in title (+100)
        if (normTitle.equals(normConcept) || normTitle.contains(normConcept)) {
            return 100;
        }

        List<String> conceptTokens = extractStemmedKeywords(normConcept);
        if (conceptTokens.isEmpty()) {
            return normTitle.contains(normConcept) || normSnippet.contains(normConcept) ? 70 : 0;
        }

        int titleTokenMatches = 0;
        int snippetTokenMatches = 0;
        int genericCount = 0;

        for (String token : conceptTokens) {
            if (GENERIC_WORDS.contains(token)) {
                genericCount++;
            }
            if (normTitle.contains(token)) {
                titleTokenMatches++;
            }
            if (normSnippet.contains(token)) {
                snippetTokenMatches++;
            }
        }

        double titleCoverage = (double) titleTokenMatches / conceptTokens.size();
        double comboCoverage = (double) (titleTokenMatches + snippetTokenMatches) / conceptTokens.size();
        boolean isMostlyGeneric = genericCount >= (conceptTokens.size() / 2.0);

        int score = 0;

        // Exact phrase match in snippet (+70)
        if (normSnippet.contains(normConcept)) {
            score += 70;
        }

        if (titleCoverage >= 1.0) {
            score += 70;
        } else if (comboCoverage >= 1.0) {
            score += 40;
        } else {
            // Penalty for failing to cover all concept keywords
            score -= 50;
        }

        // Generic Word Protection:
        // Multi-word generic concepts (e.g. "Environment Properties") require full token coverage or exact phrase match.
        if (isMostlyGeneric && !normTitle.contains(normConcept) && !normSnippet.contains(normConcept)) {
            if (titleCoverage < 1.0) {
                score -= 60;
            }
        }

        // Semantic Context Support (+25)
        if (!normContext.isEmpty()) {
            List<String> contextTokens = extractStemmedKeywords(normContext);
            for (String ctxTok : contextTokens) {
                if (normTitle.contains(ctxTok) || normSnippet.contains(ctxTok)) {
                    score += 25;
                    break;
                }
            }
        }

        return score;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[-_]", " ")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> extractStemmedKeywords(String text) {
        String[] words = text.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String w : words) {
            String cleaned = w.trim();
            if (cleaned.length() > 1 && !STOP_WORDS.contains(cleaned)) {
                keywords.add(stemToken(cleaned));
            }
        }
        return keywords;
    }

    private String stemToken(String token) {
        if (token.endsWith("ies") && token.length() > 4) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.endsWith("s") && token.length() > 3 && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private String extractXmlTag(String xml, String tagName) {
        Pattern p = Pattern.compile("<" + tagName + "[^>]*>(.*?)</" + tagName + ">", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1) : "";
    }

    private String extractDomain(String urlStr) {
        try {
            URI uri = new URI(urlStr);
            String domain = uri.getHost();
            if (domain != null) {
                return domain.startsWith("www.") ? domain.substring(4) : domain;
            }
        } catch (Exception ignored) {}
        return "external-resource.org";
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return text.replace(" ", "+");
        }
    }
}
