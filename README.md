# Project 3 Team 44

A Point-of-Sale (POS) system built in Java using Quarkus and Svelte.

## Getting Started

We use [Pixi](https://pixi.prefix.dev/latest/) to manage all our tools.
It will create an isolated cross-platform development environment and will take care of
a lot of issues for us.

You do **not** need to install Java, Python, Maven, or any other libraries used
in this project manually (if you already have them installed -- don't worry either).

### 1. Install Pixi

**Windows:**

```powershell
powershell -ExecutionPolicy Bypass -c "irm -useb https://pixi.sh/install.ps1 | iex"
```

**MacOS & Linux (curl):**

```shell
curl -fsSL https://pixi.sh/install.sh | sh
```

**MacOS & Linux (wget):**

```shell
wget -qO- https://pixi.sh/install.sh | sh
```

Note: You may need to restart your terminal after installing.

### 2. Set up the project

**Clone the GitHub repository (ssh):**

```shell
git clone git@github.com:CSCE-331-Spring-2026-900-908/team44-project3.git
cd team44-project3
```

**Clone the GitHub repository (https):**

```shell
git clone https://github.com/CSCE-331-Spring-2026-900-908/team44-project3.git
cd team44-project3
```

**Install all project tools:**

```shell
pixi install
```
**If you are using a win64 machine for development:**
```shell
pixi run install-bun
```
Note: Restart your terminal after installing bun so it's available in PATH.

### 3. IDE/Text Editor Setup

If your IDE doesn't automatically detect the Pixi Python environment, you can point it to the interpreter directly:

- **macOS/Linux:** `.pixi/envs/default/bin/python`
- **Windows:** `.pixi/envs/default/python.exe`

For the Java SDK, point your IDE to: `.pixi/envs/default/lib/jvm/`

### 4. Running the application

Run in dev mode with live reload:

```shell
pixi run mvn quarkus:dev
```

The Dev UI is available at: <http://localhost:8080/q/dev/>
