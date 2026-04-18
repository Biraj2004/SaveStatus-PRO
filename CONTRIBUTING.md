# Contributing to SaveStatus PRO

Thanks for your interest in contributing to SaveStatus PRO.

This guide explains how to set up the project, propose changes, and submit pull requests that are easy to review and merge.

## Code of Conduct

Please be respectful and constructive in all project interactions.

## Getting Started

### Prerequisites

- Android Studio (latest stable recommended)
- JDK 17
- Android SDK 34
- Git

### Fork and Clone

1. Fork the repository
2. Clone your fork locally
3. Add upstream remote

```bash
git clone https://github.com/<your-username>/SaveStatus-PRO.git
cd SaveStatus-PRO
git remote add upstream https://github.com/Biraj2004/SaveStatus-PRO.git
```

### Build locally

```bat
gradlew.bat assembleDebug
```

Optional verification:

```bat
gradlew.bat :app:compileDebugKotlin
```

## Branching Model

- Create a branch from `main`
- Use clear branch names:
  - `feature/<short-description>`
  - `fix/<short-description>`
  - `docs/<short-description>`

Example:

```bash
git checkout -b fix/about-version-label
```

## Development Guidelines

- Keep changes focused and minimal
- Preserve existing architecture and naming conventions
- Avoid unrelated refactors in the same PR
- Update docs when behavior or workflow changes

### Android-specific notes

- Target SDK and dependencies are managed in Gradle files
- Do not commit local machine files or secrets:
  - `local.properties`
  - `keystore.properties`
  - keystore files (`.jks`, `.keystore`)

## Commit Guidelines

Use clear, imperative commit messages.

Examples:

- `feat: add update status badge in about screen`
- `fix: handle missing status files in image viewer`
- `docs: improve release steps in README`

## Pull Request Checklist

Before opening a PR, ensure:

- [ ] Project builds locally (`assembleDebug` or relevant task)
- [ ] Changes are scoped to one clear purpose
- [ ] README/docs updated if needed
- [ ] No secrets, keystore files, or build artifacts included
- [ ] PR description explains what changed and why

## How to Submit a Pull Request

1. Push your branch
2. Open a pull request against `main`
3. Use a clear PR title and description
4. Include screenshots for UI changes
5. Link related issues when applicable

## Reporting Bugs

When reporting a bug, include:

- Device and Android version
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs or screenshots

## Suggesting Features

For feature requests, include:

- Problem statement
- Proposed solution
- Alternative options considered
- Any UI mockups or flow notes (if applicable)

## Security Issues

Do not open public issues for security vulnerabilities.

Please follow the private reporting instructions in [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contributions will be licensed under the MIT License in [LICENSE](LICENSE).
