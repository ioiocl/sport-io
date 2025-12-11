package cl.ioio.sportbot.websocket;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;

/**
 * REST endpoint to proxy API-Football requests and avoid CORS issues
 */
@Path("/api/live-matches")
@Slf4j
public class LiveMatchesResource {

    @ConfigProperty(name = "football.api.key")
    String apiKey;

    @ConfigProperty(name = "football.api.host", defaultValue = "v3.football.api-sports.io")
    String apiHost;

    @ConfigProperty(name = "football.api.base-url", defaultValue = "https://v3.football.api-sports.io")
    String baseUrl;

    private final HttpClient httpClient;
    
    public LiveMatchesResource() {
        HttpClient client;
        try {
            // Create a trust manager that accepts all certificates (for development)
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
                    
            log.info("HTTP client initialized with permissive SSL context");
        } catch (Exception e) {
            log.error("Failed to initialize HTTP client with custom SSL context, using default", e);
            client = HttpClient.newHttpClient();
        }
        this.httpClient = client;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLiveMatches() {
        try {
            String url = baseUrl + "/fixtures?live=all";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-apisports-key", apiKey)
                    .header("x-apisports-host", apiHost)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.info("Successfully fetched live matches");
                // Don't add CORS headers manually - Quarkus handles it via application.properties
                return Response.ok(response.body()).build();
            } else {
                log.error("API returned status {}: {}", response.statusCode(), response.body());
                return Response.status(response.statusCode())
                        .entity("{\"error\": \"API request failed\"}")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error fetching live matches", e);
            return Response.serverError()
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
