# Assignment creation

The important distinction is between an `Assignment` and an Entra membership:

- An assignment says that a resource is assigned directly to a user, to a role, or to a device group.
- A flattened assignment says that one concrete user/device should get that resource because of that assignment.
- A user/device Entra membership is the unique `(user/device Entra id, resource Entra group id)` pair which is sent to the Graph GW.

More than one assignment can lead to the same Entra membership. In that case we keep one membership and do not remove it until the last active flattened assignment is gone.

## Assignment creation

`POST /api/assignments` is used for all three assignment types.

```java
public class NewAssignmentRequest {
    @NotNull
    Long resourceRef;
    Long userRef;
    Long roleRef;
    Long deviceGroupRef;
    @NotBlank
    String organizationUnitId;

    @AssertTrue(message = "Exactly one of userRef, roleRef or deviceGroupRef must be set")
    public boolean isExactlyOneAssigneeRefSet() {
        int assigneeCount = 0;
        assigneeCount += userRef == null ? 0 : 1;
        assigneeCount += roleRef == null ? 0 : 1;
        assigneeCount += deviceGroupRef == null ? 0 : 1;
        return assigneeCount == 1;
    }
}
```

- `resourceRef` and `organizationUnitId` are required.
- Exactly one of `userRef`, `roleRef` and `deviceGroupRef` must be set. This is checked both by request validation and by a database check constraint.
- `organizationUnitId` is the org unit used when looking up the nearest application-resource location. It is not necessarily the user's or device group's own org unit.


### Common resource validation

Before an assignment is saved, the resource must exist and:

- have status `ACTIVE`
- have `identityProviderGroupObjectId` set

If either requirement is not met, the API returns `422 Unprocessable Entity`. A missing resource returns `404`.

The Entra group id from the resource is copied to `Assignment.entraGroupId`. The resource name, the authenticated assigner's username, and the nearest application-resource location are also copied when available.

The nearest location is looked up from the resource and the request's `organizationUnitId`. Creation does not fail when no location is found. The assignment is still saved, but its `applicationResourceLocationOrgUnitId` is empty.

### Direct user assignment

For `userRef`:

- the user must exist
- the user must have status `ACTIVE`
- the user must have `identityProviderUserObjectId` set
- there must not already be an active assignment for the same `(resourceRef, userRef)` pair

The assignment stores a snapshot of the user's Entra id, name and user type. One flattened user assignment is then created and linked to one `UserEntraMembership` for `(user Entra id, resource Entra group id)`.

The membership starts as:

```text
membershipStatus = ACTIVE
entraStatus      = NOT_SENT
```

It is immediately published as an `ADD` command after the transaction commits.

#### User update
When a user is updated, the stored user is updated as well. If the user becomes inactive, all active direct assignments for that user are deactivated.
If the user's Entra id changes, existing assignments and flattened assignments are updated with the new id. The local membership is updated or merged into an existing membership for the new Entra id, and the new membership is published as an ADD command. No explicit REMOVE is sent for the old id: the update contract assumes that the old Entra object no longer exists. Role-membership records for the user are also updated with the new Entra id. Counts for affected resources are recalculated after the migration.

#### User deletion
When a user is deleted, all active direct assignments for that user are deactivated before the user is removed from the local database.
This terminates the related flattened assignments. A REMOVE command is sent for each user/resource Entra membership only when the user has no other active flattened assignment for the same resource, for example through a role assignment.

### Role assignment

For `roleRef`:

- the role must exist
- the role must have status `ACTIVE`
- there must not already be an active assignment for the same `(resourceRef, roleRef)` pair

The role assignment is expanded to all active `role-catalog-membership` entries for that role which have an Entra user id. There is one flattened user assignment per active member. Each flattened assignment is linked to the deduplicated `UserEntraMembership` for that user and resource group.

If the role currently has no matching active members, the top-level assignment is still created. Flattened assignments and Entra commands are created later when matching membership messages arrive.

Both status checks are enforced by the API, not delegated to the frontend. A direct assignment to an inactive user or an assignment to an inactive role returns `422 Unprocessable Entity`.

### Device-group assignment

For `deviceGroupRef`:

- the device group must exist
- there must not already be an active assignment for the same `(resourceRef, deviceGroupRef)` pair

The assignment is expanded to active `DeviceGroupMembership` records. There is one flattened device assignment per device in the group. The flattened assignment is linked to the deduplicated `DeviceEntraMembership` for `(device Entra id, resource Entra group id)` and is immediately published as an `ADD` command after commit.

If the device group has no active members, the top-level assignment is still created. A later active device-group membership creates the flattened assignment and sends the command.

#### Updates to devices and device memberships
When a new device-group membership is received, the device and device group must already exist. The membership is stored, and if its status is ACTIVE, all active assignments for that device group are found. A flattened device assignment is created for the device for each matching resource assignment, linked to its (device Entra id, resource Entra group id) membership, and published as an ADD command.

If an existing device-group membership changes from ACTIVE to another status, the related flattened device assignments are terminated. A REMOVE command is sent only if the device has no other active flattened assignment for the same resource.

A device update with status DISABLED, INACTIVE, or similar does not currently deactivate assignments or send a REMOVE command. The device is just updated locally. Only a change to its dataObjectId causes existing device memberships to be updated and republished; as with user Entra-id changes, the old Entra object is assumed no longer to exist. A Kafka tombstone for the device does deactivate and remove its flattened assignments.

## What changes an existing assignment

Assignments are soft deleted. `DELETE /api/assignments/{id}` sets `assignmentRemovedDate`; it does not delete the row.

- Removing a direct user assignment terminates its flattened assignment.
- Removing a role assignment terminates every flattened user assignment created from that role assignment.
- Removing a device-group assignment terminates every flattened device assignment created from that group assignment.
- A role/user/resource becoming inactive also deactivates affected assignments.
- An inactive role membership or device-group membership only terminates the affected flattened assignment. It does not remove the top-level role/device-group assignment.

When a flattened assignment is terminated, the service checks whether another active flattened assignment is linked to the same user/device Entra membership. A `REMOVE` command is only sent if there is no remaining active source for that membership.


## License counting and hard stop

License counting is based on desired active Entra memberships, not on the number of top-level assignments.

- The resource total is the number of `UserEntraMembership` rows with `membershipStatus = ACTIVE` plus the number of active `DeviceEntraMembership` rows for the resource group.
- The count for an application-resource location is the number of distinct active flattened user memberships at that location plus distinct active flattened device memberships at that location.
- The same user/device appearing through several roles/groups only counts once for a resource, because the Entra-membership table is unique on the user/device and resource Entra ids.
- Counts are recalculated when flattened assignments are created or terminated, when role/device-group changes affect an assignment, after a user Entra-id migration, and by the manual reconciliation endpoint.

`POST /api/assignments/update-assigned-resources-usage` recalculates every resource in batches of 500. The equivalent device maintenance endpoint is `POST /api/device-assignments/update-assigned-resources-usage`.

Hard stop is active only when both conditions are true:

```text
fint.kontroll.assignment-catalog.license-enforcement.hardstop-enable = true
resource.licenseEnforcement = HARDSTOP
```

Before saving an assignment, the service locks the resource and its application-resource locations, calculates the current counts, and adds the projected new memberships.

- A direct user assignment adds `1` only when the user is not already active in the resource Entra group.
- A role assignment adds the active members of the role that do not already have an active membership in the resource Entra group.
- A device-group assignment does the same for active devices in the device group.
- The request is rejected with `409 Conflict` when the projected resource total exceeds `resourceLimit`, or a resolved location total exceeds that location's `resourceLimit`.
- A `null` limit is treated as `0` for a hard-stop resource/location.
- If no nearest application-resource location was found during creation, there is no location count to project against. In that case only the resource total can reject the request.

For resources without `HARDSTOP`, creation is allowed and the count is recalculated afterwards.

Hard stop is enforced when a top-level assignment is created. It is intentionally not enforced when a later role-membership or device-group-membership message expands an existing assignment: those messages are processed so that the local catalog remains complete. Reconciliation persists the resulting observed counts and logs an `ERROR` if a hard-stop resource or location is over capacity.

## Kafka communication


### Input topics used by assignment processing

All entity topics below are consumed as keyed records. `null` values are tombstones where the consumer supports them.

| Resource/event name | Message contains | What happens here |
| --- | --- | --- |
| `resource-group` | Resource id, name, status, Entra group id, enforcement type and limits | Stores resources used by assignment creation. Resources without an Entra group id are skipped. |
| `kontrolluser` | User id, username, name, user type, org unit, status and `identityProviderUserObjectId` | Stores users. A `DELETED` user deactivates direct assignments before deletion. |
| `role-catalog-role` | Role id, name/type, org unit, status and member count | Stores roles. A status change to inactive deactivates role assignments. |
| `role-catalog-membership` | Membership id, `roleId`, `memberId`, `identityProviderUserObjectId`, `memberStatus` and change timestamp | Creates/terminates flattened user assignments for role assignments. |
| `applicationresourcelocation-extended` | Location id, resource id/ref, org unit, resource limit and assigned count | Used to resolve the nearest location and apply location limits. |
| `orgunitdistance` | `id`, `orgUnitId`, `subOrgUnitId`, `distance` | Supports nearest application-resource-location lookup. |
| `kontroll-device` | Device id, `dataObjectId` (Entra device id), platform/type, status and flags | Stores devices. A tombstone terminates device flattened assignments and deletes the device. |
| `kontroll-device-group` | Device-group id, name, org unit, platform/type and member count | Stores device groups. |
| `kontroll-device-group-membership` | `deviceId`, `deviceGroupId`, `membershipStatus`, `membershipStatusChanged` | Creates/terminates flattened device assignments for device-group assignments. |

### Assumptions about incoming Kafka data

- `kontroll-device-group-membership` uses a status update to invalidate a membership. Tombstones on this topic are ignored, so a tombstone does not terminate a device's flattened assignments or send a `REMOVE` command.
- `role-catalog-membership` is the source of truth for role access. When a user becomes inactive, the catalog only deactivates that user's direct assignments. Role-based flattened assignments remain active until role catalog sends an inactive membership update or a tombstone for that membership.
- A role-membership record's `roleId`, `memberId`, and `identityProviderUserObjectId` are immutable for its membership id. A change is represented as a tombstone for the old record and a new membership record, rather than an in-place update.
- A resource's `identityProviderGroupObjectId` is treated as immutable after assignments exist. If it changed, existing assignments and memberships would still point to the old Entra group; this application does not migrate, terminate, or recount them as part of the resource update.


### Commands sent to the Graph integration

User membership commands are sent to event `resource-group-membership-user`. Device membership commands are sent to event `resource-group-membership-device`.

The Kafka key is generated for every command as:

```text
<current epoch milliseconds>-<six random alphanumeric characters>
```

Commands are scheduled after the surrounding database transaction commits. The membership row is marked `SENT` or `DELETION_SENT` inside that transaction, but there is no transactional outbox: a process failure after commit and before Kafka accepts the command is recovered by reconciliation rather than atomically with the database write.

```json
// resource-group-membership-user
{
  "operation": "ADD",
  "entraGroupRef": "22222222-2222-2222-2222-222222222222",
  "entraUserRef": "11111111-1111-1111-1111-111111111111"
}
```


```json
// resource-group-membership-device
{
  "operation": "ADD",
  "entraGroupRef": "22222222-2222-2222-2222-222222222222",
  "entraDeviceRef": "33333333-3333-3333-3333-333333333333"
}
```

`operation` is either `ADD` or `REMOVE`.

The outgoing command does not contain assignment ids, resource refs, org units, names or license counts. The Graph integration only needs the target group and the user/device Entra id.

### Results received from the Graph integration

User results are consumed from event `graph-user-group-membership`:

```json
{
  "code": "ADDED",
  "entraGroupRef": "22222222-2222-2222-2222-222222222222",
  "entraUserRef": "11111111-1111-1111-1111-111111111111"
}
```

Device results are consumed from event `graph-device-group-membership`:

```json
{
  "code": "ADDED",
  "entraGroupRef": "22222222-2222-2222-2222-222222222222",
  "entraDeviceRef": "33333333-3333-3333-3333-333333333333"
}
```

`code` can be `ADDED`, `REMOVED`, `NO_CHANGES`, `FAILED` or `ERROR`.

| Graph result | Stored Entra sync status |
| --- | --- |
| `ADDED` while the local membership is active | `MEMBERSHIP_CONFIRMED` |
| `REMOVED` while the local membership is inactive | `DELETION_CONFIRMED` |
| `NO_CHANGES` | Confirmed as active/inactive based on the current local membership status |
| `FAILED` | `NEEDS_REPUBLISH` |
| `ERROR` | `ERROR` |

If Graph confirms the opposite of what the local membership currently requires, the service immediately republishes the current state. For example, a late `ADDED` result after the membership was made inactive causes a `REMOVE` command.


## Scheduled jobs

Scheduling is enabled in the application. The jobs currently in the code are:

| Job | When | What it does |
| --- | --- | --- |
| Reconcile stale user Entra memberships | Initial delay: 2 hours. Fixed delay: 24 hours after the previous run finishes. | Immediately recovers memberships with no recorded command; otherwise retries an unconfirmed ADD or REMOVE after the configured 72-hour grace period. |
| Reconcile stale device Entra memberships | Initial delay: 2 hours. Fixed delay: 24 hours after the previous run finishes. | Uses the same immediate-recovery and state-aware 72-hour retry policy. |
| Publish flattened user assignment report | Every day at midnight, `Europe/Oslo`. | Streams the flattened user-assignment report to the reporting topic. |

The reconciliation jobs work on the unique Entra membership records, not on flattened assignments. This ensures that one user/resource or device/resource membership produces at most one retry command per eligible run, even if several flattened assignments refer to it.

### Reconciliation policy

`fint.kontroll.assignment-catalog.reconciliation` in `application.yaml` controls both jobs. Its defaults are:

| Setting | Default | Meaning |
| --- | --- | --- |
| `initial-delay` | `2` hours | Wait after application startup before the first reconciliation run. |
| `fixed-delay` | `24` hours | Wait after a run has completed before starting the next run. |
| `response-timeout` | `72` hours | Time to wait for Entra's response before retrying a command that was recorded as sent. |

All values use `TimeUnit.HOURS`. The 72-hour timeout is deliberately longer than the daily schedule: a delayed downstream service is not retried on every run.

| Membership state | Eligible when | Command republished | Why |
| --- | --- | --- | --- |
| `SENT` and membership is `ACTIVE` | `sentToEntraAt` is older than the response timeout | ADD | An ADD was sent but confirmation has not arrived. |
| `DELETION_SENT` and membership is `INACTIVE` | `deletionSentToEntraAt` is older than the response timeout | REMOVE | A REMOVE was sent but confirmation has not arrived. |
| `NEEDS_REPUBLISH` and membership is `ACTIVE` | The most recent ADD attempt is older than the response timeout | ADD | Entra returned `FAILED` for the ADD, or another retryable failure was recorded. |
| `NEEDS_REPUBLISH` and membership is `INACTIVE` | The most recent REMOVE attempt is older than the response timeout | REMOVE | Entra returned `FAILED` for the REMOVE, or another retryable failure was recorded. |
| `MEMBERSHIP_CONFIRMED` or `DELETION_CONFIRMED` | Never | None | The desired state was confirmed. |
| `ERROR` | Never automatically | None | A terminal error needs explicit investigation or a forced/manual republish. |
| `NOT_SENT` or `TO_BE_DELETED` | Immediately, on the next reconciliation run | ADD or REMOVE, matching the desired membership state | No command has been recorded. Normal creation publishes it first; reconciliation is the recovery path, including after the V40 backfill. |


After a retry is published, its matching sent timestamp is updated. The membership therefore becomes ineligible until both the response timeout and the next daily job run have passed. With the defaults, a command sent at Monday 10:00 that receives `FAILED` will not be retried before Thursday 10:00; in practice it is retried by the first daily reconciliation run after that time, no later than roughly 24 hours later.

The same rules apply to user and device memberships. Each job pages through memberships in batches of 500, so a large retry backlog does not load all eligible records into memory at once.


## New constraints in this branch compared with `main`

This branch adds device assignment support and also hardens the existing user/role path.

### Request and application constraints

- `deviceGroupRef` is a new possible assignment target.
- `organizationUnitId` changed from `@NotNull` to `@NotBlank`.
- Exactly one target is required instead of relying on the caller to choose a user or role.
- Resource assignment validation is now shared by user, role and device-group creation: the resource must be active and have an Entra group id.
- Direct user creation requires an active user with an Entra user id; role creation requires an active role.
- Active duplicates are rejected by the application before persistence for user/resource, role/resource and device-group/resource pairs.

### Database constraints and data cleanup

| Migration | New rule |
| --- | --- |
| `V37` | At most one active assignment for a `(resource_ref, device_group_ref)` pair. |
| `V38` | `assignment_resources.number_of_resources_assigned` is backfilled to `0`, defaulted to `0`, and made `NOT NULL`. |
| `V40` | Renames user flattened assignments, backfills `user_entra_memberships`, links every migrated flattened assignment to its membership, and requires resource Entra group ids. |
| `V41` | `assignment_roles.role_status` is backfilled to `ACTIVE` and made `NOT NULL`. |
| `V42` | `assignments` must have exactly one of `user_ref`, `role_ref` or `device_group_ref`; users require `identity_provider_user_object_id`; active user/resource and role/resource pairs are unique. |
| `V43` | User flattened assignments require user Entra id, resource Entra group id and a linked `user_entra_membership`; users require a status. Invalid legacy flattened rows are removed before enforcing this. |
| `V44` | Assignments require `resource_ref` and `entra_group_id`; user flattened assignments require `resource_ref` and resource Entra group id. Invalid legacy assignments/projections are removed before enforcing this. |
| `V45` | Adds indexes used by the paged stale-membership reconciliation queries for users and devices. |

The `V42` exactly-one-target check is added as `NOT VALID`. PostgreSQL enforces it for rows written after the migration, but existing rows are not scanned and validated by that migration.

### Migration to Entra membership tables

The schema separates the desired Entra membership from the flattened assignments that caused it. A flattened assignment is evidence of an assignment; the membership table is the one record that represents the user/resource or device/resource command sent to Entra.

`V35` creates the device tables for the new device-assignment feature. It creates an empty `device_entra_memberships` table and `flattened_device_assignments`; there is no legacy device membership data to backfill.

`V40` migrates the existing user model as follows:

1. It removes legacy resources without an Entra group id and their dependent assignments/projections, because those rows cannot produce a valid Entra membership command.

2. It renames `flattened_assignments` to `flattened_user_assignments`.

3. It creates one `user_entra_memberships` row per distinct `(identity_provider_user_object_id, identity_provider_group_object_id)` pair. The unique constraint prevents several flattened assignments for the same user and resource from creating duplicate Entra memberships.

4. For each pair, it derives the desired membership state. If any source flattened assignment is active, the membership is `ACTIVE`; otherwise it is `INACTIVE`.

5. It derives the initial synchronization state from the legacy confirmation flags:

   - active and every active source confirmed: `MEMBERSHIP_CONFIRMED`;
   - active but not fully confirmed: `NOT_SENT`;
   - inactive and every source removal confirmed: `DELETION_CONFIRMED`;
   - otherwise: `TO_BE_DELETED`.

6. It adds `user_entra_membership_id` to every flattened user assignment and fills it from the matching Entra user/resource pair. The old per-flattened-assignment confirmation columns are then removed.

The backfill does not invent historical command timestamps: `sent_to_entra_at` and `deletion_sent_to_entra_at` begin as `NULL`. `V43` subsequently removes any flattened user rows that cannot be linked to a valid membership and makes the link and the required Entra IDs `NOT NULL`.

Migrated `NOT_SENT` and `TO_BE_DELETED` memberships are deliberately eligible for the reconciliation job's first run. It publishes the corresponding initial ADD or REMOVE command once; afterwards the record becomes `SENT` or `DELETION_SENT`, and any further automatic retry observes the configured response timeout. This prevents the migration from leaving previously unconfirmed additions or removals stranded while still avoiding repeated commands during downstream lag.

### New persistence model

- `assignments.device_group_ref` points to `device_groups`.
- `flattened_device_assignments` represents individual devices which should receive a resource through a device group.
- `device_entra_memberships` has a unique `(device_entra_id, resource_entra_id)` pair, matching the deduplication model already used for user memberships.
- `flattened_assignments` is renamed to `flattened_user_assignments`.
- `user_entra_memberships` stores the user/resource-group membership status separately from its flattened source assignments.
