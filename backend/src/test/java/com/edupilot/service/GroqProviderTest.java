package com.edupilot.service.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;

public class GroqProviderTest {

    private GroqProvider groqProvider;

    @BeforeEach
    public void setUp() {
        groqProvider = new GroqProvider();
    }

    @Test
    public void testParseRetryDelayFromGroqResponseBodyExactExample() {
        String rawBody = "{\"error\":{\"message\":\"Rate limit reached for model qwen/qwen3.8-27b in organization org_xyz on tokens per minute (TPM): Limit 1000, Used 807, Requested 401. Please try again in 14.399999999s.\",\"type\":\"tokens\",\"param\":null,\"code\":\"rate_limit_exceeded\"}}";
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), rawBody.getBytes(), null);

        long retryMs = groqProvider.parseRetryDelayMs(ex, rawBody);

        // 14.399999999s -> 14,399 ms + 1,000 ms safety buffer = 15,399 ms
        assertEquals(15399, retryMs, "Should parse 14.399999999s body retry as 15399 ms (14399ms + 1000ms safety buffer)");
        assertTrue(retryMs < 20000, "Retry delay should be ~15.4 seconds, NOT ~58 seconds");
    }

    @Test
    public void testIgnoreResetTokensHeader() {
        String rawBody = "{\"error\":{\"message\":\"Please try again in 14.4s.\"}}";
        HttpHeaders headers = new HttpHeaders();
        // x-ratelimit-reset-tokens is 57.94s - this MUST BE IGNORED by the parser!
        headers.add("x-ratelimit-reset-tokens", "57.940000000s");
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, rawBody.getBytes(), null);

        long retryMs = groqProvider.parseRetryDelayMs(ex, rawBody);

        // Should use body retry ~14.4s + 1s safety = 15,400 ms, NOT 57.94s (~58s)
        assertEquals(15400, retryMs, "Should ignore x-ratelimit-reset-tokens and parse body regex 14.4s as 15400 ms");
    }

    @Test
    public void testRetryAfterHeader() {
        String rawBody = "Rate limit exceeded";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "14.4");
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, rawBody.getBytes(), null);

        long retryMs = groqProvider.parseRetryDelayMs(ex, rawBody);

        assertEquals(15400, retryMs, "Should parse Retry-After header 14.4 as 15400 ms");
    }

    @Test
    public void testResetRequestsHeader() {
        String rawBody = "Rate limit exceeded";
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-reset-requests", "14.4s");
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, rawBody.getBytes(), null);

        long retryMs = groqProvider.parseRetryDelayMs(ex, rawBody);

        assertEquals(15400, retryMs, "Should parse x-ratelimit-reset-requests header 14.4s as 15400 ms");
    }
}
