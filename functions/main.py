from firebase_functions import firestore_fn
from firebase_admin import initialize_app, firestore, messaging

initialize_app()

@firestore_fn.on_document_created(document="events/{eventId}/winners/{deviceId}")
def notify_winner(event: firestore_fn.Event[firestore_fn.DocumentSnapshot]) -> None:
    """
    Triggers when a new document is created in events/{eventId}/winners/{deviceId}.

    Flow:
    1. Reads the deviceId from the new winner document
    2. Looks up the event name from events/{eventId}
    3. Looks up the winner's FCM token from users/{deviceId}
    4. Sends a push notification via FCM
    """

    # --- Step 1: Extract path parameters and winner data ---
    winner_data = event.data.to_dict()
    device_id = winner_data.get("deviceId")
    event_id = event.params["eventId"]

    if not device_id:
        print("No deviceId found in winner document, skipping.")
        return

    db = firestore.client()

    # --- Step 2: Look up the event name ---
    event_doc = db.collection("events").document(event_id).get()
    event_name = event_doc.to_dict().get("name", "the event") if event_doc.exists else "the event"

    # --- Step 3: Look up the winner's FCM token ---
    user_doc = db.collection("users").document(device_id).get()

    if not user_doc.exists:
        print(f"No user document found for deviceId: {device_id}")
        return

    fcm_token = user_doc.to_dict().get("fcmToken")

    if not fcm_token:
        print(f"No FCM token found for deviceId: {device_id}")
        return

    # --- Step 4: Send the push notification ---
    message = messaging.Message(
        token=fcm_token,
        notification=messaging.Notification(
            title="You've been selected!",
            body=f"Congratulations! You were chosen to attend: {event_name}"
        )
    )

    response = messaging.send(message)
    print(f"Notification sent successfully. FCM response: {response}")
