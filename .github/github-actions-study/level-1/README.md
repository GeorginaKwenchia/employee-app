# Level 1: Beginner

## Goal

Understand the basic structure of a GitHub Actions workflow and run a simple job.

## Concepts

- **Workflow:** A YAML automation file in `.github/workflows/`.
- **Event:** Something that starts a workflow, such as a push.
- **Job:** A group of related steps.
- **Runner:** The virtual machine that executes a job.
- **Step:** One task in a job.
- **Action:** Reusable code called with `uses`.
- **Shell command:** A command run with `run`.

## Read This Repository

Open `../../workflows/basic.yml`. Notice that `on` defines triggers, `jobs` defines work, `runs-on` chooses the runner, and `steps` lists tasks.

The workflow has two independent jobs. One prints a greeting. The other runs `env` to show runner variables in the log. Never print secrets in a real workflow.

## Example

```yaml
name: Beginner Example

on: workflow_dispatch

jobs:
  hello:
    runs-on: ubuntu-latest
    steps:
      - name: Say hello
        run: echo "Hello from Actions"
```

## Exercises

1. Change the greeting in `basic.yml`.
2. Add a step using `pwd` to print the current directory.
3. Add a step using `ls -la` to list files.
4. Print `$GITHUB_REF` and `$GITHUB_SHA`.
5. Run the workflow manually from the Actions tab.

## Checkpoint

Explain what starts the workflow, what a runner does, and how `uses` differs from `run`. Find a job's logs and identify each step.
