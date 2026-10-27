# Project status

## Java 11 / Windows / Codex fork

The embedded server runs inside Visual Paradigm and exposes five actual use case
modeling tools over Streamable HTTP at http://127.0.0.1:8080/mcp.

### Implemented and exercised

- Java 11-compatible NanoHTTPD/Gson runtime with relocated dependencies.
- JSON-RPC initialization, discovery, errors, UTF-8 and Host/Origin validation.
- OpenAPI operations on the Swing event dispatch thread.
- Correct Visual Paradigm Extend endpoint convention and UML report direction.
- Reuse of unambiguous existing actors/use cases across diagrams.
- Visible actor/relationship captions and spaced use case positions.
- Windows build and installation helpers with backups and optional Codex setup.

### Verified environment

- Windows 11; Visual Paradigm Community Edition 18.1 with bundled Java 11.
- Compiled using JDK 21 and release target 11.
- 21 automated tests passed; all five tools were exercised against the running application.
- Formatting and Checkstyle checks passed through pre-commit.
- The full Maven verify lifecycle has additional gates and is not claimed as validated.
- Linux and macOS installations have not been validated in this fork.

## Preparing the fork for manual publication

- Installation, configuration, custom ports, troubleshooting and rollback: docs/INSTALLATION.md.
- Manual GitHub fork/upload and optional release steps: docs/PUBLISHING.md.
- Fork changes and validation scope: CHANGELOG.md.
- Original Apache-2.0 license and provider attribution retained.
- Third-party distribution notices and NanoHTTPD license included.
- Shareable configuration and connection check are under examples/.
- prepare-fork.ps1 exports only selected source, documentation and license files.
- Local configuration, caches, logs, project files and proprietary OpenAPI binaries are excluded.
- Public status documentation contains no personal installation paths or local project identifiers.
- Publication is manual: no repository creation, remote changes, commit, push or release publication is performed.

### Publication validation

- Rebuilt the installable ZIP successfully; all 21 automated tests passed.
- Checked PowerShell syntax and local documentation links.
- Exercised the installer in isolated workspace folders: custom paths/port,
  preservation of existing configuration, plugin/configuration backups and ZIP auto-detection.
- Ran the public MCP connection check against the running server without changing its project.
- Pre-commit checks passed on all changed/new public files.
- Verified the source export: 42 files, matching SHA-256 hashes, included dotfiles,
  valid public TOML and no personal paths, private keys, binaries or caches.
- Verified the installable ZIP contains its installer, configuration example,
  documentation and license notices, with no proprietary OpenAPI classes.

### Integrity audit

- All 42 public source files matched the prepared fork export byte for byte.
- All 17 production classes in the build matched the installed plugin; the distribution JAR matched the build.
- The obsolete Spring configuration had reappeared unchanged from upstream; its intended removal was restored.
- No implementation, test, installation, configuration-example or documentation changes were lost.

## English translation

- Translated the installation guide, its example prompt and the remaining Spanish README link labels into English.
- Translated the HTTP test's diagram name while retaining non-ASCII characters to verify UTF-8 handling.
- Reviewed project source, documentation and configuration files for remaining Spanish text; none found.
- All 21 automated tests passed using JDK 21 with the Java 11 release target.

## Documentation for other harnesses

- Added README and installation guidance inviting users of Claude Code or other harnesses to freely adapt configuration, examples, helper scripts and plugin code to their environment.
- Clarified that other harnesses use their own configuration format and should be configured separately from the optional Codex installer setup.
