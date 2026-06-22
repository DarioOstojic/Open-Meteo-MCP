# Open-Meteo-MCP

A Spring Boot MCP (Model Context Protocol) server that exposes weather forecast data from [Open-Meteo](https://open-meteo.com/) as a tool for AI applications.

This server is one half of the applied case study described in the accompanying thesis, *Does MCP Deliver on Performance and Practical Usage? Testing REST and MCP Through a Controlled Experiment and a Real-World Case Study*. Its companion server, which exposes flight data instead of weather data, is [Aviation-Stack-MCP](https://github.com/DarioOstojic/Aviation-Stack-MCP). Together, the two servers demonstrate how an LLM application can coordinate multiple external APIs through MCP without needing custom routing logic for each one.

---

## Table of Contents

- [Glossary](#glossary)
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Available Tool](#available-tool)
- [Configuration](#configuration)
- [Running the Server](#running-the-server)
- [Connecting to Claude Desktop](#connecting-to-claude-desktop)
- [Testing the Connection](#testing-the-connection)
- [Troubleshooting](#troubleshooting)

---

## Glossary

| Term | Meaning |
|---|---|
| **MCP tool** | A callable function exposed by an MCP server, with a natural-language description of what it does, the inputs it requires and the output it returns. An LLM reads these descriptions to decide which tool to call for a given request. |
| **Streamable HTTP** | One of the transport protocols MCP supports for communication between a client and a server. It allows a client to connect to a server over a normal HTTP connection, as opposed to running the server as a local process. This server uses Streamable HTTP rather than stdio. |
| **stdio** | Short for **st**andard **i**nput/**o**utput, another MCP transport in which the client starts the server as a local subprocess and communicates with it directly through process input and output, rather than over a network connection. |
| **`mcp-remote`** | A small command-line bridge that allows MCP clients which only support local (stdio) servers, such as Claude Desktop, to connect to a remote server over HTTP. It runs locally, forwards requests to the remote server and can attach custom headers such as an API key along the way. |
| **Custom connector** | The mechanism Claude Desktop and claude.ai use to connect to a remote MCP server through a graphical settings page, as an alternative to editing the configuration file directly. |

---

## Overview

This server acts as a bridge between AI assistants and the Open-Meteo weather API, allowing an LLM to retrieve forecast data using natural language rather than calling the underlying REST endpoint directly. Open-Meteo itself is a free, public weather API that does not require an API key, so this server's only authentication layer is the MCP-level key described in [Configuration](#configuration), which controls who is allowed to call this server's tool rather than who is allowed to query Open-Meteo.

In the applied case study from the accompanying thesis, this server was deployed publicly and connected to Claude Desktop alongside a second MCP server exposing flight data. Given a single natural-language request such as asking for sunny travel destinations with direct flights, Claude was able to call tools from both servers, combine their results and produce a single structured answer, without any custom code dictating which API should be called for which part of the request.

---

## Tech Stack

- **Java 25** / Spring Boot 3.5
- **Spring AI MCP Server** — exposes the weather tool over [Streamable HTTP](#glossary)
- **Open-Meteo API** — the external, key-free REST API this server wraps
- **Docker** — used for containerized deployment

---

## Available Tool

The server exposes a single [MCP tool](#glossary):

**`getWeatherForecast`** — fetches forecast data for a given location, including temperature, precipitation, rain, snowfall, wind speed, and sunrise/sunset times.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `latitude` | Double | Yes | Latitude of the location, between -90 and 90. |
| `longitude` | Double | Yes | Longitude of the location, between -180 and 180. |
| `pastDays` | Integer | No | Number of past days of data to include. Defaults to 0. |
| `forecastDays` | Integer | No | Number of forecast days to include. Defaults to 7. Maximum 16. |

Latitude and longitude are the only two parameters an LLM strictly needs to supply; in practice, this means a request phrased around a place name (for example, "Rome") relies on the LLM itself to resolve that name into coordinates before calling the tool, since the tool only accepts coordinates and has no awareness of place names.

---

## Configuration

The server requires one environment variable to be set before it will start successfully.

| Variable | Description |
|---|---|
| `X_API_HEADER_KEY` | A key chosen when deploying this server, used to authenticate incoming MCP requests. Any client connecting to this server must send this same value in an `X-API-Key` header, or its requests will be rejected. |

This value is not provided by Open-Meteo, since Open-Meteo itself does not require an API key at all; it is a value defined when deploying this server, and the same value then needs to be supplied by every MCP client that connects to it. This mechanism exists because the server is designed to be deployed publicly, and without it, anyone who discovers the server's URL could call its tool freely.

---

## Running the Server

### Step 1: Choose a value for the MCP authentication key

This is not retrieved from anywhere; it is simply a value chosen, such as a long random string, to be used as the `X_API_HEADER_KEY`. The same value will need to be entered into Claude Desktop's configuration later, so it should be noted down somewhere accessible.

### Step 2: Run the server

```bash
# With Docker
docker build -t open-meteo-mcp .
docker run -p 8080:8080 -e X_API_HEADER_KEY=your-api-key open-meteo-mcp

# With Gradle
X_API_HEADER_KEY=your-api-key ./gradlew bootRun
```

The server starts on port `8080`. Once running, the MCP endpoint is available at `http://localhost:8080/mcp`.

A request to that endpoint without the correct `X-API-Key` header will be rejected, which is expected behaviour rather than a misconfiguration; this is exactly what the authentication step in [Configuration](#configuration) is meant to enforce.

---

## Connecting to Claude Desktop

Claude Desktop's two supported ways of reaching a remote MCP server are a graphical [custom connector](#glossary), and a manual edit of its configuration file. Which one applies depends on whether the server is reachable from the public internet or only running locally.

### Option A: Public deployment, using a custom connector

If this server has been deployed somewhere publicly reachable (the applied case study in the thesis used Render, but any host that allows outbound traffic to reach the deployed URL would work the same way), it can be added directly through Claude Desktop's settings:

1. Open Claude Desktop and go to **Settings → Connectors**.
2. Select **Add custom connector**.
3. Enter the deployed server's MCP endpoint URL, for example `https://your-deployed-url.onrender.com/mcp`.
4. When prompted for authentication, supply the `X-API-Key` header using the same value set as `X_API_HEADER_KEY` when the server was deployed.
5. Confirm and enable the connector.

Custom connectors require a Pro, Max, Team, or Enterprise plan; they are not available on the free tier of Claude Desktop.

### Option B: Local server, using a configuration file edit

If the server is only running locally (for example, with `./gradlew bootRun` on `localhost:8080`), Claude Desktop's custom connector UI cannot reach it directly, since it does not run on the public internet. In this case, a small local bridge tool called [`mcp-remote`](#glossary) is used instead, configured through Claude Desktop's configuration file directly.

1. Locate the configuration file:
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

   This file can also be opened directly from Claude Desktop via **Settings → Developer → Edit Config**.

2. Add an entry for this server under `mcpServers`. If the file already contains other servers (such as a companion entry for Aviation-Stack-MCP), add this entry alongside them rather than replacing the file's contents.

   ```json
   {
     "mcpServers": {
       "open-meteo": {
         "command": "npx",
         "args": [
           "-y",
           "mcp-remote",
           "http://localhost:8080/mcp",
           "--header",
           "X-API-Key: your-api-key"
         ]
       }
     }
   }
   ```

   The value after `X-API-Key:` must match the `X_API_HEADER_KEY` the server was started with in [Running the Server](#running-the-server).

3. Save the file and restart Claude Desktop completely (quitting it rather than just closing the window).

4. On restart, "open-meteo" should appear as an available tool source, which can be confirmed through the tool/connector icon in the message input area.

`npx` runs `mcp-remote` directly without requiring it to be installed separately beforehand, provided Node.js is already installed on the machine running Claude Desktop.

---

## Testing the Connection

Once connected, a natural-language prompt referencing weather or forecast information for a specific place should cause Claude to discover and call `getWeatherForecast`. For example, asking what the weather will be like in a named city over the coming days should be enough for Claude to resolve that city into coordinates and call the tool with them.

If Claude responds without attempting to call the tool, or states that it does not have access to live weather data, the connection has likely not been established correctly; see [Troubleshooting](#troubleshooting) below.

---

## Troubleshooting

**Claude Desktop does not list the connector after restarting.** Confirm that the configuration file is valid JSON; a single misplaced comma or bracket will cause Claude Desktop to silently ignore the entire file rather than reporting an error. Restarting must mean fully quitting the application, not just closing its window.

**Requests are rejected with an authentication error.** Confirm that the `X-API-Key` header value used by the client exactly matches the `X_API_HEADER_KEY` the server was started with. If these were set at different times, it is easy for them to have drifted apart.

**Tool calls fail with a parameter validation error.** This typically means the latitude or longitude supplied falls outside the valid range (-90 to 90 for latitude, -180 to 180 for longitude), or that `forecastDays` was set above the maximum of 16. These limits are enforced directly by the underlying Open-Meteo API, not by this server.

**The custom connector option in Claude Desktop is greyed out or unavailable.** Custom connectors require a paid Claude Desktop plan (Pro, Max, Team, or Enterprise); on the free tier, only the configuration file approach in [Option B](#option-b-local-server-using-a-configuration-file-edit) is available, and even then only for a locally running server.
