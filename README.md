<div align="center">

# 🌤️ Open-Meteo-MCP

A Spring Boot MCP (Model Context Protocol) server that exposes live weather forecast data from [Open-Meteo](https://open-meteo.com/) as a tool for AI applications.

![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-6DB33F?logo=springboot&logoColor=white)
![MCP](https://img.shields.io/badge/MCP-Streamable%20HTTP-8A2BE2)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)

</div>

This server is one half of the applied case study described in the accompanying thesis, *Does MCP Deliver on Performance and Practical Usage?*. Its companion server, which exposes flight data instead of weather data, is [Aviation-Stack-MCP](https://github.com/DarioOstojic/Aviation-Stack-MCP). Together, the two servers demonstrate how an LLM application can coordinate multiple external APIs through MCP without needing custom routing logic for each one.

---

## 📑 Table of Contents

- [Glossary](#glossary)
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Available Tool](#available-tool)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Running the Server](#running-the-server)
- [Connecting to Claude Desktop](#connecting-to-claude-desktop)
- [Testing the Connection](#testing-the-connection)
- [Troubleshooting](#troubleshooting)

---

<a id="glossary"></a>
## 📖 Glossary

| Term | Meaning |
|---|---|
| **Spring Boot** | A widely used Java framework that simplifies building standalone, production-ready applications. It removes much of the manual setup, such as configuring a web server, that would otherwise be needed to run a Java application, allowing a project to start with a single command and a minimal amount of boilerplate code. This server and its companion Aviation-Stack-MCP are both built on Spring Boot. |
| **MCP tool** | A callable function exposed by an MCP server, with a natural-language description of what it does, the inputs it requires, and the output it returns. An LLM reads these descriptions to decide which tool to call for a given request. |
| **Streamable HTTP** | One of the transport protocols MCP supports for communication between a client and a server. It allows a client to connect to a server over a normal HTTP connection, as opposed to running the server as a local process. This server uses Streamable HTTP rather than stdio. |
| **stdio** | Short for **st**andard **i**nput/**o**utput, another MCP transport in which the client starts the server as a local subprocess and communicates with it directly through process input and output, rather than over a network connection. |
| **`mcp-remote`** | A small command-line bridge that allows MCP clients which only support local (stdio) servers, such as Claude Desktop, to connect to a remote server over HTTP. It runs locally, forwards requests to the remote server, and can attach custom headers such as an API key along the way. |
| **Custom connector** | The mechanism Claude Desktop and claude.ai use to connect to a remote MCP server through a graphical settings page, as an alternative to editing the configuration file directly. |
| **Bean** | A Spring term for an object that Spring creates and manages on your behalf, rather than the code creating it directly with `new`. Once Spring knows how to build a bean, it can automatically supply that same instance to any other class that needs it — this is what `OpenMeteoConfiguration.java` does for `WeatherForecastClient`, so that `OpenMeteoTools.java` never has to construct one itself. |

---

<a id="overview"></a>
## 🔭 Overview

This server acts as a bridge between AI assistants and the Open-Meteo weather API, allowing an LLM to retrieve forecast data using natural language rather than calling the underlying REST endpoint directly. Open-Meteo itself is a free, public weather API that does not require an API key, so this server's only authentication layer is the MCP-level key described in [Configuration](#configuration), which controls who is allowed to call this server's tool rather than who is allowed to query Open-Meteo.

In the applied case study from the accompanying thesis, this server was deployed publicly and connected to Claude Desktop alongside a second MCP server exposing flight data. Given a single natural-language request such as asking for sunny travel destinations with direct flights, Claude was able to call tools from both servers, combine their results and produce a single structured answer, without any custom code dictating which API should be called for which part of the request.

---

<a id="tech-stack"></a>
## 🧱 Tech Stack

- **Java 25** / **Spring Boot 3.5.9**
- **Spring AI MCP Server** — exposes the weather tool over [Streamable HTTP](#glossary)
- **Open-Meteo API** — the external, key-free REST API this server wraps
- **Docker** — used for containerized deployment

---

<a id="available-tool"></a>
## 🛠️ Available Tool

The server exposes a single [MCP tool](#glossary):

| Tool | Description |
|---|---|
| `getWeatherForecast` | Fetches forecast data for a given location, including temperature, precipitation, rain, snowfall, wind speed and sunrise/sunset times. |

| Parameter | Type | Required | Description |
|---|---|---|---|
| `latitude` | `Double` | Yes | Latitude of the location, between -90 and 90. |
| `longitude` | `Double` | Yes | Longitude of the location, between -180 and 180. |
| `pastDays` | `Integer` | No | Number of past days of data to include. Defaults to 0. |
| `forecastDays` | `Integer` | No | Number of forecast days to include. Defaults to 7. Maximum 16. |

Latitude and longitude are the only two parameters an LLM strictly needs to supply. In practice, this means a request phrased around a place name, such as "Rome", relies on the LLM itself to resolve that name into coordinates before calling the tool, since the tool only accepts coordinates and has no awareness of place names.

---

<a id="project-structure"></a>
## 🗂️ Project Structure

```text
Open-Meteo-MCP/
│
├── src/
│   └── main/
│       ├── java/
│       │   ├── mcp/
│       │   │   ├── OpenMeteoMcpApplication.java
│       │   │   ├── OpenMeteoTools.java
│       │   │   ├── OpenMeteoConfiguration.java
│       │   │   ├── McpAuthConfig.java
│       │   │   └── McpAuthenticationAspect.java
│       │   │
│       │   └── rest/
│       │       └── WeatherForecastClient.java
│       │
│       └── resources/
│           └── application.properties
│
├── Dockerfile
├── build.gradle.kts
└── settings.gradle.kts
```

### How a request flows

Before examining what each file does, it helps to see the order in which the code gets executed. Here's what occurs, step by step, when a client makes a tool call:

1. The client sends a request to `/mcp`, with an `X-API-Key` header attached. This path is not related to the `mcp/` Java package shown in the project structure above; it is simply the default endpoint that Spring AI registers automatically based on the `spring.ai.mcp.server.protocol=STREAMABLE` setting in `application.properties`.

2. **`McpAuthenticationAspect.java`** catches that request and checks the header's value against the value that is stored in `McpAuthConfig.java`. If the two don't match, the request gets rejected and does not go any further.

3. If the key checks out, the request reaches the `getWeatherForecast` method inside **`OpenMeteoTools.java`**. This method is exposed as an MCP tool through the `@McpTool` annotation, which is also where the tool description shown to the LLM is defined.

4. **`WeatherForecastClient.java`** takes over from there: it validates the latitude, longitude and day-range parameters, builds the actual query string and sends the HTTP request to the Open-Meteo API — the external third-party service this project wraps, not anything internal to this codebase.

5. The response then travels back the same way it came forward: Open-Meteo API → **`WeatherForecastClient.java`** → **`OpenMeteoTools.java`** → the client.

**`OpenMeteoMcpApplication.java`** isn't part of this request-response chain. It only runs when the server starts, handing control over to Spring Boot so the Open-Meteo-MCP server can load its runtime configuration, enable the authentication check and make the `getWeatherForecast` MCP tool available.

### File-by-file reference

Now that the code execution process was shown, here's an explanation of what each file actually does. Different files are grouped together on the basis of the type of role they have:

**Startup**

**`OpenMeteoMcpApplication.java`** is where the server starts. It is the [Spring Boot](#glossary) application's entry point and its only job is to hand control over to Spring Boot once the program runs. It contains no other logic.

**Tool and business logic (steps 3 and 4 above)**

- **`OpenMeteoTools.java`** defines the [MCP tool](#glossary) this server exposes: `getWeatherForecast`. It carries an `@McpTool` annotation, with a description written inside that annotation. This description is what the LLM reads in order to decide whether to call the tool, so this file acts as a small "menu" that tells the AI what weather-related function is available.

- **`WeatherForecastClient.java`** is the file that actually talks to the Open-Meteo API. It validates the latitude, longitude and day-range parameters, builds the query string Open-Meteo expects, sends the HTTP request and hands the raw response back to **`OpenMeteoTools.java`**.

- **`OpenMeteoConfiguration.java`** is a short Spring configuration class with one job: create the **`WeatherForecastClient`** [bean](#glossary) so Spring can automatically plug it into **`OpenMeteoTools.java`**.

**Authentication (step 2 above)**

**`McpAuthenticationAspect.java`** is the file that enforces the authentication check on every tool call. It intercepts every request that reaches an `@McpTool`-annotated method, checks the `X-API-Key` header against the value held in `McpAuthConfig.java` and rejects the request if the two values do not match.

**`McpAuthConfig.java`** holds the configuration for the `X_API_HEADER_KEY` value described in [Configuration](#configuration), reading it in from the environment variable so that it can be checked against incoming requests.

**Configuration**

- **`application.properties`** holds the server's runtime settings: which port it listens on, which MCP transport it uses ([Streamable HTTP](#glossary)), whether authentication is switched on and where to read the `X_API_HEADER_KEY` from.

**Build and deployment**

- **`Dockerfile`** defines how to build the container image: one stage compiles the project and a smaller second stage actually runs it.

- **`build.gradle.kts`** is the build configuration file — it's where you'd see that the project runs on Java 25, Spring Boot 3.5.9 and Spring AI 1.1.2.

- **`settings.gradle.kts`** simply sets the Gradle project's name. It doesn't affect how the server behaves; it's standard Gradle boilerplate code.

---

<a id="configuration"></a>
## ⚙️ Configuration

The server requires one environment variable to be set before it will start successfully.

| Variable | Description |
|---|---|
| `X_API_HEADER_KEY` | A key chosen when deploying this server, used to authenticate incoming MCP requests. Any client connecting to this server must send this same value in an `X-API-Key` header, or its requests will be rejected. |

The `X_API_HEADER_KEY` is not provided by Open-Meteo, since Open-Meteo itself does not require an API key at all. It is a value defined when deploying this server, and the same value then needs to be supplied by every MCP client that connects to it. This mechanism exists because the server is designed to be deployed publicly, and without it, anyone who discovers the server's URL could call its tool freely.

---

<a id="running-the-server"></a>
## 🚀 Running the Server

### Step 1: Choose a value for the MCP authentication key

This is not retrieved from anywhere; it is simply a value chosen, such as a long random string, to be used as the `X_API_HEADER_KEY`. The same value will need to be entered into Claude Desktop's configuration later, so it should be noted down somewhere accessible.

### Step 2: Run the server

```bash
# With Gradle
X_API_HEADER_KEY=your_secret ./gradlew bootRun

# With Docker
docker build -t open-meteo-mcp .
docker run -p 8080:8080 \
  -e X_API_HEADER_KEY=your_secret \
  open-meteo-mcp
```

The server starts on port `8080`. Once running, the MCP endpoint is available at `http://localhost:8080/mcp`.

A request to that endpoint without the correct `X-API-Key` header will be rejected, which is expected behaviour rather than a misconfiguration; this is exactly what the authentication step in [Configuration](#configuration) is meant to enforce.

---

<a id="connecting-to-claude-desktop"></a>
## 🔌 Connecting to Claude Desktop

Claude Desktop's two supported ways of reaching a remote MCP server are a graphical [custom connector](#glossary), and a manual edit of its configuration file. Which one applies depends on whether the server is reachable from the public internet or only running locally.

### Option A: Public deployment, using a custom connector

If this server has been deployed somewhere publicly reachable, it can be added directly through Claude Desktop's settings:

1. Open Claude Desktop and go to **Settings → Connectors**.
2. Select **Add custom connector**.
3. Enter the deployed server's MCP endpoint URL, for example `https://your-deployed-url.onrender.com/mcp`.
4. When prompted for authentication, supply the `X-API-Key` header using the same value set as `X_API_HEADER_KEY` when the server was deployed.
5. Confirm and enable the connector.

Custom connectors require a Pro, Max, Team, or Enterprise plan; they are not available on the free tier of Claude Desktop.

### Option B: Local server, using a configuration file edit

If the server is only running locally, for example with `./gradlew bootRun` on `localhost:8080`, Claude Desktop's custom connector UI cannot reach it directly, since it does not run on the public internet. In this case, a small local bridge tool called [`mcp-remote`](#glossary) is used instead, configured through Claude Desktop's configuration file directly.

1. Locate the configuration file:
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

   This file can also be opened directly from Claude Desktop via **Settings → Developer → Edit Config**.

2. Add an entry for this server under `mcpServers`. If the file already contains other servers, add this entry alongside them rather than replacing the file's contents.

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
           "X-API-Key: your_secret"
         ]
       }
     }
   }
   ```

   The value after `X-API-Key:` must match the `X_API_HEADER_KEY` the server was started with in [Running the Server](#running-the-server).

3. Save the file and restart Claude Desktop completely, quitting it rather than just closing the window.

4. On restart, "open-meteo" should appear as an available tool source, which can be confirmed through the tool/connector icon in the message input area.

`npx` runs `mcp-remote` directly without requiring it to be installed separately beforehand, provided Node.js is already installed on the machine running Claude Desktop.

---

<a id="testing-the-connection"></a>
## ✅ Testing the Connection

Once connected, a natural-language prompt referencing weather or forecast information for a specific place should cause Claude to discover and call `getWeatherForecast`. For example, asking what the weather will be like in a named city over the coming days should be enough for Claude to resolve that city into coordinates and call the tool with them.

If Claude responds without attempting to call the tool, or states that it does not have access to live weather data, the connection has likely not been established correctly; see [Troubleshooting](#troubleshooting) below.

---

<a id="troubleshooting"></a>
## 🩺 Troubleshooting

**Claude Desktop does not list the connector after restarting.** Confirm that the configuration file is valid JSON; a single misplaced comma or bracket will cause Claude Desktop to silently ignore the entire file rather than reporting an error. Restarting must mean fully quitting the application, not just closing its window.

**Requests are rejected with an authentication error.** Confirm that the `X-API-Key` header value used by the client exactly matches the `X_API_HEADER_KEY` the server was started with. If these were set at different times, it is easy for them to have drifted apart.

**Tool calls fail with a parameter validation error.** This typically means the latitude or longitude supplied falls outside the valid range, which is -90 to 90 for latitude and -180 to 180 for longitude, or that `forecastDays` was set above the maximum of 16. These limits are enforced directly by the underlying Open-Meteo API, not by this server.

**The custom connector option in Claude Desktop is greyed out or unavailable.** Custom connectors require a paid Claude Desktop plan, such as Pro, Max, Team, or Enterprise. On the free tier, only the configuration file approach in [Option B](#option-b-local-server-using-a-configuration-file-edit) is available, and even then only for a locally running server.
