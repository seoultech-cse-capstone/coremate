# Repository Instructions

## Commit Policy

- Whenever a unit of work is finished (a feature, a fix, a requested change — anything that leaves the tree in a coherent, working state), commit it before moving on. Don't wait to be asked each time.
- Group changes into logical commits (split unrelated changes, bundle tightly-coupled ones), same as usual. Verify with `tsc --noEmit` before committing when touching TypeScript.
- This does not authorize pushing — see Git Remote Policy below.

## Git Remote Policy

- Never run `git push` under any circumstance. The user pushes branches manually.
- Do not create, delete, or modify remote branches or tags.
- When the user asks to connect a branch to a remote, explain the required manual command instead of pushing.
