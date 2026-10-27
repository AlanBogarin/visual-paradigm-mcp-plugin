# Changelog

## Unreleased — Java 11 / Windows / Codex fork

Based on [orgatex/visual-paradigm-mcp-plugin](https://github.com/orgatex/visual-paradigm-mcp-plugin),
upstream revision `26aff05`. Original provider attribution: Manoel Brunnen.

### Runtime and MCP

- Replace the Spring-based prototype with NanoHTTPD and Gson, compatible with Visual Paradigm's Java 11 runtime.
- Bundle and relocate runtime dependencies and register the packaged JAR in `plugin.xml`.
- Serve stateless Streamable HTTP at `http://127.0.0.1:8080/mcp`.
- Add JSON-RPC errors, input schemas, UTF-8 handling and HTTP Host/Origin validation.
- Implement the five tools using actual OpenAPI operations on the Swing event dispatch thread.
- Correct Visual Paradigm's Extend endpoint convention and report UML arrow direction consistently.
- Reuse existing, unambiguous actor/use case models between diagrams.
- Add actor and relationship captions and space the generated use cases.

### Installation and distribution

- Add Windows build and installation scripts with backups and optional Codex configuration.
- Document custom paths, ports, troubleshooting and manual fork publication.
- Add a shareable Codex configuration example and a read-only MCP check with optional diagram creation.
- Add a curated source export for manual upload without local configuration, caches, logs or project files.
- Include project and third-party license material in the distribution.

### Validation

- 21 automated tests passed on Windows using JDK 21 and Java 11 compilation targets.
- Formatting and Checkstyle checks passed through pre-commit.
- All five MCP tools were exercised against Visual Paradigm Community Edition 18.1, running its bundled Java 11.
- Linux and macOS installations have not been validated for this fork.

The full `verify` lifecycle includes additional static-analysis and coverage gates;
the validation above refers to `package`, the listed tests and pre-commit checks.
