# ADR-003 - Object Storage: MinIO Replacement Strategy

| Field | Value |
| --- | --- |
| Status | Accepted (interim) - target migration tracked |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| Supersedes | The MinIO selection from ADR-001 (Stack Selection) |
| References | NFR-OPS-003 (data location CH, no external CDN), NFR-SEC-001..004, arc42 §2.1, §3 |

## Context and Problem

ADR-001 selected **MinIO** as the self-hosted, S3-compatible object store for asset delivery, satisfying the "Switzerland-only, no hyperscaler, no external CDN" constraint (NFR-OPS-003).

In **April 2026**, MinIO Inc. **archived the public `minio/minio` GitHub repository** and migrated the project to a new commercial product, **AiStor**. Consequences:

- The last freely available open-source release is `RELEASE.2025-10-15T17-29-55Z`.
- No further security patches, CVE fixes, or feature releases will be published under the open-source AGPLv3 license.
- AiStor is a paid, source-restricted product and conflicts with our budget posture and AGPLv3 commitment for the asset layer.

We must therefore decide whether to (a) freeze on the last OSS MinIO release, (b) license AiStor, or (c) migrate to a maintained OSS S3-compatible alternative.

## Decision Drivers

- Switzerland-only hosting; self-hostable on a single-node compose deployment (NFR-OPS-003).
- AGPLv3 / Apache 2.0 / MPL preferred; no source-restricted licenses.
- S3-compatible API (signed URLs, presigned PUTs, lifecycle rules).
- Low operational burden; no Kubernetes prerequisite.
- Active upstream with security response.
- Migration path that does not block early development.

## Considered Options

- **Option A** - Pin to MinIO `RELEASE.2025-10-15T17-29-55Z` indefinitely.
- **Option B** - License AiStor.
- **Option C** - Migrate to a maintained OSS S3-compatible alternative (Garage, SeaweedFS, Ceph RGW).

## Decision

**Combined approach (a + c)**:

1. **Interim**: pin MinIO to `RELEASE.2025-10-15T17-29-55Z` for initial scaffolding and pilot environments, so development is not blocked. Track this pin in `compose.yaml`.
2. **Target**: migrate to **Garage** (Rust, AGPLv3, designed for self-hosted geo-distributed deployments) or **SeaweedFS** (Apache 2.0, broader feature set including filer/erasure coding) **before the first public release**. A short evaluation spike (1-2 days) will pick between the two.

**Option B (AiStor) is rejected** on license/cost grounds and conflict with the OSS-first stance.

## Consequences

- Positive:
  - Unblocks immediate development with a known, security-acceptable MinIO version.
  - Eliminates long-term lock-in and security exposure by setting a hard deadline (first public release) for migration off frozen MinIO.
  - Both Garage and SeaweedFS expose an S3-compatible API, so service code does not need a second major rewrite.
- Negative:
  - The interim MinIO pin will not receive further CVE patches; mitigated by network isolation (object store reachable only from backend, signed URLs to the browser) and short interim window.
  - A second migration step (MinIO → Garage/SeaweedFS) before launch.
- Follow-ups:
  - Run an evaluation spike on Garage vs SeaweedFS; record outcome in a follow-up ADR amendment or new ADR.
  - Update `compose.yaml` and integration tests (Testcontainers) to point to the chosen target before launch.
  - Update arc42 §3 deployment view once the target is selected.

## Rejected Options

- **Option A only** - leaves the asset layer permanently unpatched; unacceptable beyond pilot.
- **Option B (AiStor)** - paid, source-restricted, and conflicts with the OSS-first / Switzerland-only operating posture.

## Links

- ADR-001 (Stack Selection) - MinIO selection superseded by this ADR
- ADR-002 (Java 25 + version refresh)
- arc42 §2.1, §3
- MinIO repo archival announcement (Apr 2026)
- Garage: <https://garagehq.deuxfleurs.fr/>
- SeaweedFS: <https://github.com/seaweedfs/seaweedfs>
