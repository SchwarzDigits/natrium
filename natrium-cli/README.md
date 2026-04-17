## Natrium CLI

The Natrium CLI is a JVM command-line client that uses the Natrium SDK (and internally Kalium) to:

- **Log in / log out** against a Wire backend
- Display the **current session status**
- **Create, list, delete, and archive conversations**
- **Add / remove members** to/from conversations
- **Generate and use join links**
- **Send and receive chat messages** within a conversation
- **View message history** for a conversation
- **Interactive chat mode** for real-time messaging
- Run multiple commands without restarting in **interactive console mode**

The CLI primarily serves as a reference client for partners and for local testing of the Natrium SDK.

### Prerequisites

- **JDK 21**
- **Gradle Wrapper** (`./gradlew`)
- **Kalium as Git submodule** checked out and initialized

### Build

```bash
./gradlew :natrium-cli:assemble
```

### Running

```bash
./gradlew :natrium-cli:run --args "<command> [options]"
```

---

### `login`

Logs in a user and registers account + client in Kalium.

```bash
./gradlew :natrium-cli:run --args "login --backend staging --email user@example.com --password secret"
```

- `--backend`, `-b`: `staging` (default) or `production`
- `--email`, `-e`: Email address
- `--password`, `-p`: Password

### `status`

Shows the current login status.

```bash
./gradlew :natrium-cli:run --args "status"
```

### `logout`

Logs out the current user (hard logout).

```bash
./gradlew :natrium-cli:run --args "logout"
```

### `sso-login`

Logs in via SSO. Provide either an email (for domain lookup) or an SSO code directly.

```bash
./gradlew :natrium-cli:run --args "sso-login --email user@example.com"
./gradlew :natrium-cli:run --args "sso-login --code wire-<uuid>"
```

- `--email`, `-e`: Email address for SSO domain lookup
- `--code`, `-c`: SSO code (e.g. `wire-<uuid>`)

---

### Profile Commands

### `update-display-name`

Updates the display name of the logged-in user.

```bash
./gradlew :natrium-cli:run --args "update-display-name --name 'New Name'"
```

- `--name`: New display name (required)

### `update-handle`

Updates the handle of the logged-in user.

```bash
./gradlew :natrium-cli:run --args "update-handle --handle new-handle"
```

- `--handle`: New user handle (required)

### `update-email`

Updates the email of the logged-in user.

```bash
./gradlew :natrium-cli:run --args "update-email --email new@example.com"
```

- `--email`: New email address (required)

---

### `conversations`

Lists all conversations of the logged-in user.

```bash
./gradlew :natrium-cli:run --args "conversations"
```

### `conversation-create`

Creates a new conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-create --title 'Housing Benefit Application'"
```

- `--title`, `-t`: Title of the conversation (required)

### `conversation-delete`

Deletes a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-delete --id 'abc-123@wire.com'"
```

- `--id`: Conversation ID in format `value@domain`

### `conversation-archive`

Archives a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-archive --id 'abc-123@wire.com'"
```

### `conversation-members`

Lists the members of a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-members --id 'abc-123@wire.com'"
```

### `conversation-add-person`

Adds a member to a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-add-person --conversation-id 'abc-123@wire.com' --user-id 'user-456@wire.com'"
```

### `conversation-remove-person`

Removes a member from a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-remove-person --conversation-id 'abc-123@wire.com' --user-id 'user-456@wire.com'"
```

### `conversation-joinlink`

Generates a join link for a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-joinlink --id 'abc-123@wire.com'"
./gradlew :natrium-cli:run --args "conversation-joinlink --id 'abc-123@wire.com' --password 'secret'"
```

- `--id`: Conversation ID (required)
- `--password`, `-p`: Optional password for the join link

### `conversation-join`

Joins a conversation via a join link.

```bash
./gradlew :natrium-cli:run --args "conversation-join --join-link 'https://...'"
./gradlew :natrium-cli:run --args "conversation-join --join-link 'https://...' --password 'secret'"
```

- `--join-link`: Join link (required)
- `--password`, `-p`: Password (optional)

### `conversation-send`

Sends a text message to a conversation.

```bash
./gradlew :natrium-cli:run --args "conversation-send --id 'abc-123@wire.com' -m 'Hello from Natrium!'"
```

- `--id`: Conversation ID (required)
- `--message`, `-m`: Text message to send (required)

### `auth-events`

Subscribes to auth events and prints them as they occur.

```bash
./gradlew :natrium-cli:run --args "auth-events"
```

---

### Chat Commands (Console Mode)

The following chat commands are also available inside the interactive console (`console` command).

#### `conversation-send`

```
natrium> conversation-send abc-123@wire.com Hello from Natrium!
Message sent
```

#### `conversation-history`

Shows recent message history for a conversation.

```
natrium> conversation-history abc-123@wire.com
  [2024-11-14T12:00:00Z] Max: Hello from Natrium!
  [2024-11-14T12:01:00Z] Erika: Hi back!

natrium> conversation-history abc-123@wire.com 5
```

- First argument: Conversation ID (required)
- Second argument: Limit (optional, default: 20)

#### `conversation-chat`

Enters interactive chat mode for a conversation. Shows recent history, then lets you type messages. Type `/quit` to exit chat mode.

```
natrium> conversation-chat abc-123@wire.com
Chat mode for conversation abc-123@wire.com (type /quit to exit)
Loading recent messages...
  [2024-11-14T12:00:00Z] Max: Hello
---
chat> How are you?
  > sent
chat> /quit
Left chat mode
```

---

### `console`

Starts the interactive console mode (REPL). All commands can be run sequentially
without restarting. Sync is performed once and stays active.

```bash
./gradlew :natrium-cli:run --args "console" --console=plain
```

**Note:** `--console=plain` is required so that Gradle correctly passes stdin through.

All commands are available inside the console (login, status, conversations, etc.).
`help` shows the full list, `quit` exits the console.

```
natrium> login
Backend (staging/production) [production]: production
Email: user@example.com
Password: ****
Login successful

natrium> conversations
2 conversations found:
  1. Housing Benefit Application              [open]   ID: abc@wire.com

natrium> quit
Goodbye!
```

---

### Smoke Test

The CLI can be used for end-to-end smoke tests against a staging backend. Below is the full flow: login, create a conversation, send a message, observe messages, and logout.

#### Interactive (console mode)

```bash
./gradlew :natrium-cli:run --args "console" --console=plain
```

```
natrium> login
Backend (staging/production) [staging]: staging
Email: test@example.com
Password: ****
Login successful

natrium> conversation-create Smoke Test Conversation
Conversation created: Smoke Test Conversation
  ID: abc-123@staging.wire.com

natrium> conversation-send abc-123@staging.wire.com Hello from CLI
Message sent

natrium> conversation-history abc-123@staging.wire.com
  [2024-11-14T14:01:00Z] test@example.com: Hello from CLI

natrium> conversation-delete abc-123@staging.wire.com
Conversation deleted

natrium> logout
Logged out
```

#### Two-User Conversation Smoke Test

The `TwoUserConversationSmokeTest` validates the full two-party messaging flow end-to-end against a staging backend. It requires two separate test accounts and exercises the following sequence:

1. **User 1** logs in and creates a new conversation
2. **User 1** generates a join link for that conversation
3. **User 1** sends a message ("Hello from User 1") and logs out
4. **User 2** logs in and joins the conversation via the join link
5. **User 2** sends a reply ("Hello from User 2") and logs out
6. **User 1** logs back in, sends another message ("Reply from User 1"), deletes the conversation, and logs out

A `finally` block performs best-effort cleanup (delete conversation + logout) in case any step fails.

##### Configuration

Credentials can be provided via environment variables or `local.properties` (searched upward from the working directory):

| Environment Variable       | `local.properties` Key | Description              |
|----------------------------|------------------------|--------------------------|
| `NATRIUM_TEST_EMAIL`       | `test.email`           | Email for User 1         |
| `NATRIUM_TEST_PASSWORD`    | `test.password`        | Password for User 1      |
| `NATRIUM_TEST_EMAIL_2`     | `test.email2`          | Email for User 2         |
| `NATRIUM_TEST_PASSWORD_2`  | `test.password2`       | Password for User 2      |

If any credential is missing, the test prints `SKIPPED` and returns without failure.

##### Running

```bash
./gradlew :natrium-cli:test --tests "schwarz.digits.natrium.cli.TwoUserConversationSmokeTest"
```

#### Scriptable (one-liner per step)

Each step is a separate Gradle invocation. Session state is persisted to `~/.natrium`, so subsequent commands reuse the active session.

```bash
# 1. Login
./gradlew :natrium-cli:run --args "login -b staging -e test@example.com -p secret"

# 2. Create a conversation
./gradlew :natrium-cli:run --args "conversation-create --title 'Smoke Test Conversation'"

# 3. Send a message (console mode)
./gradlew :natrium-cli:run --args "console"
# then: conversation-send abc-123@staging.wire.com Hello from CLI

# 4. View message history
# then: conversation-history abc-123@staging.wire.com

# 5. Delete the conversation
./gradlew :natrium-cli:run --args "conversation-delete --id 'abc-123@staging.wire.com'"

# 6. Logout
./gradlew :natrium-cli:run --args "logout"
```

---

### Data Storage

Natrium/Kalium stores accounts and session data under:

```text
~/.natrium
```
