# Level 5: Security and Reliability

## Goal

Make automation safe, predictable, and resilient.

## Concepts

- Grant `GITHUB_TOKEN` only the permissions a job needs.
- Store credentials in secrets and do not print them.
- Treat pull request code as untrusted when it can access secrets.
- Review and pin third-party actions to trusted versions.
- Use dependency review and security scanning.
- Use concurrency to cancel stale runs.
- Use timeouts and deliberate failure handling.
- Separate deployment credentials by environment.

## Example

```yaml
permissions:
  contents: read

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
```

## Exercises

1. Add explicit permissions to a workflow.
2. Add a dependency review job.
3. Cancel older runs for the same branch.
4. Add a timeout to a long-running job.
5. Review deployment workflows for unnecessary secrets and permissions.

## Checkpoint

Before merging a workflow, explain what it can access, what happens if it fails, and whether untrusted code can reach credentials.
