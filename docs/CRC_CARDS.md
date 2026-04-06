# CRC cards — kebabs-wrap codebase

This document lists **Class–Responsibility–Collaborators** for every Java source file under `app/src/main/java`.  
**Scope:** application Java classes only (layouts, Gradle, tests, and generated code are excluded).

**Legend**

- **Responsibilities:** what the class or interface is for.
- **Collaborators:** other types, Android/Firebase APIs, or layers it depends on or hands work to.

---

## Root package — `com.example.eventmanager`

| File | Class / interface | Responsibilities | Collaborators |
|------|---------------------|------------------|---------------|
| `AcceptDeclineActivity.java` | `AcceptDeclineActivity` | Screen for lottery winners to accept or decline enrollment; coordinates writes to Firestore enrollment/cancel paths and navigation. | `EventRepository`, `NotificationRepository`, intents, UI |
| `AnasEntrant.java` | `AnasEntrant` | Row model for lists: device id, name, email, status; `sectionHeader` for grouped list headers. | `EntrantAdapter`, `ManageEventActivity` |
| `AnasEvent.java` | `AnasEvent` | Event POJO with embedded waitlist/chosen/attendees/declined lists; helpers for registration window and capacity checks. | `AnasFirebaseRepo`, Firestore |
| `AnasFirebaseRepo.java` | `AnasFirebaseRepo` | Legacy/alternate Firestore API: listen to event, join waitlist, get event, accept/decline invitation, query lists. | `AnasEvent`, Firestore, `EventActivity` |
| `BrowseEventsActivity.java` | `BrowseEventsActivity` | Full-screen browse from home “See All”; keyword search and filters (open registration, capacity). | Root `FirebaseRepository.getInstance()`, `fetchAllActiveEvents`, `HomeEventAdapter`, `DocumentSnapshot`s |
| `CreateEventActivity.java` | `CreateEventActivity` | Form to create events and persist `models.Event` to Firestore. | `models.Event`, `FirebaseRepository`, Storage/poster as needed |
| `DeviceAuthManager.java` | `DeviceAuthManager` | Reads Android ID from `Settings.Secure` as stable device identity. | `Context`, Android Settings API |
| `EnrolledListActivity.java` | `EnrolledListActivity` | Shows enrolled entrants for an event (organizer view). | `FirebaseRepository`, `Entrant`, `WinnerAdapter` / list UI |
| `EntrantAdapter.java` | `EntrantAdapter` | `RecyclerView` adapter for `AnasEntrant` rows. | `AnasEntrant`, layout holders |
| `Entrant.java` | `Entrant` | **Legacy** user POJO (subset of fields vs `models.Entrant`). | Root `EventAdapter` consumers, tests |
| `EntrantListCallback.java` | `EntrantListCallback` | Callback: success with `List<Entrant>` or failure message. | `models.Entrant`, loaders |
| `EntrantMapActivity.java` | `EntrantMapActivity` | Map UI for entrant/geolocation-related viewing (Google Map). | Maps SDK, `OnMapReadyCallback`, event/location data |
| `EventActivity.java` | `EventActivity` | Minimal event screen: listens to `AnasEvent`, optional join waitlist via `AnasFirebaseRepo`. | `AnasFirebaseRepo`, `AnasEvent` |
| `EventAdapter.java` | `EventAdapter` | Lists **root** `Event` objects in a `RecyclerView`. | `com.example.eventmanager.Event`, Glide, click listener |
| `EventDetailsActivity.java` | `EventDetailsActivity` | Rich event detail, registration, comments, organizer actions, QR, etc. | `FirebaseRepository`, `EventRepository`, `FollowRepository`, `EventComment`, dialogs |
| `Event.java` | `Event` | **Legacy** event POJO with `isDeleted` flag (differs from `models.Event`). | Same-package adapters/activities |
| `EventQRActivity.java` | `EventQRActivity` | Displays or shares QR for an event id. | `QRCodeManager`, intents |
| `EventRepository.java` | `EventRepository` | Firestore ops on event subcollections: waiting, selected, invitee, enrolled, cancelled. | Firestore, `OrganizerNotificationManager`, `AcceptDeclineActivity` |
| `FirebaseRepository.java` | `FirebaseRepository` | **Singleton** Firestore helper: aggregate queries, documents, batches used by several screens. | Firestore, `models.Event` / `Entrant` where applicable |
| `FollowerBroadcastActivity.java` | `FollowerBroadcastActivity` | Organizer composes title/body and sends a broadcast to all followers via `FollowRepository.broadcastToFollowers`. | `FollowRepository`, `managers.DeviceAuthManager` |
| `HomeActivity.java` | `HomeActivity` | Main entrant home: featured events, navigation to profile, browse, notifications, etc. | `FirebaseRepository`, `HomeEventAdapter`, `models.Entrant` |
| `ImageManager.java` | `ImageManager` | Upload/delete **event poster** under `event_posters/{eventId}.jpg` in Firebase Storage. | Firebase Storage, URIs |
| `InviteGuestsActivity.java` | `InviteGuestsActivity` | Private-event guest invites; list pending invites and actions. | `GuestInviteAdapter`, `EventRepository`, Firestore |
| `LotteryCallback.java` | `LotteryCallback` | Callback after lottery draw: winners + device ids or error. | `models.Entrant`, `LotteryManager` |
| `LotteryManager.java` | `LotteryManager` | Random sample from waitlist into `selected` subcollection. | Firestore, `LotteryCallback`, `Entrant` resolution |
| `MainActivity.java` | `MainActivity` | Admin dashboard hub: cards to events, profiles, images, comments, notification logs. | Admin activities |
| `ManageEventActivity.java` | `ManageEventActivity` | Organizer event management: details, lottery preview, entrants, invites, navigation to sub-tools. | `AnasEntrant`, `EntrantAdapter`, repos, `OrganizerPermissionHelper` |
| `ManagePosterActivity.java` | `ManagePosterActivity` | Pick/upload/manage poster for an event. | `ImageManager`, Storage, event id |
| `ModerateCommentsActivity.java` | `ModerateCommentsActivity` | Organizer-facing comment moderation for an event. | Firestore comments, adapters |
| `MyEventsActivity.java` | `MyEventsActivity` | Lists events the user organizes or is tied to. | Firestore, `EventAdapter` or list UI |
| `NotificationAdapter.java` | `NotificationAdapter` | Lists in-app `Notification` rows. | `Notification`, read/mark UI |
| `Notification.java` | `Notification` | POJO for `users/{id}/notifications` documents. | `NotificationRepository`, UI |
| `NotificationLog.java` | `NotificationLog` | POJO for `notificationLogs` audit documents. | Admin logs, `OrganizerNotificationManager` |
| `NotificationRepository.java` | `NotificationRepository` | CRUD and realtime listeners for per-user notifications. | Firestore, `Notification` |
| `NotificationsActivity.java` | `NotificationsActivity` | Inbox UI; opens `AcceptDeclineActivity` when `eventId` present. | `NotificationRepository`, `NotificationAdapter` |
| `OrganizerNotificationManager.java` | `OrganizerNotificationManager` | Sends group notifications (waitlist, winners, etc.), respects opt-out, writes `NotificationLog`. | `EventRepository`, `NotificationRepository`, Firestore |
| `OrganizerPermissionHelper.java` | `OrganizerPermissionHelper` | Static helpers: primary organizer, co-organizer, `canActAsOrganizer` on `DocumentSnapshot`. | Firestore event shape |
| `OrganizerProfileActivity.java` | `OrganizerProfileActivity` | Public organizer profile; follow/unfollow, events. | `FollowRepository`, profile UI |
| `QRCodeManager.java` | `QRCodeManager` | Encode event id string into QR `Bitmap` (ZXing). | ZXing, Android `Bitmap` |
| `QRScannerActivity.java` | `QRScannerActivity` | Camera/scanner to resolve QR into event id and navigate. | Barcode APIs / camera |
| `RunLotteryActivity.java` | `RunLotteryActivity` | Organizer UI to run lottery via `LotteryManager` and show results. | `LotteryManager`, `LotteryCallback`, notifications |
| `WaitlistCallback.java` | `WaitlistCallback` | Simple success/failure callback for waitlist operations. | Callers joining waitlist |

---

## `com.example.eventmanager.models`

| File | Class / interface | Responsibilities | Collaborators |
|------|---------------------|------------------|---------------|
| `Event.java` | `Event` | Canonical Firestore event document: schedule, location, capacity, poster, organizer, geo, private flag. | `FirebaseRepository`, UI, `RegistrationHistoryItem` |
| `Entrant.java` | `Entrant` | User profile: id, contact, photo, roles, notification preference, disabled flag. | Firestore `users`, repositories, profile screens |
| `EventComment.java` | `EventComment` | Comment on event: author, text, timestamp, denormalized event fields; `fromDocument` factory. | Firestore subcollections, `EventCommentAdapter` |
| `RegistrationHistoryItem.java` | `RegistrationHistoryItem` | Pairs `Event` with `RegistrationStatus` for history list; display helpers. | `RegistrationHistoryAdapter`, `FirebaseRepository` |
| *(nested)* | `RegistrationHistoryItem.RegistrationStatus` | Enum: waiting, selected, invited, enrolled, cancelled. | `RegistrationHistoryItem` |

---

## `com.example.eventmanager.repository`

| File | Class | Responsibilities | Collaborators |
|------|-------|------------------|---------------|
| `FirebaseRepository.java` | `FirebaseRepository` | **Primary** data layer: users, events, registration history, subcollections, deletes across events. | Firestore, `models.*`, callbacks |
| `FollowRepository.java` | `FollowRepository` | Follow/unfollow organizers; follower counts; may enqueue notifications. | Firestore, `NotificationRepository` |

---

## `com.example.eventmanager.admin`

| File | Class | Responsibilities | Collaborators |
|------|-------|------------------|---------------|
| `AdminCommentListItem.java` | `AdminCommentListItem` | View-model row for admin comment list from snapshot: author, text, event line, relative time, avatar styling. | `DocumentSnapshot`, `AdminCommentsAdapter` |
| `AdminCommentsActivity.java` | `AdminCommentsActivity` | Admin browses all event comments. | Firestore queries, `AdminCommentsAdapter` |
| `AdminCommentsAdapter.java` | `AdminCommentsAdapter` | Renders `AdminCommentListItem` or similar rows. | `AdminCommentListItem`, RecyclerView |
| `AdminEventDetailDialog.java` | `AdminEventDetailDialog` | Bottom sheet: admin view of one event’s details. | `DocumentSnapshot`, Firestore |
| `AdminEventsActivity.java` | `AdminEventsActivity` | Admin list of events; open detail or remove. | `adapter.EventAdapter`, Firestore |
| `AdminImagesActivity.java` | `AdminImagesActivity` | Admin browse/delete poster images in Storage. | `adapter.ImageAdapter`, `managers.ImageManager` |
| `AdminNotificationLogItem.java` | `AdminNotificationLogItem` | View-model for one `notificationLogs` row; `fromSnapshot`. | Firestore, `AdminNotificationLogsAdapter` |
| `AdminNotificationLogsActivity.java` | `AdminNotificationLogsActivity` | Admin audit list of organizer notification dispatches. | `AdminNotificationLogsAdapter`, Firestore |
| `AdminNotificationLogsAdapter.java` | `AdminNotificationLogsAdapter` | Renders notification log rows. | `AdminNotificationLogItem` |
| `AdminProfileDetailDialog.java` | `AdminProfileDetailDialog` | Bottom sheet for admin profile inspection/actions. | User docs, Firestore |
| `AdminProfilesActivity.java` | `AdminProfilesActivity` | Admin list of user profiles. | `ProfileAdapter`, Firestore |

---

## `com.example.eventmanager.adapter`

| File | Class | Responsibilities | Collaborators |
|------|-------|------------------|---------------|
| `EventAdapter.java` | `EventAdapter` | Admin event list from `DocumentSnapshot`s (not root `Event` model). | `AdminEventsActivity`, Glide, dates |
| `GuestInviteAdapter.java` | `GuestInviteAdapter` | Rows for invitee list with accept/decline actions. | `InviteGuestsActivity` |
| `HomeEventAdapter.java` | `HomeEventAdapter` | Home feed event cards. | `HomeActivity`, event docs/models |
| `ImageAdapter.java` | `ImageAdapter` | Grid of image URLs for admin images screen. | `AdminImagesActivity`, Glide |
| `ProfileAdapter.java` | `ProfileAdapter` | Admin profile list rows. | `AdminProfilesActivity` |
| `RegistrationHistoryAdapter.java` | `RegistrationHistoryAdapter` | Registration history rows using `RegistrationHistoryItem`. | `EditProfileActivity` |

---

## `com.example.eventmanager.adapters`

| File | Class | Responsibilities | Collaborators |
|------|-------|------------------|---------------|
| `EnrolledEntrantAdapter.java` | `EnrolledEntrantAdapter` | Rows for enrolled `Entrant` profiles. | `EnrolledListActivity`, `models.Entrant` |
| `EventCommentAdapter.java` | `EventCommentAdapter` | Comment list under event details. | `EventComment`, `EventDetailsActivity` |
| `WinnerAdapter.java` | `WinnerAdapter` | Displays lottery winners / selected entrants. | `RunLotteryActivity`, `models.Entrant` |

---

## `com.example.eventmanager.ui`

| File | Class | Responsibilities | Collaborators |
|------|-------|------------------|---------------|
| `BookedEventsActivity.java` | `BookedEventsActivity` | Onboarding-style screen; skip/next go to `QrAccessActivity`. | Intents |
| `EditProfileActivity.java` | `EditProfileActivity` | Edit entrant profile and show registration history. | `FirebaseRepository`, `RegistrationHistoryItem`, `RegistrationHistoryAdapter` |
| `EntrantSignUpActivity.java` | `EntrantSignUpActivity` | First-time profile creation for device id. | `models.Entrant`, `FirebaseRepository` |
| `ProfileActivity.java` | `ProfileActivity` | View own profile and navigate to edit. | `Entrant`, repository |
| `QrAccessActivity.java` | `QrAccessActivity` | Gate or explain QR-based access to features. | Navigation to scanner or home |
| `SplashActivity.java` | `SplashActivity` | App entry: load user doc, route to signup or home. | `FirebaseRepository`, `Entrant` |

---

## `com.example.eventmanager.managers`

| File | Class | Responsibilities | Collaborators |
|------|-------|------------------|---------------|
| `DeviceAuthManager.java` | `DeviceAuthManager` | Same as root `DeviceAuthManager`: Android ID helper. | `Context` |
| `ImageManager.java` | `ImageManager` | Lists all poster URLs under Storage folder `posters/` for admin gallery. | Firebase Storage |

---

## Duplicate class names (different packages)

| Name | Locations | Note |
|------|-----------|------|
| `DeviceAuthManager` | `com.example.eventmanager` and `com.example.eventmanager.managers` | Identical responsibility; two copies. |
| `ImageManager` | `com.example.eventmanager` (upload/delete single poster) vs `managers` (list all in folder) | Different APIs — do not merge blindly. |
| `FirebaseRepository` | `repository` (primary, user/event/history) vs root (singleton, extra queries) | Two repositories with overlapping names; callers use one or the other by import. |
| `Event` / `Entrant` | `models` vs root package | Prefer `models` for new code; root types kept for legacy screens/adapters. |

---

## File count

- **Java files in `app/src/main/java`:** 76  
- **This document:** one CRC row per **public** top-level class/interface per file, plus nested `RegistrationStatus` and duplicate-name notes.
