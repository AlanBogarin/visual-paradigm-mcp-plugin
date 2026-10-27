# Visual Paradigm MCP Plugin — Java 11 / Codex fork

A Model Context Protocol (MCP) server plugin for Visual Paradigm that provides
AI applications with seamless integration to Visual Paradigm's modeling and
diagramming capabilities. The MCP server is embedded directly within
the Visual Paradigm plugin, starting automatically when the plugin loads.

This fork is based on [orgatex/visual-paradigm-mcp-plugin](https://github.com/orgatex/visual-paradigm-mcp-plugin)
and retains the upstream attribution to Manoel Brunnen. It adds a Java 11 runtime,
real modeling operations, Windows installation and Codex configuration.
See [changes](CHANGELOG.md), [installation and configuration](docs/INSTALLATION.md)
and [manual publication of the fork on GitHub](docs/PUBLISHING.md).

## Architecture

- **Java 11, NanoHTTPD and Gson**: Embedded MCP server with isolated, bundled dependencies
- **Visual Paradigm**: OpenAPI integration, tested with Community Edition 18.1 on Windows
- **Maven**: Build automation and dependency management
- **MCP Transport**: Streamable HTTP communication with MCP clients
- **Visual Paradigm Plugin API**: Integration with Visual Paradigm's modeling capabilities
- **JUnit & Mockito**: Comprehensive testing framework

## Features

### MCP Server Integration

The plugin includes an embedded MCP server that:

- **Auto-starts** when Visual Paradigm plugin is loaded
- **Auto-stops** when Visual Paradigm plugin is unloaded
- Listens on **127.0.0.1:8080** with Streamable HTTP endpoint `/mcp`
- Provides **tool capabilities** for external MCP clients

#### Available MCP Tools

- **createUseCaseDiagram(diagramName)**: Create new use case diagrams
- **addActor(actorName, diagramName)**: Add actors to specific diagrams
- **addUseCase(useCaseName, diagramName)**: Add use cases to diagrams
- **addRelationship(actorName, useCaseName, relationshipType, diagramName?)**: Create Association, Include or Extend relationships. For Include/Extend, `actorName` is the source use case name.
- **generateReport(diagramName)**: Generate reports for use case diagrams

#### Future MCP Features (Planned)

##### Resources

- Server Status: Monitor MCP server health and connection status
- Project Information: Retrieve current project details
- Diagram Metadata: Access diagram properties and structure

##### Prompts

- Use Case Templates: Generate standard use case patterns
- Diagram Validation: Check diagram completeness and consistency

### Plugin User Interface

- Toolbar actions for creating diagrams, actors, use cases, relationships and reports
- MCP starts and stops automatically with the plugin

### Plugin Integration

- **Automatic Lifecycle Management**: MCP server starts/stops with plugin
- **Error Handling**: Robust startup/shutdown with detailed logging
- **Java 11 Compatibility**: Works with Visual Paradigm's bundled runtime

## Usage

### Installation

On Windows, with Visual Paradigm, a JDK 11+ and Maven installed (validated using JDK 21):

```powershell
.\run.ps1 package
.\install-windows.ps1 -ConfigureCodex
```

Close Visual Paradigm before installing; start it again after installation.
Then reload MCP servers in Codex (or restart Codex).
The installer backs up existing plugin files and Codex configuration before writing.
Open a project before using the tools; save the project in Visual Paradigm to persist changes.
The plugin is installed under `%APPDATA%\VisualParadigm\plugins\visual-paradigm-mcp-plugin`.
Pass `-VpHome` and `-Maven` to `run.ps1` when those installations are in other locations.
The [installation guide](docs/INSTALLATION.md) also covers installing a packaged ZIP,
custom ports, updating, rollback and troubleshooting.

The legacy Bash helper uses Linux-specific defaults and has not been validated in this fork.
Its build, package and installation commands are:

1. **Build the plugin**:

   ```bash
   ./run build
   ```

2. **Package for distribution**:

   ```bash
   ./run package
   ```

3. **Install to Visual Paradigm**:

   ```bash
   ./run install
   ```

4. **Start Visual Paradigm** - the MCP server will start automatically

### Using the MCP Server

Once Visual Paradigm is running with the plugin:

- **MCP Server Endpoint**: `http://127.0.0.1:8080/mcp` (Streamable HTTP)
- **Server Name**: `visual-paradigm-use-case-mcp-server`
- **Available Tools**: 5 use case diagram operations

#### Connecting with Codex or Other MCP Clients

For Codex, add the following to `~/.codex/config.toml` (the Windows installer does this):

```toml
[mcp_servers.visual_paradigm]
url = "http://127.0.0.1:8080/mcp"
startup_timeout_sec = 20
tool_timeout_sec = 60
```

If you use another harness, such as Claude Code or another MCP client, feel free to 
adapt the configuration, examples, helper scripts and plugin code as needed to make 
the integration work correctly in your environment. Configure your client to use the 
plugin's Streamable HTTP endpoint, following that client's configuration format.

Tools operate on actual models on the Swing event dispatch thread. Names must 
uniquely identify their diagram or element. `generateReport` reads the selected
diagram's models and relationships.
The server binds only to loopback and validates HTTP Host and Origin headers.
It has no authentication and is intended for local use on the same computer.
The current transport uses POST responses; opening `/mcp` in a browser returns HTTP 405.

## Development

### Building

On Windows:

```powershell
.\run.ps1 build
.\run.ps1 test
.\run.ps1 package
```

The packaged plugin is `target/visual-paradigm-mcp-plugin.zip`.
Visual Paradigm's proprietary `openapi.jar` must come from the user's own installation;
it is neither included in the source upload nor bundled in the plugin.

Legacy Bash commands:

```bash
./run build
./run test
./run package
./run all          # Build, test, package, and install
```

### Code Quality

```bash
./run format       # Format code with Google Java Format
./run spotbugs     # Run SpotBugs static analysis
./run pmd          # Run PMD static analysis
```

### Testing

- **Unit and Integration Tests**: JUnit/Mockito and real HTTP transport tests
- **Manual Testing**: All five tools exercised against Visual Paradigm CE 18.1 on Windows
- **HTTP Protocol Tests**: Verify initialization, discovery, tool errors and UTF-8 messages
- **Tool Service Tests**: Validate all MCP tool implementations
- **Plugin Lifecycle Tests**: Test integration with Visual Paradigm

#### Unit and Integration Tests

```bash
./run test
```

#### MCP Protocol Testing

With Visual Paradigm running and the plugin installed:

```powershell
.\examples\Test-McpConnection.ps1
```

To create an optional example in the currently open project:

```powershell
.\examples\Test-McpConnection.ps1 -CreateExample -DiagramName 'Online shop demo'
```

The example remains unsaved until you save the project in Visual Paradigm.

### Debugging

**MCP Server Logging**: Check Visual Paradigm console output for:

- `"MCP Server started for Use Case Plugin"` - successful startup
- `"MCP Server stopped"` - clean shutdown
- Error messages if startup fails

**Configuration**: The default port is 8080. Override it using the JVM option
`-Dvp.mcp.port=8081` in Visual Paradigm, and update the URL in Codex to match.

### Support

- **MCP Protocol**: [Model Context Protocol Specification](https://modelcontextprotocol.io/specification/2025-06-18/architecture)
- **Codex**: [Official MCP configuration documentation](https://learn.chatgpt.com/docs/extend/mcp?surface=cli)
- **Visual Paradigm**: [Plugin API Documentation](https://www.visual-paradigm.com/support/documents/pluginjavadoc/)
- **Licenses**: [Apache-2.0](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md)
