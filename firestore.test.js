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

test("Unauthenticated user: cannot read user data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync").get());
});

test("Unauthenticated user: cannot create user data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync").set({
    userId: ALICE_UID,
    dataJson: "{}",
    version: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user: cannot read another user's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("userData").doc("sync").set({
      userId: ALICE_UID,
      dataJson: "{}",
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync").get());
});

test("Authenticated user: cannot write to another user's data", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync").set({
    userId: BOB_UID,
    dataJson: "{}",
    version: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user: can create and read their own data with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync");
  
  await assertSucceeds(docRef.set({
    userId: ALICE_UID,
    dataJson: "{\"transactions\":[]}",
    version: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(docRef.get());
});

test("Authenticated user: cannot create with mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync");
  
  await assertFails(docRef.set({
    userId: BOB_UID,
    dataJson: "{}",
    version: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user: can update own data and cannot change immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("userData").doc("sync");
  const createdTime = new Date();

  await assertSucceeds(docRef.set({
    userId: ALICE_UID,
    dataJson: "{}",
    version: 1,
    createdAt: createdTime,
    updatedAt: createdTime,
  }));

  // Valid update
  await assertSucceeds(docRef.update({
    dataJson: "{\"updated\":true}",
    updatedAt: new Date(),
  }));

  // Invalid update: mutating createdAt
  await assertFails(docRef.update({
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});
