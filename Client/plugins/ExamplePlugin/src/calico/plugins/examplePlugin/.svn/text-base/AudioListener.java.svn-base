package calico.plugins.userlist;

import calico.events.CalicoEventHandler;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;

import javax.sound.sampled.*;

public class AudioListener
{
	private static long uuid;
	private static AudioFormat format;
	private static TargetDataLine line;
	private static boolean captureInProgress = false;
	private static boolean stopCapture = false;
	private static boolean overThreshold = false;
	private static final int threshold = 50; // 40 = picks up skype sounds, 55 = upper voice limit

	public AudioListener()
	{

	}
	
	public void startCapture(long u)
	{
		uuid = u;

		if (!captureInProgress)
		{
			captureInProgress = true;
			captureAudio();
		}
	}
	
	public void stopCapture()
	{
		stopCapture = true;
	}
	
	private void captureAudio()
	{
		try
		{
			format = getAudioFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class,
					format);
			line = (TargetDataLine)AudioSystem.getLine(info);
			line.open(format);
			Thread captureThread = new Thread(new CaptureThread());
			captureThread.start();
		}
		catch (Exception e)
		{
			System.out.println(e);
			e.printStackTrace();
		}
	}
	
	public static boolean capturing()
	{
		return captureInProgress;
	}

	private AudioFormat getAudioFormat()
	{
		float sampleRate = 8000.0F; // 8000, 11025, 16000, 22050, 44100
		int sampleSizeInBits = 16;  // 8, 16
		int channels = 1;           // 1, 2
		boolean signed = true;
		boolean bigEndian = false;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}
	
	/**
	 * Calculates the power of a signal using root mean square
	 */
	private int RMS(byte[] signal)
	{ 
		long sum = 0;
		
		for(int i = 0; i < signal.length; i++)
			sum += signal[i];

		double avg = sum / signal.length;
		double sumMeanSquare = 0;
		
		for(int i = 0; i < signal.length; i++)
			sumMeanSquare += Math.pow(signal[i] - avg, 2d);

		double avgMeanSquare = sumMeanSquare / signal.length;
		return (int)(Math.pow(avgMeanSquare, 0.5d) + 0.5);
	}
	
	/**
	 * Separate thread to capture audio, calculate power, and show/hide icon
	 */
	// areas to consider:
	// * average background noise
	// * only calculate power every x milliseconds
	class CaptureThread extends Thread
	{
		public void run()
		{
			stopCapture = false;
			overThreshold = false;
			byte[] data = new byte[4000];//[line.getBufferSize() / 5];
			line.start();
			
			try
			{
				while (!stopCapture)
				{
					if (line.read(data, 0, data.length) > 0)
					{
						int power = RMS(data);
						if (power >= threshold && !overThreshold)
						{
							audioStart();
//							System.out.println("Audio signal power above threshold (" + power + ") for uuid: " + uuid);
						}
						else if (power < threshold && overThreshold)
						{
							audioEnd();
//							System.out.println("Audio signal power below threshold (" + power + ") for uuid: " + uuid);
						}
//						else
//						{
//							System.out.println("\tAudio signal power (" + power + "); threshold (" + threshold + ")");
//						}
					}
				}				
			}
			catch (Exception e)
			{
				System.out.println(e);
				e.printStackTrace();
			}
			
			if (overThreshold)
				audioEnd();
			
			captureInProgress = false;
			line.stop();
			line.close();
			System.out.println("End Audio Thread"); // debug
		}
	}

	private void audioStart()
	{
		overThreshold = true;

		CalicoEventHandler.getInstance().fireEvent(
				UserListNetworkCommands.AUDIO_START,
				CalicoPacket.getPacket(
						UserListNetworkCommands.AUDIO_START, uuid));
		
		Networking.send(CalicoPacket.getPacket(
				UserListNetworkCommands.AUDIO_START, uuid));
		
//		System.out.println("Event: " + UserListNetworkCommands.AUDIO_START
//				+ "; uuid: " + uuid);
	}
	
	private void audioEnd()
	{
		overThreshold = false;

		CalicoEventHandler.getInstance().fireEvent(
				UserListNetworkCommands.AUDIO_END,
				CalicoPacket.getPacket(
						UserListNetworkCommands.AUDIO_END, uuid));
		
		Networking.send(CalicoPacket.getPacket(
				UserListNetworkCommands.AUDIO_END, uuid));
		
//		System.out.println("Event: " + UserListNetworkCommands.AUDIO_END
//				+ "; uuid: " + uuid);
	}
}