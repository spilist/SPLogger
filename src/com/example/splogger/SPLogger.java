package com.example.splogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class SPLogger extends Activity {

	String FileName, userLocation, userAction, userPrivacy = "";
	String LogStr, ConLog, WifiLog, WifiListLog, GPSLog, UserLog, AudioLog, CameraLog = "";
	BufferedWriter out;
	SimpleDateFormat DF = new SimpleDateFormat("yyMMdd", Locale.getDefault());
	SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd_HHmm", Locale.getDefault());
	private WifiManager wManager;
	private WifiInfo wInfo;
	private LocationManager lManager;
	private Location lInfo;
	private Criteria crt;
	private String bestProvider;
	private ConnectivityManager cManager;
	private NetworkInfo nInfo;
	private MediaRecorder mRecorder;
	private NotificationManager nm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splogger);

		// 각 스피너의 리스너 등록
		Spinner loc_spinner = (Spinner) findViewById(R.id.location);
		ArrayAdapter<CharSequence> loc_adapter = ArrayAdapter
				.createFromResource(this, R.array.location_set,
						android.R.layout.simple_spinner_item);
		loc_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		loc_spinner.setAdapter(loc_adapter);
		loc_spinner.setOnItemSelectedListener(new onItemSelectedListner());

		Spinner action_spinner = (Spinner) findViewById(R.id.action);
		ArrayAdapter<CharSequence> action_adapter = ArrayAdapter
				.createFromResource(this, R.array.action_set,
						android.R.layout.simple_spinner_item);
		action_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		action_spinner.setAdapter(action_adapter);
		action_spinner.setOnItemSelectedListener(new onItemSelectedListner());

		Spinner prv_spinner = (Spinner) findViewById(R.id.privacy);
		ArrayAdapter<CharSequence> prv_adapter = ArrayAdapter
				.createFromResource(this, R.array.privacy_set,
						android.R.layout.simple_spinner_item);
		prv_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		prv_spinner.setAdapter(prv_adapter);
		prv_spinner.setOnItemSelectedListener(new onItemSelectedListner());

		// 로그가 저장될 파일 만들기
		String savingPath = Environment.getExternalStorageDirectory().getPath()
				+ "/SPLogger";
		File directory = new File(savingPath);
		String FilePath = directory.getAbsolutePath() + "/SPLogger_" + DF.format(new Date()) + ".txt";
		File logFile = new File(FilePath);
		if (!directory.exists())
			directory.mkdir();
		try {
			out = new BufferedWriter(new FileWriter(FilePath, true));
			// 로그파일이 새 파일이면 첫 라인에 헤더를 씀
			if (logFile.length() == 0) {
				out.write("Date\tLocation\tAction\tPrivacy\tMore\tMic\tCamera\tGPSLati\tGPSLongi\tGPSAlti\tSpeed\tNetwork\tWifi\tWifiList");
				out.newLine();
			}
		} catch (FileNotFoundException e) {
			System.err.println("File Not Found");
		} catch (IOException e) {
			System.err.println("IOEXCEPTION");
		}

		//GPS Criteria
		crt = new Criteria();
    	crt.setAccuracy(Criteria.ACCURACY_COARSE);
    	crt.setPowerRequirement(Criteria.POWER_HIGH);
    	crt.setAltitudeRequired(true);
    	crt.setSpeedRequired(true);
    	
    	//녹음기 세팅
    	mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    	
		findViewById(R.id.btn_log).setOnClickListener(
			new View.OnClickListener() {
	
				@Override
				public void onClick(View v) {
					// 기록 년월일
					LogStr = DF.format(new Date());
	
					// SpyCamOS로 인텐트를 보내 사진찍음
					Intent intent = new Intent("ScreenPrivacy");
					sendBroadcast(intent);
	
					// 사용자 입력 정보 기록
					UserLog = "\t" + userLocation;
					UserLog += "\t" + userAction;
					UserLog += "\t" + userPrivacy;
					UserLog += "\t" + userAction;
					EditText additional = (EditText) SPLogger.this
							.findViewById(R.id.additional);
					UserLog += "\t" + additional.getText().toString();
	
					// 네트워크 로그 기록
					cManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					nInfo = cManager.getActiveNetworkInfo();
					if (nInfo != null) {
						ConLog += "\t" + nInfo.getTypeName();
						ConLog += "\t" + nInfo.isRoaming();
						ConLog += "\t" + nInfo.isFailover();
						ConLog += "\t" + nInfo.getDetailedState();
					} else {
						ConLog += "\tNull\tNull\tNull\tNull";
					}
	
					// 와이파이 로그 기록
					wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					wInfo = wManager.getConnectionInfo();
	
					if (wManager.isWifiEnabled() && wInfo.getSSID() != null) {
						int ipAddr = wInfo.getIpAddress();
	
						WifiLog += "\t" + wInfo.getSSID();
						WifiLog += "\t" + wInfo.getBSSID();
						WifiLog += "\t" + wInfo.getNetworkId();
						WifiLog += "\t"
								+ String.format("%d.%d.%d.%d",
										(ipAddr & 0xff),
										(ipAddr >> 8 & 0xff),
										(ipAddr >> 16 & 0xff),
										(ipAddr >> 24 & 0xff));
						WifiLog += "\t" + wInfo.getLinkSpeed();
						WifiLog += "\t" + wInfo.getRssi();
						WifiLog += "\t"
								+ wInfo.getSupplicantState().toString();
	
						List<ScanResult> wifiList = wManager
								.getScanResults();
						WifiListLog += "\tWifi List Start";
						WifiListLog += "\t" + wifiList.size();
						for (int i = 0; i < wifiList.size(); i++) {
							WifiListLog += "\t" + wifiList.get(i).SSID;
							WifiListLog += "\t" + wifiList.get(i).BSSID;
							WifiListLog += "\t" + wifiList.get(i).frequency;
							WifiListLog += "\t" + wifiList.get(i).level;
							if (wifiList.get(i).capabilities.equals(""))
								WifiListLog += "\t[OPEN]";
							else
								WifiListLog += "\t"
										+ wifiList.get(i).capabilities;
						}
						WifiListLog += "\tWifi List End";
					} else {
						WifiLog += "\tNull\tNull\tNull\tNull\t0\t0\tNull";
						WifiListLog += "\tList Null\t0";
					}
						
					// GPS 정보 기록
					lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					if (lManager != null) {
						bestProvider = lManager.getBestProvider(crt, true);
						lInfo = lManager.getLastKnownLocation(bestProvider);
					}
					if (lInfo == null && lManager != null) {
						lInfo = lManager.getLastKnownLocation(bestProvider);
					}
					if (lInfo != null)
						GPSLog += String.format("\t%f\t%f\t%f\t%f",
								lInfo.getLatitude(), lInfo.getLongitude(),
								lInfo.getAltitude(), lInfo.getSpeed());
					else
						GPSLog += String.format("\t%f\t%f\t%f\t%f", 0, 0,
								0, 0);						
					
					// 5초동안 녹음
					String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SPLogger/";
					String audioFile = "SPLogger_" + SDF.format(new Date()) + ".amr";
					mRecorder.setOutputFile(path + audioFile);
					mRecorder.setMaxDuration(5000);
					try {
			            mRecorder.prepare();
			        } catch (IOException e) {
			            Log.e("audio", "prepare() failed");
			        }
					mRecorder.start();								
					AudioLog = "\t" + audioFile;
					
					// 브로드캐스트 리시버로 돌아온 인텐트를 받아 사진파일 이름을 얻어냄
					IntentFilter ft = new IntentFilter();
					ft.addAction("ScreenPrivacy.return");
					BroadcastReceiver rec = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							CameraLog = "\t"	+ intent.getStringExtra("FileName");
							//로그 정보 종합 기록: 여기의 타이밍이 가장 느리기 때문에 여기서 로깅해야 사진파일 이름이 제대로 뜸
							LogStr += UserLog + AudioLog + CameraLog + GPSLog + ConLog + WifiLog + WifiListLog;
							try {
								out.append(LogStr + "\n");
								out.flush();
								out.close();
							} catch (IOException e) {
								System.err.println("OnClick IOException");
							}
						}
					};
					registerReceiver(rec, ft);					
				}
			});
	}

	public class onItemSelectedListner implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
			if (parent == (findViewById(R.id.location))) {
				userLocation = parent.getItemAtPosition(pos).toString();
			} else if (parent == (findViewById(R.id.action))) {
				userAction = parent.getItemAtPosition(pos).toString();
			} else if (parent == (findViewById(R.id.privacy))) {
				userPrivacy = parent.getItemAtPosition(pos).toString();
			}
		}

		public void onNothingSelected(AdapterView<?> parent) {

		}
	}
}
