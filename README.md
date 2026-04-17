# Link State Routing

> Project "_**Sparrowhawk**_" by Frank Xikun Yang and Jim Jinkun Yang
> 
> [_What is sparrowhawk?_](https://en.wikipedia.org/wiki/Eurasian_sparrowhawk)
> 
> This project aims to develop a Java program that emulates the process done by routers exercising the **link state routing (LSR) protocol**.
> 
> [_What is link state routing?_](https://en.wikipedia.org/wiki/Link-state_routing_protocol)  [_What is the Dijkstra's algorithm?_](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm)

## How to Run

This project uses plain Java, so Maven or Gradle is not required.

**Java 11** or newer is recommended.

First, compile the source files:

```shell
javac -cp "lib/*" -d . $(find src -name "*.java")
```

Note that all compiled `.class` files will placed to the project root, so the program can be started with the same command style used in the project specification.

### Run GUI (Without Arguments)

```shell
java LSRCompute
```

**If no command-line arguments are given, the program starts the GUI.** 

In the GUI, users can:

- Browse and parse a `.lsa` topology file.
- View the network topology in a table.
- Run the LSR calculation in `SS` or `CA` mode.
- Trace Dijkstra's algorithm step by step in `SS` mode.
- Use `All Nodes` by default in `CA` mode, or select one source router.
- Edit the topology by adding/removing nodes, updating links, breaking links, and saving the `.lsa` file.

### Run CLI (With Arguments)

The command-line interface **follows the project requirement**:

```shell
java LSRCompute <file.lsa> <source> [SS|CA]
```

Examples:

```shell
java LSRCompute testdata/test.lsa A
java LSRCompute testdata/test.lsa A CA
java LSRCompute testdata/test.lsa A SS
```

If `[SS|CA]` is not provided, `CA` mode is used by default.

## Project Overview

This project simulates how routers use the **Link State Routing (LSR) protocol** to compute the shortest paths.

The program reads **Link State Advertisement (LSA)** data from an ASCII `.lsa` file. It then builds a network graph, checks whether the topology is valid, and runs **Dijkstra's algorithm** to find the shortest paths.

The input format is:

```text
A: B:5 C:3 D:5
B: A:5 C:4 E:3 F:2
C: A:3 B:4 D:1 E:6
```

Each line describes one router and the links from that router:

- The first token is the router identifier followed by `:`.
- Each later token uses `neighbor:cost` format.
- Link cost is bidirectional, so `A: B:5` should match `B: A:5`.
- The validator checks missing reverse links and mismatched costs.

The program supports two computation modes:

- `CA` - Compute the result in one run and show the summary.
- `SS` - Visit one node at a time and show each newly found path before continuing.

### Additional Features

- Javax Swing-based GUI.
- Topology editor for dynamic network changes.
- File writer for saving edited `.lsa` files.
- Input validation and user-friendly error messages.
- Unit tests for parsing, graph operations, Dijkstra computation, formatting, CLI behavior, and topology editing.

## Architecture

The code is split into small classes, with each class handling one main job:

| File | Responsibility |
| --- | --- |
| `src/LSRCompute.java` | Entry point. Opens the GUI by default, or runs CLI mode when arguments are provided. |
| `src/LSRGUI.java` | Main Swing GUI for loading files, viewing topology, and running `SS`/`CA` mode. |
| `src/LSREditorGUI.java` | GUI editor for changing nodes and links in a topology file. |
| `src/LSRFileParser.java` | Parses `.lsa` files or raw LSA text into a graph. |
| `src/LSRFileWriter.java` | Saves graph data in `.lsa` format. |
| `src/LSRGraphValidator.java` | Checks graph correctness, especially bidirectional link consistency. |
| `src/LSRDijkstraCalculator.java` | Runs Dijkstra's algorithm and records final routes plus single-step progress. |
| `src/LSRResultFormatter.java` | Formats step output and summary results for CLI and GUI logs. |
| `src/LSRTopologyService.java` | Handles topology operations used by the editor. |
| `src/models/` | Data models for graphs, links, routes, Dijkstra steps, and Dijkstra results. |
| `src/argparser/` | Lightweight command-line argument parser. |
| `test/` | JUnit tests for the main components. |

