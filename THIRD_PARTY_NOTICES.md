# Third-party notices

## Upstream project

This fork derives from [orgatex/visual-paradigm-mcp-plugin](https://github.com/orgatex/visual-paradigm-mcp-plugin).
The upstream plugin attributes its provider to Manoel Brunnen. That attribution is retained.
The project is distributed under the Apache License 2.0; see [LICENSE](LICENSE).
Changes made by this fork are described in [CHANGELOG.md](CHANGELOG.md).

## Bundled runtime dependencies

| Component | Version | License | Source |
| --- | --- | --- | --- |
| NanoHTTPD | 2.3.1 | BSD-3-Clause | [NanoHttpd/nanohttpd](https://github.com/NanoHttpd/nanohttpd) |
| Gson | 2.11.0 | Apache-2.0 | [google/gson](https://github.com/google/gson) |
| Error Prone annotations, a transitive Gson dependency | 2.27.0 | Apache-2.0 | [google/error-prone](https://github.com/google/error-prone) |

NanoHTTPD and Gson packages are relocated inside the plugin JAR to avoid conflicts with Visual Paradigm.
The NanoHTTPD license text from the distributed 2.3.1 JAR is reproduced in
[licenses/NanoHTTPD-LICENSE.txt](licenses/NanoHTTPD-LICENSE.txt).
The Apache-2.0 license text is included in [LICENSE](LICENSE).

Build and test dependencies are resolved by Maven and are not part of the plugin runtime distribution.

## Visual Paradigm OpenAPI

Visual Paradigm and its `openapi.jar` are provided by the user's own Visual Paradigm installation.
The OpenAPI JAR is used for compilation and tests and is not redistributed by this fork's plugin ZIP or source export.
This fork is an independent project and is not an official Visual Paradigm or OpenAI distribution.
