# Building a Git-like VCS in Java — Step-by-Step Roadmap

No frameworks required. Plain Java + standard library (`java.security`, `java.util.zip`, `java.nio.file`, `java.io`). Optionally Picocli for CLI parsing later.

---

## Phase 0 — Project skeleton

**Goal:** A runnable Java project with a CLI entry point that does nothing yet.

- Set up a Maven or Gradle project (either is fine; Maven is a bit more common for tutorials).
- Create a `Main` class with a `main(String[] args)` that just prints the first argument (`args[0]`).
- Decide your repo folder name, e.g. `.mygit` (to avoid clashing with real `.git`).

**Done when:** `java -jar mygit.jar hello` prints `hello`.

---

## Phase 1 — Content-addressable object storage

**Goal:** Store and retrieve arbitrary content by its hash — the foundation everything else builds on.

- Write a function `String hashObject(byte[] content, String type)` that:
    1. Prepends a header like `"blob <size>\0"` to the content (mimic Git's format).
    2. Hashes the result with `MessageDigest.getInstance("SHA-1")`.
    3. Compresses it with `Deflater`.
    4. Writes it to `.mygit/objects/<first 2 chars>/<remaining 38 chars>`.
- Write the inverse: `byte[] readObject(String hash)` — locate the file, `Inflater` to decompress, strip the header, return content.

**Done when:** You can hash a string, write it as an object, read it back, and get identical bytes.

---

## Phase 2 — Repository initialization (`init`)

**Goal:** `mygit init` creates the directory structure.

```
.mygit/
  objects/
  refs/heads/
  HEAD          → contains "ref: refs/heads/main"
```

**Done when:** Running `init` twice doesn't crash (idempotent or gives a clear "already initialized" message).

---

## Phase 3 — Plumbing commands: `hash-object` and `cat-file`

**Goal:** Expose Phase 1 as CLI commands — invaluable for debugging everything after this.

- `mygit hash-object <file>` → hashes and stores the file, prints the hash.
- `mygit cat-file <hash>` → prints the decompressed content of an object.

**Done when:** You can round-trip any file through these two commands from the terminal.

---

## Phase 4 — Tree objects

**Goal:** Represent a directory snapshot, not just single files.

- Design a tree entry format: `<mode> <type> <hash> <name>` per line (mode: file vs directory).
- Write `String writeTree(File directory)`:
    - Recursively walk the directory.
    - For each file → store as a blob, record its hash.
    - For each subdirectory → recursively write its tree, record that tree's hash.
    - Serialize all entries, hash and store as a `tree` object.

**Done when:** `writeTree()` on a folder with nested subfolders produces a single hash representing the whole structure, and you can walk it back into a file listing.

---

## Phase 5 — The staging area (index)

**Goal:** Decouple "what's in the working directory" from "what will be committed."

- Design an index format — simplest option: a text or JSON file mapping `path → blob hash`.
- `mygit add <file>` → hashes the file as a blob, adds/updates the entry in the index.
- `mygit add .` → adds all tracked/new files.
- Support removing a path from the index (for a future `rm`/`reset`).

**Done when:** You can `add` a few files and print out the index contents showing correct paths and hashes.

---

## Phase 6 — Commit objects and the `commit` command

**Goal:** Turn a staged snapshot into permanent history.

- Build a tree from the current index (reuse Phase 4 logic, but driven from the index rather than walking disk directly).
- Serialize a commit object: `tree <hash>`, `parent <hash>` (if any), `author`, `timestamp`, blank line, `message`.
- Hash and store it like any other object.
- Update the current branch ref (`refs/heads/main`) to point to the new commit hash.

**Done when:** `mygit commit -m "message"` creates a commit object and moves the branch pointer forward.

---

## Phase 7 — `log` and graph traversal

**Goal:** Walk the commit DAG and print history.

- Starting from the current branch's commit hash, follow `parent` pointers backward.
- Print each commit's hash, message, author, timestamp.

**Done when:** `mygit log` shows your commits in reverse chronological order.

---

## Phase 8 — Branches and refs

**Goal:** Multiple lines of development.

- `mygit branch <name>` → create a new file in `refs/heads/` pointing at the current commit.
- `mygit branch` (no args) → list branches, marking the current one.
- Understand `HEAD` as a symbolic ref: it points to a branch file, not directly to a commit (except in "detached HEAD" state).

**Done when:** You can create branches and confirm each one independently tracks its own tip commit.

---

## Phase 9 — `checkout`

**Goal:** Move between commits/branches and update the working directory to match.

- Given a target commit, read its tree, and recursively write out blobs as real files, replacing the working directory contents.
- Update `HEAD` to point at the new branch (or the commit directly, for detached HEAD).
- Handle the tricky part: removing files that exist in the old tree but not the new one.

**Done when:** Switching branches actually changes the files on disk correctly, both adding and removing files as needed.

---

## Phase 10 — `diff` (Myers algorithm)

**Goal:** Show line-by-line differences between two blobs.

- Split file contents into lines.
- Implement the Myers diff algorithm (or use the simpler O(ND) LCS-based version) to compute the minimal edit script.
- Print with `+`/`-` prefixes like Git does.

**Done when:** Diffing two versions of a text file produces sensible, minimal output — not a full rewrite for a one-line change.

---

## Phase 11 — `status`

**Goal:** Compare working directory, index, and HEAD commit to report state.

- Files in working dir but not index → "untracked."
- Files in index but changed on disk → "modified, not staged."
- Files in index but different from HEAD's tree → "staged for commit."

**Done when:** `status` correctly classifies a mix of new, modified, and unchanged files.

---

## Phase 12 — Merging

**Goal:** Combine two branches.

- Find the **merge base** (lowest common ancestor) by walking both branches' histories and finding the first shared commit.
- Fast-forward case: if one branch's tip is an ancestor of the other, just move the pointer — no real merge needed.
- Three-way merge case: for each file, compare base/ours/theirs line-by-line; auto-merge non-overlapping changes, and insert `<<<<<<<` / `=======` / `>>>>>>>` conflict markers for overlapping ones.
- Create a merge commit with two parents.

**Done when:** Merging a branch with non-conflicting changes succeeds cleanly, and a genuine conflict produces correct markers in the file.

---

## Phase 13 — Ignore patterns

**Goal:** Respect a `.mygitignore` file.

- Implement glob matching (`*`, `**`, `!negation`) against relative paths.
- Apply it in `add` (skip matched files) and `status` (don't report them as untracked).

**Done when:** Ignored files never show up as untracked or get staged, even with `add .`.

---

## Phase 14 — CLI polish

**Goal:** Make it pleasant to use.

- Swap hand-rolled `switch(args[0])` parsing for a small library like Picocli if you want proper `--flag` support and auto-generated help text.
- Add meaningful error messages (e.g. "not a mygit repository," "nothing to commit").
- Write unit tests for the object store, tree building, and diff logic — these are the parts most prone to subtle bugs.

**Done when:** Someone unfamiliar with the code can run `--help` and figure out the basic commands.

---

## Phase 15 — Advanced / optional

These are genuinely hard and not required for a working VCS — attempt only if you want to go deeper:

- **Packfiles & delta compression**: bundle loose objects together, storing many as diffs against a base object to save space (this is what `git gc` does).
- **Remote push/pull**: implement Git's smart HTTP protocol or a simple socket-based equivalent to sync with another repository.
- **Rebase**: replay commits from one branch onto another, which is really "cherry-pick in a loop" plus history rewriting.

---

## Suggested pacing

If you're doing this as a learning project, each phase above is roughly a weekend's worth of focused work. Phases 1–7 give you a genuinely functional "init → add → commit → log" tool, which is a great milestone to pause at and feel good about before tackling branching and merging.