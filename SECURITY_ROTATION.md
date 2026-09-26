# Security rotation runbook

The repository previously contained a Firebase Admin service-account JSON in Git history. Treat that key, and any Spotify or Redis credentials that may have appeared in remote Docker build context, as compromised.

Before the next production deployment, an operator with access to Firebase, Spotify, Redis, and the repository host must:

1. Revoke/delete the exposed Firebase service-account key and issue a replacement. Store the replacement only in the deployment secret `FIREBASE_SERVICE_ACCOUNT_JSON`.
2. Rotate the Spotify client secret and Redis password if either was ever present in a remote build context or logs.
3. Audit CI, Render, Docker, and application logs for the old values and invalidate any cached artifacts containing them.
4. Coordinate a maintenance window, then rewrite repository history with a vetted secret-removal tool (for example, `git filter-repo`), verify with a secret scanner, and force-push the cleaned refs.
5. Notify anyone with an existing clone that their clone must be recloned or repaired; old objects can retain the credential even after the branch is rewritten.

Do not put service-account files in the Docker build context. `.dockerignore` excludes known credential names, and the container reads deployment secrets at runtime.
