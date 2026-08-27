# Level 3: Workflow Data and Control

## Goal

Control job execution and move information between steps and jobs.

## Concepts

- Environment variables can exist at workflow, job, or step scope.
- Variables hold non-sensitive configuration; secrets protect sensitive values.
- Expressions such as `${{ github.ref }}` read Actions context.
- `needs` creates a job dependency.
- `if` runs work only when a condition is true.
- Step and job outputs pass generated values forward.
- Artifacts preserve files after a run.
- A matrix repeats a job across versions or platforms.

## Example

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      tag: ${{ steps.version.outputs.tag }}
    steps:
      - id: version
        run: echo "tag=${GITHUB_SHA}" >> "$GITHUB_OUTPUT"

  show-tag:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Tag: ${{ needs.build.outputs.tag }}"
```

## Exercises

1. Pass an image tag from one job to another.
2. Upload a test report as an artifact.
3. Cache Python dependencies.
4. Test with a matrix of Python versions.
5. Add a manual input for a deployment target.

## Checkpoint

Predict when each job runs and explain how a value travels from one step to another job.
