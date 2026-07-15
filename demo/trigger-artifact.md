```
!sentry_investigation

*Error:* AuthorizationError: user 4471 accessed claim 90233 belonging to user 5583
*Location:* `com.aegis.claims.web.ClaimDetailController — getClaim`
*Type:* Broken Access Control (CWE-639)
*Service:* aegis-claims-api

Level: error | Env: prod | Endpoint: GET /claims/{id}
Signal: cross-account claim read detected in access logs; billing page p95 latency
also breached SLO (N+1 query on /claims and /billing).

A production authorization failure was detected on the claim-detail endpoint.
Investigate the root cause in the controller/service above, confirm whether other
users' claims are exposed, fix the access-control gap and the related N+1 query,
add regression tests, and open a PR with the fix.
```
