# store

A Git-like version control system built from scratch in Java — no VCS libraries, just `java.security`, `java.util.zip`, `java.nio.file`, and plain content-addressable storage. Built as a learning project following the roadmap in `CLAUDE.md`.

The one deliberate departure from Git: **`store merge` never produces a conflict**. See [Merging without conflicts](#merging-without-conflicts) below.

## Requirements

- Java 21
- Maven

## Building

```bash
mvn compile      # compile
mvn test         # run the unit test suite
mvn package      # build the jars
```

## Running

This project produces two independent programs from the same codebase:

- **`Main`** — the `store` CLI (init, add, commit, branch, merge, push, pull, ...)
- **`StoreServerApplication`** — a Spring Boot server that acts as the "remote" (`origin`) for push/pull

```bash
# CLI
mvn exec:java -Dexec.mainClass=Main -Dexec.args="init"
# or, after packaging:
java -cp target/store-1.0-SNAPSHOT.jar Main init

# Remote server (separate process, defaults to port 8080)
mvn spring-boot:run
```

> Both `maven-shade-plugin` and `spring-boot-maven-plugin` are configured in `pom.xml`, and both currently target `target/store-1.0-SNAPSHOT.jar` — they can overwrite each other's output. Until that's sorted out, prefer running via `mvn exec:java`/`mvn spring-boot:run` (as above), or check which jar actually landed in `target/` before relying on `java -jar`.

## Commands

Run `store --help` for the full command list, or `store <command> --help` for a command's own usage — both are backed by Picocli's auto-generated help, not hand-written text.

| Command | Description |
|---|---|
| `store init` | Create a `.store/` repository in the current directory |
| `store hash-object <file>` | Hash and store a file as a blob, print its hash |
| `store cat-file <hash>` | Print the decompressed content of an object |
| `store add <file>` \| `store add .` | Stage a file, or everything not excluded by `.storeignore` |
| `store commit -m "<message>"` | Commit the staged snapshot |
| `store log` | Show commit history for the current branch (first-parent only, see [Known limitations](#known-limitations)) |
| `store branch [name]` | List branches, or create a new one from the current commit |
| `store checkout <branch>` | Switch branches and update the working directory |
| `store status` | Show untracked / modified / staged / deleted files |
| `store diff <file>` | Line-by-line diff of a file's HEAD version vs. its current working-directory content |
| `store merge <branch>` | Merge a branch into the current one — see below, this never conflicts |
| `store config [--name] [--email] [--remote-url]` | View or set author info and the remote server URL |
| `store push` | Push the current branch to the configured remote |
| `store pull` | Pull the current branch from the configured remote |

### Example session

```bash
store init
store config --name "Jane Doe" --email jane@example.com
echo "hello" > greeting.txt
store add .
store commit -m "Initial commit"
store branch feature
store checkout feature
echo "world" >> greeting.txt
store add .
store commit -m "Add world"
store checkout main
store merge feature
store log
```

## Object model

Everything is stored the same way Git does it, at a smaller scale:

- **Blobs** — raw file contents, prefixed with a `blob <size>\0` header, SHA-1 hashed, Deflate-compressed, and written to `.store/objects/<first 2 chars>/<remaining 38 chars>`.
- **Trees** — a directory snapshot: lines of `<mode>,<type>,<hash>,<name>`, hashed and stored the same way as blobs.
- **Commits** — `tree <hash>`, optional `parent <hash>`, optional `merge-parent <hash>` (for merge commits — see below), `author`, `timestamp`, a blank line, then the commit message.

Refs live under `.store/refs/heads/<branch>` (local branches) and `.store/refs/remotes/origin/<branch>` (the last commit this repo knows the remote had, updated by `push`/`pull`). `HEAD` is a symbolic ref (`ref: refs/heads/<branch>`), never a raw commit hash except in a detached state.

## Merging without conflicts

Git's merge conflicts happen when a real three-way merge can't automatically reconcile overlapping edits, and it asks a human to sort it out by hand. `store merge` sidesteps that entirely by never attempting a line-level reconciliation:

1. **Stash** — any uncommitted, locally-modified tracked file is set aside and the working copy reverted to the current branch's last commit.
2. **Bring the other branch in** — if the current branch's tip is an ancestor of the target's, it's a plain fast-forward (ref moves, tree checked out). Otherwise, the two branches' trees are unioned: files that exist on only one side are kept as-is, files identical on both sides collapse to one copy, and files that differ keep the *incoming* branch's content at the original path while the *current* branch's version is preserved alongside it as `name.local.ext`. This produces a real two-parent merge commit (`parent` + `merge-parent`).
3. **Pop the stash** — the same identical-collapses/different-gets-`.local`-suffix rule reapplies the stashed changes on top.

Nothing is ever silently discarded, and nothing ever blocks waiting for manual resolution — worst case, you end up with a `.local` sibling file that `store status` will show as untracked, for you to review and fold in by hand whenever you like.

## Remote (push/pull)

`StoreServerApplication` is a small Spring Boot app exposing:

- `POST /origin/push/{branch}/{commitHash}` — accepts a commit hash and the objects behind it, fast-forward-checks against what the remote already has, and moves the branch ref.
- `GET /origin/pull/{branch}?localCommitHash={hash}` — returns the remote's tip and every object the caller doesn't have yet (rejects if the caller has diverged, in which case `store merge` is the answer, not `pull`).

The `store push`/`store pull` CLI commands are the client half: they walk local history to figure out what's new, talk to the server over plain `java.net.http.HttpClient`, and update `.store/refs/remotes/origin/<branch>` on success. Point the client at a server with:

```bash
store config --remote-url http://localhost:8080
```

(defaults to `http://localhost:8080` if never set).

## `.storeignore`

Two pattern shapes are supported, one per line:

- `name/` — ignore a directory by name, anywhere in the tree (e.g. `target/`)
- `.ext*` — ignore files by extension (e.g. `.log*` ignores anything ending in `.log`)

Blank lines and lines starting with `#` are skipped. Bare filenames, `*.ext`-style globs, and `**` are not yet supported.

## Testing

```bash
mvn test
```

Unit tests cover the three areas most prone to subtle bugs, per the project roadmap: the content-addressable object store (`HashContentTest`), tree building (`TreeTest`), and the Myers diff algorithm (`DiffTest`). Filesystem-touching tests redirect `Constants.root` to a JUnit `@TempDir` for the duration of the test and restore it afterward, so running the suite never touches this repository's own `.store/`.

Higher-level flows — merge's stash/reconcile logic, the push/pull HTTP round-trip, checkout — are exercised manually rather than under JUnit; they'd need heavier fixtures (a running server, multi-commit repo scaffolding) to test in isolation.

## Project layout

```
src/main/java/
  init/         store init
  hash/         content-addressable object store (blob/tree/commit storage)
  tree/         tree objects: build from a directory or an index, read back
  index/        the staging area's data shape
  add/          store add, .storeignore handling
  commit/       commit objects, commit-graph ancestry walks
  log/          commit history traversal + (de)serialization
  branch/       branches and HEAD resolution
  checkout/     working directory checkout
  status/       working dir / index / HEAD comparison
  diff/         Myers diff algorithm
  merge/        the no-conflict merge described above
  config/       author info + remote URL, stored in .store/config.json
  push/ pull/   client-side remote sync
  remote/       server-side REST endpoints (Spring Boot) for push/pull
  common/       shared constants and small utilities
```

## Known limitations

- `store log` only follows a commit's first parent, so commits that arrived via a merge's second parent don't show up in the log output. A correct fix needs a full DAG traversal instead of a linear walk.
- `store diff` always compares a file's HEAD version against its current working-directory content — there's no separate staged-vs-working or arbitrary-commit-vs-commit mode yet.
- `store push`/`store pull` assume push is always fast-forward-safe on the server and pull's response is always fast-forward-safe on the client — both are true given the current server-side checks, but would need revisiting if that validation ever changes.
- No packfiles/delta compression, no rebase — both explicitly out of scope per the project roadmap.
