# Level 6: Advanced and Expert

## Goal

Design maintainable GitHub Actions systems for teams and many repositories.

## Concepts

- Reusable workflows with `workflow_call`.
- Composite actions for repeated step sequences.
- Dynamic matrices generated from project data.
- Self-hosted runners, runner groups, and larger runners.
- OIDC authentication instead of long-lived cloud keys.
- Deployment protection and rollback strategies.
- Monorepo path-based pipelines.
- Workflow duration, failure rate, and cost measurement.
- Organization standards, templates, and governance.

## Example Design

A mature pipeline commonly has this shape:

```text
pull request -> checks -> build artifact -> staging -> approval -> production
```

Each stage should have a clear owner, limited permissions, useful logs, and a recovery path.

## Exercises

1. Extract repeated setup into a reusable workflow.
2. Create a composite action for dependency installation.
3. Replace long-lived cloud credentials with OIDC.
4. Add a rollback path for a failed deployment.
5. Create a workflow template for a new service.
6. Measure the slowest jobs and reduce their runtime.

## Checkpoint

You can design a workflow from a delivery requirement, justify its triggers and permissions, debug failures from logs and event context, and improve speed or cost without making the workflow harder to maintain.
