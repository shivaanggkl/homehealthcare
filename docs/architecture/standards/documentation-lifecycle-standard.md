# Documentation Lifecycle Standard

This standard defines the shared lifecycle and guardrail expectations for Epic 8 structured documentation and task completion.

## Documentation lifecycle

Epic 8 documentation uses these shared statuses:

- `DRAFT`
- `IN_PROGRESS`
- `SUBMITTED`
- `AMENDED`
- `LOCKED`

These states describe documentation readiness and mutability, not the broader visit, mobile execution, or EVV lifecycle.

## Guardrails

Epic 8 documentation must enforce these shared rules:

- one active documentation record per visit and selected template unless an explicit versioning workflow exists
- draft saves may persist incomplete fields and tasks
- submission must enforce all required fields and required tasks
- role-based field visibility and editability must be enforced consistently by backend rules
- attachment links must stay within the same visit, patient, and agency context
- printable summaries must exclude internal-only metadata and hidden fields
- cross-tenant and cross-branch documentation access is always blocked

## History expectations

Epic 8 workflows should preserve durable history for:

- template changes
- task library changes
- draft saves
- final submissions
- amendments
- attachment linkage changes
- printable summary generation
