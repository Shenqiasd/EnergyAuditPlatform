# SpreadJS License Deployment

SpreadJS deployment licenses are environment-specific secrets. Do not commit real license values to Git.

## Required variables

Set both variables before building the frontend:

```bash
VITE_SPREADJS_LICENSE=<spreadjs-sheets-license>
VITE_SPREADJS_DESIGNER_LICENSE=<spreadjs-designer-license>
```

The values are read by `audit-ui/src/utils/spreadjs-license.ts` at build/runtime through Vite's `import.meta.env`.

## Railway

Add the two variables to the Railway service that builds `audit-ui`, then trigger a redeploy. Because Vite embeds `VITE_*` values during the frontend build, changing either variable requires a new frontend deployment.

## Tencent Cloud

Set the same variables in the environment used by the frontend build step. For Docker or CI-based builds, pass them as build-time environment variables. For direct server builds, keep them in an untracked local env file such as `audit-ui/.env.local`.

## Local development

Copy `audit-ui/.env.example` to `audit-ui/.env.local` and fill in the real values locally:

```bash
cp audit-ui/.env.example audit-ui/.env.local
```

`audit-ui/.env.local` is ignored by Git and must not be committed.
