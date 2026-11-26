# Friday for Android

An Android wake-word launcher prototype. It detects **Friday** locally with Vosk and attempts to launch the device's default digital assistant.

## Scope

- Start and stop listening from the app.
- Microphone foreground service with a persistent notification.
- Download and unpack the Vosk small English model on first start.
- On-device speech recognition constrained to the wake phrase "Friday".
- Attempt to launch the system's configured assistant after detection.
- GitHub Actions builds a debug APK and uploads it as a workflow artifact.

## Important limitations

This is a prototype, not a replacement for Android's privileged system hotword service. Android restricts background microphone access and launching other apps from the background. Start listening while the app is visible, grant microphone permission, and keep the foreground notification active. Samsung battery optimisation may interrupt long sessions.

Vosk's general-purpose small English model is not a dedicated keyword-spotting model; false activations and missed detections are possible. First launch downloads the acoustic model (internet required).

## Build

Open **Actions → Build Friday APK**, run the workflow, then download the `Friday-debug-apk` artifact from the completed run.

## Privacy

Recognition runs locally. The model is downloaded from the Vosk project; Friday does not upload microphone recordings.

## License

App source: MIT. Vosk and its acoustic model are separately licensed.
