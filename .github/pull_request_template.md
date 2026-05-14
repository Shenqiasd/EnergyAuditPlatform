## Summary

- 

## Test Plan

- [ ] Backend unit tests: `mvn -B -ntp test`
- [ ] Backend integration tests if DB/API behavior changed: `mvn -B -ntp verify -P integration-test`
- [ ] Frontend checks if UI changed: `cd audit-ui && npm run typecheck && npm run lint && npm run test:unit && npm run build`
- [ ] E2E/regression if user flows changed: `cd audit-ui && npm run test:e2e`
- [ ] Post-deploy smoke if deployment/config changed

## Regression Risk

- Affected roles: Admin / Auditor / Enterprise / Public
- Affected areas: Auth / Enterprise settings / Template & SpreadJS / Reports / Audit tasks / SQL migration / Deployment
- Backward compatibility notes:

## Secrets & Deployment

- [ ] No real secrets, SpreadJS licenses, database dumps, or certificates are committed.
- [ ] Required Railway/Tencent Cloud variables are documented or already present in the target environment.
