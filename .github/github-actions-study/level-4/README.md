# Level 4: Build and Release

## Goal

Turn tested source code into traceable, deployable artifacts.

## Concepts

- Build artifacts are outputs such as packages, reports, or binaries.
- Docker workflows build and tag images.
- Commit SHA tags make an image traceable to source code.
- Registries store images for deployment.
- GitHub Releases publish versioned software.
- Environments can require approval before deployment.
- Build, test, and deploy jobs should have clear responsibilities.

## Read This Repository

Review `../../workflows/deploy-dev.yml` and `../../workflows/deploy-prod.yml`. Identify where images are built, pushed to ECR, and started on the target environment.

## Example

```yaml
- name: Build image
  run: docker build -t my-app:${{ github.sha }} .

- name: Save test report
  uses: actions/upload-artifact@v4
  with:
    name: test-report
    path: test-results/
```

## Exercises

1. Build the backend and frontend images after tests pass.
2. Tag an image with `${{ github.sha }}`.
3. Publish an image only from a protected branch.
4. Add a staging environment with approval.
5. Add a release step for a version tag.

## Checkpoint

A successful commit produces an artifact whose source version and deployment environment can be identified.
