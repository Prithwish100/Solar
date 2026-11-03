# SOLAR Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Authentication Mandatory**: Unauthenticated users cannot read or write any document in `/users/{userId}` or its subcollections (`liked_songs`, `playlists`, `listening_history`).
2. **Strict Tenant Isolation**: A user with `request.auth.uid` can only access `/users/{userId}` and subcollections where `userId == request.auth.uid` and `data.userId == request.auth.uid`.
3. **Immutable Audit Fields**: `userId` and `createdAt` cannot be modified during `update` operations.
4. **Server Timestamps**: `createdAt` and `updatedAt` must be valid Firestore `timestamp` objects `<= request.time`.
5. **Strict Schema & Bounds**: Every collection enforces `hasAll` and `hasOnly` key allowlists on `create`, `affectedKeys().hasOnly(...)` on `update`, and bounded string/int validation via `isValid[Entity]()`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Profile Read**: `GET /users/alice_123` with `auth = null`.
2. **Cross-User Liked Song Read**: `GET /users/bob_456/liked_songs/song_1` as `alice_123`.
3. **Cross-User Playlist Write**: `CREATE /users/bob_456/playlists/pl_1` as `alice_123`.
4. **Identity Spoofing on Create**: `CREATE /users/alice_123/liked_songs/song_1` as `alice_123` with `{"userId": "bob_456"}`.
5. **Ghost Field Injection (Shadow Update)**: `UPDATE /users/alice_123` adding `"isAdmin": true`.
6. **Immutable `createdAt` Tampering**: `UPDATE /users/alice_123/playlists/pl_1` changing `createdAt`.
7. **Immutable `userId` Tampering**: `UPDATE /users/alice_123/liked_songs/song_1` changing `userId`.
8. **Future Timestamp Spoofing**: `CREATE /users/alice_123` with `createdAt` in year 2099 (`> request.time`).
9. **Oversized String DoS**: `CREATE /users/alice_123/playlists/pl_1` with a 10,000-character `name`.
10. **Negative Duration/TrackCount**: `CREATE /users/alice_123/liked_songs/song_1` with `durationMs: -500`.
11. **Invalid Enum Value**: `CREATE /users/alice_123` with `preferredAudioQuality: "ULTRA_HACKED"`.
12. **Unauthorized Cross-User List**: `LIST /users/bob_456/listening_history` as `alice_123`.
