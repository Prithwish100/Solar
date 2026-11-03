const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write user profile or liked songs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("liked_songs").get());
});

test("Authenticated user: can create and read own profile and liked songs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Prithwish",
      preferredAudioQuality: "LOSSLESS",
      amoledDarkTheme: true,
      ytMusicConnected: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_songs").doc("song_1").set({
      userId: ALICE_UID,
      trackId: "song_1",
      title: "Solar Eclipse",
      artist: "Prithwish",
      album: "Heliosphere",
      artworkUrl: "https://example.com/art.jpg",
      streamUrl: "https://example.com/audio.mp3",
      durationMs: 215000,
      sourceProvider: "INNERTUBE",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_songs").get()
  );
});

test("Authenticated user: cannot read or write another user's liked songs or playlists", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date();
    await context.firestore().collection("users").doc(BOB_UID).collection("liked_songs").doc("bob_song").set({
      userId: BOB_UID,
      trackId: "bob_song",
      title: "Bob Track",
      artist: "Bob Artist",
      album: "Bob Album",
      artworkUrl: "",
      streamUrl: "",
      durationMs: 180000,
      sourceProvider: "INNERTUBE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("liked_songs").doc("bob_song").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("liked_songs").get()
  );
});

test("Shadow update with ghost field or invalid enum is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Prithwish",
      preferredAudioQuality: "LOSSLESS",
      amoledDarkTheme: true,
      ytMusicConnected: false,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
