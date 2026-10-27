# Installation and configuration

This fork runs an MCP server inside Visual Paradigm. Codex connects to
`http://127.0.0.1:8080/mcp` over Streamable HTTP. Visual Paradigm must be open.

## Verified compatibility

| Component | Verified |
| --- | --- |
| Operating system | Windows 11 |
| Visual Paradigm | Community Edition 18.1 |
| Visual Paradigm Java runtime | Java 11 bundled with the application |
| Build | JDK 21, targeting Java 11 |
| Tests | 21 automated tests and all five MCP tools exercised in the running application |

Building requires **JDK 11 or later**, Maven and a Visual Paradigm installation
containing `lib/openapi.jar`. The JRE bundled with Visual Paradigm can run the
plugin, but building requires a JDK. The first build needs access to Maven Central.
Linux and macOS have not been validated in this fork; the Bash helper retains Linux defaults.

## Option A: build and install from source

1. Download and extract your fork's source code, or clone it. Open PowerShell in its root folder.
2. Check that `java -version` and `mvn -version` work and that `JAVA_HOME` points to your JDK.
3. Build the package. This command also runs the tests:

   ```powershell
   .\run.ps1 package
   ```

   If the tools are installed elsewhere, specify their paths on your computer:

   ```powershell
   .\run.ps1 package -VpHome 'D:\Apps\Visual Paradigm CE 18.1' -Maven 'D:\Tools\apache-maven\bin\mvn.cmd'
   ```

4. Save your projects and close Visual Paradigm before copying the plugin.
5. Install the plugin and optionally add its entry to Codex:

   ```powershell
   .\install-windows.ps1 -ConfigureCodex
   ```

   Without `-ConfigureCodex`, only the plugin is installed.
6. Open Visual Paradigm and a project. Reload the MCP servers in Codex or restart Codex.

The installer copies the plugin to
`%APPDATA%\VisualParadigm\plugins\visual-paradigm-mcp-plugin`.
It does not modify the Visual Paradigm installation in `Program Files` or change its Java runtime.

Installer options:

| Parameter | Usage |
| --- | --- |
| `-PackageRoot` | Folder containing `plugin.xml`, `plugin.properties` and `lib`. |
| `-PluginDirectory` | Destination folder; defaults to the user's plugins path. |
| `-ConfigureCodex` | Add the MCP table if it does not already exist. |
| `-CodexConfigPath` | Alternative configuration file, only used with `-ConfigureCodex`. |
| `-Port` | URL port for a new Codex entry; default: 8080. |

## Option B: install a plugin ZIP

If the maintainer has published a compiled package, download the
`visual-paradigm-mcp-plugin.zip` asset. GitHub's source code ZIP is different and requires building.

With Visual Paradigm closed, extract the plugin ZIP into a folder and run this command there:

```powershell
.\install-windows.ps1 -ConfigureCodex
```

The installer detects `plugin.xml` next to the script. To install a package extracted elsewhere:

```powershell
.\install-windows.ps1 -PackageRoot 'D:\Downloads\visual-paradigm-mcp-plugin' -ConfigureCodex
```

You can also use **Help → Install Plugin** in Visual Paradigm and select the ZIP.
This procedure installs the plugin, but Codex must be configured separately.

## Configure Codex manually

Add this table to your configuration file, preserving its other entries:

```toml
[mcp_servers.visual_paradigm]
url = "http://127.0.0.1:8080/mcp"
startup_timeout_sec = 20
tool_timeout_sec = 60
```

The default location on Windows is `%USERPROFILE%\.codex\config.toml`.
If you use `CODEX_HOME`, the installer uses `config.toml` inside that folder.
A shareable copy is available at [examples/codex.config.toml](../examples/codex.config.toml).

Alternatively, if the Codex CLI is on your PATH:

```powershell
codex mcp add visual_paradigm --url http://127.0.0.1:8080/mcp
codex mcp get visual_paradigm --json
```

Use one of these options. If the entry already exists, the installer preserves it and notifies you;
check its URL manually. The `codex mcp get` command confirms registration,
while the script in the next section checks the actual connection.
The [official OpenAI documentation](https://learn.chatgpt.com/docs/extend/mcp?surface=cli)
describes the configuration format and how to reload the client.

## Use another harness

If you use a different harness, such as Claude Code or another MCP client, feel free to adapt
the configuration, examples, helper scripts and plugin code as needed to make it work correctly
in your environment. The examples above use Codex's configuration format; adapt them to your
client's format and Streamable HTTP support, using `http://127.0.0.1:8080/mcp` or your chosen port.
Install the plugin without `-ConfigureCodex` and configure your harness separately.

## Check the connection and create an example

With Visual Paradigm open and the plugin loaded:

```powershell
.\examples\Test-McpConnection.ps1
```

This mode only initializes MCP, checks `ping` and lists the five tools.
To create an example in the open project, choose a diagram name that does not already exist:

```powershell
.\examples\Test-McpConnection.ps1 -CreateExample -DiagramName 'Online shop demo'
```

The example contains a customer, three use cases and Association, Include and Extend relationships.
You can ask Codex to do the equivalent:

> Use visual_paradigm to create a diagram called "Shop" with a Customer and
> the Place order, Validate payment and Apply coupon use cases. Connect Customer to
> Place order, include Validate payment from Place order and extend Place order
> with Apply coupon. Then call generateReport.

The tools modify the open project and do not save it automatically.
Save it in Visual Paradigm when you want to keep the changes.
Names must uniquely identify an element; an existing actor or use case
can be reused in another diagram.
In `addRelationship`, the `actorName` parameter also represents the source use case
for Include or Extend; `useCaseName` is the target of the UML arrow.

## Change the port

1. Close Visual Paradigm and add `-Dvp.mcp.port=8081` to its JVM options.
   Use your installation's JVM options mechanism and preserve the existing options.
2. Reopen Visual Paradigm. The log should show `http://127.0.0.1:8081/mcp`.
3. Change the URL in the `visual_paradigm` table in Codex and reload the client.

When adding a new entry, the installer accepts `-Port 8081` to configure its URL.
This parameter **does not change Visual Paradigm's port** or replace existing entries.
To check the alternative port:

```powershell
.\examples\Test-McpConnection.ps1 -Endpoint 'http://127.0.0.1:8081/mcp'
```

The server listens only on `127.0.0.1`, validates Host/Origin and does not implement authentication.
It is intended for local use. A remote session or one on another computer cannot reach that
localhost directly; this project does not configure tunnels or expose the server publicly.

## Update, restore or uninstall

To update, save your projects, close Visual Paradigm and run the installer again
with the new package. Replaced files are backed up to `backups/<timestamp>` inside
the plugin folder. The Codex configuration is backed up as `config.toml.backup-<timestamp>`
when its entry is first added.

To revert to an earlier version, close Visual Paradigm and restore the three files from the
backup: `plugin.xml`, `plugin.properties` and `lib/visual-paradigm-mcp-plugin.jar`.
To disable the plugin, move its folder out of `plugins` with Visual Paradigm closed.
In Codex, you can set `enabled = false` in the table or remove it. Preserve the other settings.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `openapi.jar` not found | Pass `-VpHome` with your Visual Paradigm installation folder. |
| Maven or Java not found | Check PATH and `JAVA_HOME`; use `-Maven` to specify an explicit path. |
| PowerShell blocks downloaded scripts | Review the script and use the execution procedure allowed on your computer; changing global policies is not necessary. |
| Codex does not show the tools | Open VP, check the configuration table and reload MCP or restart Codex. |
| Connection refused | VP must be open; check the port and whether the plugin started. |
| Port in use | Choose another port and update both VP and Codex. |
| HTTP 405 when opening `/mcp` | Expected for GET: use the connection check script, which sends MCP POST requests. |
| Origin error in a web tool | This server validates the origin; use Codex or the example's local client. |
| No project open | Open a project in VP before calling the modeling tools. |
| Ambiguous names | Choose unique names or specify `diagramName` in the relationship. |

On Windows, check `%APPDATA%\VisualParadigm\vp.log`. Successful startup includes
`Use Case Plugin loaded` and `Visual Paradigm MCP listening on http://127.0.0.1:8080/mcp`.
The `package` lifecycle runs tests and generates the ZIP; `all` in `run.ps1` also runs
the static analysis and coverage checks configured in Maven.
