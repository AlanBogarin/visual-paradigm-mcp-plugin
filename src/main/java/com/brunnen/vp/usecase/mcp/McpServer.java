package com.brunnen.vp.usecase.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stateless Streamable HTTP MCP server compatible with Visual Paradigm's Java 11 runtime. */
public class McpServer {
  private static final int MAX_BODY_BYTES = 1048576;
  private static final String PROTOCOL = "2025-06-18";
  private static final List<String> PROTOCOLS = List.of(PROTOCOL, "2025-03-26", "2024-11-05");
  private static final Gson JSON = new GsonBuilder().serializeNulls().create();
  private final UseCaseMcpToolsService tools;
  private final int port;
  private NanoHTTPD server;

  /** Creates the plugin server on the loopback port configured by vp.mcp.port (default 8080). */
  public McpServer() {
    this(new UseCaseMcpToolsService(), Integer.getInteger("vp.mcp.port", 8080));
  }

  public McpServer(UseCaseMcpToolsService tools, int port) {
    this.tools = tools;
    this.port = port;
  }

  /** Starts once, propagating a bind failure to the plugin lifecycle handler. */
  public synchronized void start() throws IOException {
    if (isRunning()) {
      return;
    }
    NanoHTTPD candidate =
        new NanoHTTPD("127.0.0.1", port) {
          @Override
          public Response serve(IHTTPSession session) {
            return handle(session);
          }
        };
    candidate.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
    server = candidate;
    System.out.println("Visual Paradigm MCP listening on http://127.0.0.1:" + getPort() + "/mcp");
  }

  /** Stops the listener and releases its connections when the plugin unloads. */
  public synchronized void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  public synchronized boolean isRunning() {
    return server != null && server.isAlive();
  }

  public synchronized int getPort() {
    return server == null ? port : server.getListeningPort();
  }

  private NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
    String authority = session.getHeaders().get("host");
    String loopback = "127.0.0.1:" + getPort();
    String localhost = "localhost:" + getPort();
    if (!loopback.equals(authority) && !localhost.equals(authority)) {
      return response(NanoHTTPD.Response.Status.FORBIDDEN, "Invalid Host.");
    }
    String origin = session.getHeaders().get("origin");
    if (origin != null
        && !("http://" + loopback).equals(origin)
        && !("http://" + localhost).equals(origin)) {
      return response(NanoHTTPD.Response.Status.FORBIDDEN, "Invalid Origin.");
    }
    if (!"/mcp".equals(session.getUri())) {
      return response(NanoHTTPD.Response.Status.NOT_FOUND, "Use /mcp.");
    }
    if (session.getMethod() != NanoHTTPD.Method.POST) {
      NanoHTTPD.Response result =
          response(
              NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
              "Use POST. This server does not offer an SSE stream.");
      result.addHeader("Allow", "POST");
      return result;
    }
    String version = session.getHeaders().get("mcp-protocol-version");
    if (version != null && !PROTOCOLS.contains(version)) {
      return response(NanoHTTPD.Response.Status.BAD_REQUEST, "Unsupported MCP protocol version.");
    }
    String contentType = session.getHeaders().getOrDefault("content-type", "");
    if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
      return response(
          NanoHTTPD.Response.Status.UNSUPPORTED_MEDIA_TYPE, "Expected application/json.");
    }
    try {
      long size = Long.parseLong(session.getHeaders().getOrDefault("content-length", "-1"));
      if (size < 0
          || size > MAX_BODY_BYTES
          || session.getHeaders().containsKey("transfer-encoding")) {
        return response(
            NanoHTTPD.Response.Status.BAD_REQUEST,
            "Expected Content-Length between 0 and 1048576.");
      }
      Map<String, String> body = new HashMap<>();
      // JSON-RPC is always UTF-8, including when the client omits charset.
      session.getHeaders().put("content-type", "application/json; charset=utf-8");
      session.parseBody(body);
      JsonElement parsed = JsonParser.parseString(body.getOrDefault("postData", ""));
      if (!parsed.isJsonObject()) {
        return json(error(null, -32600, "Expected one JSON-RPC object."));
      }
      JsonObject request = parsed.getAsJsonObject();
      JsonElement id = request.get("id");
      if (!isString(request.get("jsonrpc"))
          || !"2.0".equals(request.get("jsonrpc").getAsString())
          || !isString(request.get("method"))
          || (id != null
              && !id.isJsonNull()
              && (!id.isJsonPrimitive() || id.getAsJsonPrimitive().isBoolean()))) {
        return json(error(null, -32600, "Invalid JSON-RPC request."));
      }
      String method = request.get("method").getAsString();
      if (id == null) {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.ACCEPTED, "application/json", "");
      }
      JsonObject params = new JsonObject();
      if (request.has("params")) {
        if (!request.get("params").isJsonObject()) {
          return json(error(id, -32602, "params must be an object."));
        }
        params = request.getAsJsonObject("params");
      }
      try {
        JsonObject result;
        switch (method) {
          case "initialize":
            result = initialize(params);
            break;
          case "ping":
            result = new JsonObject();
            break;
          case "tools/list":
            result = new JsonObject();
            result.add("tools", toolDefinitions());
            break;
          case "tools/call":
            result = call(params);
            break;
          default:
            return json(error(id, -32601, "Method not found: " + method));
        }
        JsonObject reply = new JsonObject();
        reply.addProperty("jsonrpc", "2.0");
        reply.add("id", id);
        reply.add("result", result);
        return json(reply);
      } catch (IllegalArgumentException e) {
        return json(error(id, -32602, e.getMessage()));
      } catch (RuntimeException e) {
        e.printStackTrace();
        return json(error(id, -32603, "Internal server error."));
      }
    } catch (JsonParseException e) {
      return json(error(null, -32700, "Invalid JSON."));
    } catch (IOException | NanoHTTPD.ResponseException | IllegalArgumentException e) {
      return response(NanoHTTPD.Response.Status.BAD_REQUEST, "Invalid HTTP request body.");
    }
  }

  private static boolean isString(JsonElement value) {
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
  }

  private JsonObject initialize(JsonObject params) {
    String requested = required(params, "protocolVersion");
    JsonObject result = new JsonObject();
    result.addProperty("protocolVersion", PROTOCOLS.contains(requested) ? requested : PROTOCOL);
    JsonObject capabilities = new JsonObject();
    JsonObject toolCapability = new JsonObject();
    toolCapability.addProperty("listChanged", false);
    capabilities.add("tools", toolCapability);
    result.add("capabilities", capabilities);
    JsonObject info = new JsonObject();
    info.addProperty("name", "visual-paradigm-use-case-mcp-server");
    info.addProperty("version", "0.1.0");
    result.add("serverInfo", info);
    result.addProperty(
        "instructions",
        "Operate on the project currently open in Visual Paradigm. "
            + "Changes remain unsaved until the user saves the project. "
            + "Use unique diagram and element names. "
            + "For Include/Extend, actorName is the source use case. "
            + "generateReport reads the actual diagram.");
    return result;
  }

  private JsonObject call(JsonObject params) {
    String name = required(params, "name");
    JsonObject definition = null;
    for (JsonElement value : toolDefinitions()) {
      if (name.equals(value.getAsJsonObject().get("name").getAsString())) {
        definition = value.getAsJsonObject();
        break;
      }
    }
    if (definition == null) {
      throw new IllegalArgumentException("Unknown tool: " + name);
    }
    JsonObject args = new JsonObject();
    if (params.has("arguments")) {
      if (!params.get("arguments").isJsonObject()) {
        throw new IllegalArgumentException("arguments must be an object.");
      }
      args = params.getAsJsonObject("arguments");
    }
    JsonObject schema = definition.getAsJsonObject("inputSchema");
    for (JsonElement key : schema.getAsJsonArray("required")) {
      required(args, key.getAsString());
    }
    for (Map.Entry<String, JsonElement> entry : args.entrySet()) {
      if (!schema.getAsJsonObject("properties").has(entry.getKey())
          || !isString(entry.getValue())) {
        throw new IllegalArgumentException("Unknown or non-string argument: " + entry.getKey());
      }
    }
    try {
      String output;
      switch (name) {
        case "createUseCaseDiagram":
          output = tools.createUseCaseDiagram(required(args, "diagramName"));
          break;
        case "addActor":
          output = tools.addActor(required(args, "actorName"), required(args, "diagramName"));
          break;
        case "addUseCase":
          output = tools.addUseCase(required(args, "useCaseName"), required(args, "diagramName"));
          break;
        case "addRelationship":
          output =
              tools.addRelationship(
                  required(args, "actorName"),
                  required(args, "useCaseName"),
                  required(args, "relationshipType"),
                  args.has("diagramName") ? required(args, "diagramName") : null);
          break;
        case "generateReport":
          output = tools.generateReport(required(args, "diagramName"));
          break;
        default:
          throw new IllegalArgumentException("Unknown tool: " + name);
      }
      return toolResult(output, false);
    } catch (RuntimeException e) {
      return toolResult(
          e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), true);
    }
  }

  private static String required(JsonObject object, String key) {
    if (!isString(object.get(key)) || object.get(key).getAsString().trim().isEmpty()) {
      throw new IllegalArgumentException("Missing or empty string: " + key);
    }
    return object.get(key).getAsString();
  }

  private static JsonArray toolDefinitions() {
    JsonArray result = new JsonArray();
    result.add(
        tool(
            "createUseCaseDiagram",
            "Create and open a use case diagram in the current project.",
            List.of("diagramName"),
            List.of()));
    result.add(
        tool(
            "addActor",
            "Create an actor and visible shape in the named use case diagram.",
            List.of("actorName", "diagramName"),
            List.of()));
    result.add(
        tool(
            "addUseCase",
            "Create a use case and visible shape in the named diagram.",
            List.of("useCaseName", "diagramName"),
            List.of()));
    result.add(
        tool(
            "addRelationship",
            "Create Association (Actor -> UseCase), Include or Extend (UseCase -> UseCase). "
                + "actorName is the source name. "
                + "Supply diagramName if names occur in several diagrams.",
            List.of("actorName", "useCaseName", "relationshipType"),
            List.of("diagramName")));
    result.add(
        tool(
            "generateReport",
            "Read actors, use cases and relationships actually present in the named diagram.",
            List.of("diagramName"),
            List.of()));
    return result;
  }

  private static JsonObject tool(
      String name, String description, List<String> required, List<String> optional) {
    JsonObject result = new JsonObject();
    result.addProperty("name", name);
    result.addProperty("description", description);
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    JsonObject properties = new JsonObject();
    List<String> names = new java.util.ArrayList<>(required);
    names.addAll(optional);
    for (String key : names) {
      JsonObject property = new JsonObject();
      property.addProperty("type", "string");
      property.addProperty("minLength", 1);
      if ("relationshipType".equals(key)) {
        JsonArray choices = new JsonArray();
        choices.add("Association");
        choices.add("Include");
        choices.add("Extend");
        property.add("enum", choices);
      }
      properties.add(key, property);
    }
    schema.add("properties", properties);
    JsonArray keys = new JsonArray();
    required.forEach(keys::add);
    schema.add("required", keys);
    schema.addProperty("additionalProperties", false);
    result.add("inputSchema", schema);
    JsonObject hints = new JsonObject();
    hints.addProperty("readOnlyHint", "generateReport".equals(name));
    hints.addProperty("destructiveHint", false);
    hints.addProperty("openWorldHint", false);
    result.add("annotations", hints);
    return result;
  }

  private static JsonObject toolResult(String text, boolean failed) {
    JsonObject result = new JsonObject();
    JsonObject content = new JsonObject();
    content.addProperty("type", "text");
    content.addProperty("text", text);
    JsonArray contents = new JsonArray();
    contents.add(content);
    result.add("content", contents);
    result.addProperty("isError", failed);
    return result;
  }

  private static JsonObject error(JsonElement id, int code, String message) {
    JsonObject result = new JsonObject();
    result.addProperty("jsonrpc", "2.0");
    result.add("id", id == null ? com.google.gson.JsonNull.INSTANCE : id);
    JsonObject error = new JsonObject();
    error.addProperty("code", code);
    error.addProperty("message", message);
    result.add("error", error);
    return result;
  }

  private static NanoHTTPD.Response json(JsonObject value) {
    return NanoHTTPD.newFixedLengthResponse(
        NanoHTTPD.Response.Status.OK, "application/json; charset=utf-8", JSON.toJson(value));
  }

  private static NanoHTTPD.Response response(NanoHTTPD.Response.Status status, String message) {
    return NanoHTTPD.newFixedLengthResponse(status, "text/plain; charset=utf-8", message);
  }
}
