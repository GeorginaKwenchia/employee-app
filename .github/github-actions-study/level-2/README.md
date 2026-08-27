# Level 2: Core CI

## Goal

Automatically test application code and report failures on every change.

## Concepts

- `actions/checkout` copies repository code to the runner.
- `actions/setup-python` installs a chosen Python version.
- Dependency installation prepares the project.
- A non-zero command exit code fails a step and usually the job.
- Branch and path filters control when work runs.
- Pull request checks give developers feedback before merging.

## Read This Repository

Open `../../workflows/test.yml`. Follow its order: checkout, set up Python, install requirements, and run `pytest` in `backend/`.

## Example

```yaml
- name: Install dependencies
  run: pip install -r backend/requirements.txt

- name: Run tests
  run: pytest -v
  working-directory: backend
```

## Exercises

1. Add a linting step after dependency installation.
2. Run the Python tests only when `backend/**` changes.
3. Add a Node.js job for `backend-node`.
4. Open a pull request and inspect the check summary.
5. Make a test fail, read the log, and repair it.

## Checkpoint

Every pull request gets a useful pass or fail result, and you can explain why a failed command stops later steps.
