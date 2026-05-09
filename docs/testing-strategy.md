# Testing Strategy

This project uses a staged quality gate so PRs can start getting protection immediately while historical debt is cleaned up.

## Test Layers

- Backend unit tests: JUnit 5, Mockito, AssertJ. Cover business rules, authentication, JWT/security context, and service decisions without a real database.
- Backend integration tests: Spring Boot Test, MockMvc, MyBatis mapper tests, and Testcontainers MySQL for SQL behavior that must match production.
- Frontend unit tests: Vitest, Vue Test Utils, jsdom. Cover stores, router guards, request handling, form rules, and SpreadJS license initialization.
- E2E smoke/regression: Playwright against a Docker Compose stack. Start with login and route protection, then expand to admin, enterprise, and auditor flows.
- Deployment smoke: after Railway or Tencent Cloud deployment, verify frontend load, login, core API availability, and SpreadJS license injection.

## Seed Data

The H2 dev profile already seeds anonymous users and domain data in `audit-web/src/main/resources/data-h2.sql`:

- `admin / admin123`
- `enterprise / admin123`
- `auditor / admin123`
- one sample enterprise, one published template, and energy/report support data

CI tests must use anonymous seed data or generated fixtures. Tencent Cloud production data and Railway data must not be copied into CI.

## PR Gate Policy

- Required immediately: backend compile, backend unit tests, frontend build, frontend unit tests, Playwright smoke.
- Advisory until debt is resolved: SQL migration dry-run, SQL lint, frontend typecheck, frontend lint, integration coverage thresholds.
- High-risk PRs must run relevant E2E/regression tests before merge: SQL migrations, SpreadJS/template changes, report generation, auth/permission changes, deployment and nginx/Docker changes.

## Regression Ownership

Every regression found in Railway or Tencent Cloud should be turned into a failing test first, then fixed. The new test should live at the lowest reliable layer: unit when possible, integration for API/SQL contracts, Playwright only for browser-visible workflows.
