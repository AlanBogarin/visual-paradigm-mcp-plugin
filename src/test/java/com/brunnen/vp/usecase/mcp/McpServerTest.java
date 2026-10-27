package com.brunnen.vp.usecase.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the actual HTTP transport and JSON-RPC contract. */
class McpServerTest {
  private McpServer server;
  private URI endpoint;
  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void start() throws Exception {
    server =
        new McpServer(
            new UseCaseMcpToolsService() {
              @Override
              public String createUseCaseDiagram(String diagramName) {
                return "Created: " + diagramName;
              }

              @Override
              public String generateReport(String diagramName) {
                throw new IllegalStateException("No project is open.");
              }
            },
            0);
    server.start();
    endpoint = URI.create("http://127.0.0.1:" + server.getPort() + "/mcp");
  }

  @AfterEach
  void stop() {
    server.stop();
    assertFalse(server.isRunning());
  }

  @Test
  void initializesAndListsFiveTools() throws Exception {
    JsonObject reply =
        rpc(
            "initialize",
            "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}");
    assertEquals(
        "2025-06-18", reply.getAsJsonObject("result").get("protocolVersion").getAsString());
    assertEquals(
        5, rpc("tools/list", "{}").getAsJsonObject("result").getAsJsonArray("tools").size());
    assertTrue(rpc("ping", "{}").getAsJsonObject("result").entrySet().isEmpty());
  }

  @Test
  void callsToolAndPreservesUnicode() throws Exception {
    JsonObject result =
        rpc(
                "tools/call",
                "{\"name\":\"createUseCaseDiagram\","
                    + "\"arguments\":{\"diagramName\":\"Loan management — £\"}}")
            .getAsJsonObject("result");
    assertFalse(result.get("isError").getAsBoolean());
    assertEquals(
        "Created: Loan management — £",
        result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString());
  }

  @Test
  void distinguishesInvalidArgumentsFromOperationFailure() throws Exception {
    assertEquals(
        -32602,
        rpc("tools/call", "{\"name\":\"addActor\",\"arguments\":{}}")
            .getAsJsonObject("error")
            .get("code")
            .getAsInt());
    assertEquals(
        -32602,
        rpc("tools/call", "{\"name\":\"unknown\"}")
            .getAsJsonObject("error")
            .get("code")
            .getAsInt());
    JsonObject result =
        rpc("tools/call", "{\"name\":\"generateReport\",\"arguments\":{\"diagramName\":\"Test\"}}")
            .getAsJsonObject("result");
    assertTrue(result.get("isError").getAsBoolean());
    assertTrue(
        result
            .getAsJsonArray("content")
            .get(0)
            .getAsJsonObject()
            .get("text")
            .getAsString()
            .contains("No project"));
  }

  @Test
  void handlesProtocolErrorsAndNotifications() throws Exception {
    assertEquals(-32601, rpc("unknown", "{}").getAsJsonObject("error").get("code").getAsInt());
    assertEquals(
        -32700,
        JsonParser.parseString(post("{", null).body())
            .getAsJsonObject()
            .getAsJsonObject("error")
            .get("code")
            .getAsInt());
    assertEquals(
        202,
        post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", null).statusCode());
    assertEquals(
        405,
        client
            .send(
                HttpRequest.newBuilder(endpoint).GET().build(),
                HttpResponse.BodyHandlers.ofString())
            .statusCode());
  }

  @Test
  void rejectsRemoteOrigins() throws Exception {
    assertEquals(403, post("{}", "https://untrusted.example").statusCode());
  }

  private JsonObject rpc(String method, String params) throws Exception {
    HttpResponse<String> response =
        post(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\""
                + method
                + "\",\"params\":"
                + params
                + "}",
            null);
    assertEquals(200, response.statusCode());
    return JsonParser.parseString(response.body()).getAsJsonObject();
  }

  private HttpResponse<String> post(String body, String origin) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream");
    if (origin != null) {
      request.header("Origin", origin);
    }
    return client.send(
        request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
