# ADR 0002: Internal Super Admin Model

## Status

Accepted

## Story

`E1-S05 — Define internal super admin model`

## Decision

The platform uses a dedicated `InternalSuperAdmin` model for product-operator access.

`InternalSuperAdmin`:

- exists outside agency tenant membership
- is stored in a global platform table
- does not carry `agency_id`
- may provision agencies and bootstrap the first agency owner
- must use audited provisioning flows

## Provisioning flow

Agency provisioning is performed through `AgencyProvisioningService`.

The flow:

1. requires an active `InternalSuperAdmin` actor
2. creates the `Agency`
3. creates an `AgencyOwnerBootstrap` record for the first owner
4. writes audit events for both actions in the same transaction

## Audit rule

Internal operator actions must always create explicit audit records.

Current audited events:

- `AGENCY_CREATED`
- `OWNER_BOOTSTRAPPED`

## Consequence

The platform operator model is separated from tenant membership while still preserving traceability for support and provisioning actions.
