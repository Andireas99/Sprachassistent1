package speech;

import java.io.IOException;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Ole32;

import edu.cmu.sphinx.api.Microphone;
import edu.cmu.sphinx.api.SpeechResult;
import feedback.AlertController;
import main.Main;
import parser.IntentDetector;

public class SpeechRecognizerThread implements Runnable {

	static volatile boolean keywordActivationState = false;
	public static volatile boolean restart = false;
	public static volatile MyLiveRecognizer recognizer;

	@Override
	public void run() {

		try {
			long time = System.nanoTime();
			recognizer = new MyLiveRecognizer();
			recognizer.startRecognition(true);

			System.out.println("Recognizer is ready: " + (System.nanoTime() - time) / 1000000000.0);
			System.out.println("Total startup time: " + (System.nanoTime() - Main.totalTime) / 1000000000.0);
		} catch (IOException e) {
			AlertController.showIOExceptionDialog("Lesen");
			e.printStackTrace();
			return;
		}

		while (true) {
			Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED);
			try {
				SpeechResult result = recognizer.getResult();
				new Thread(new Runnable() {
					public void run() {
						IntentDetector.parse(result.getHypothesis().toLowerCase(), result.getTags());
					}
				}).start();
			} finally {
				Ole32.INSTANCE.CoUninitialize();
			}
		}
	}

	/**
	 * Bei Änderungen von Einträgen muss die Spracherkennung mit den neuen Namen neugestartet werden
	 */
	public static void restart() {
		Microphone mic = recognizer.getMicrophone();
		recognizer.forceStopRecognition();
		try {
			recognizer = new MyLiveRecognizer(mic);
		} catch (IOException e) {
			AlertController.showIOExceptionDialog("Lesen");
			e.printStackTrace();
		}
		recognizer.startRecognition(true);
	}

	public static boolean isHotwordActive() {
		return keywordActivationState;
	}

	public static void activateHotword() {
		keywordActivationState = true;
	}

	public static void deactivateHotword() {
		keywordActivationState = false;
	}
}
