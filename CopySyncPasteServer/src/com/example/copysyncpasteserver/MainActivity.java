/*
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package com.example.copysyncpasteserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	  private static int DISCOVERY_REQUEST = 1;
	  private String DEVICE_NAME = "Aravindh_PC";
	  private boolean is_send = true;
	  private OutputStream os;
	  private AlertDialog.Builder alertdialogbuilder;
	  private ArrayAdapter<String> btArrayAdapter;
	  private Dialog alert;
	  private BluetoothAdapter bt;
	  final Context context = this;
	  private BluetoothSocket socket;
	  private UUID uuid = UUID.fromString("650093f0-93d5-11e2-9e96-0800200c9a66");
	  private UUID server_uuid = UUID.fromString("0e570b82-f88c-42a9-b295-44306e469a8f");



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		configureBluetooth();
		//registerReceiver(ActionFoundReceiver, 
		//          new IntentFilter(BluetoothDevice.ACTION_FOUND));
	    //setupListenButton();
	}
	
	public void onQuit(View view){
		finish();
		return;
	}

	private Thread serverworker = new Thread(){
		public void run(){
			listen();
		}
	};

	private Thread clientworker = new Thread(){
		public void run(){
			send_clip();
		}
	};
		protected void send_clip() {
			Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
			Integer s = pairedDevices.size();
			Log.d("AVI",s.toString());
			if(pairedDevices.size() > 0){
				for(BluetoothDevice device: pairedDevices){
					try{
						Log.d("AVI",device.getName());
						if(device.getName().equalsIgnoreCase(DEVICE_NAME)){
							Log.d("BT","device present");
							Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
							socket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));
							//socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
							Log.d("SCK","socket created");
							break;
						}
					}
					catch(Exception e){
						socket = null;
						continue;
					}
				}
				if(socket == null){
					Log.e("CLIENT","Service discovery failed or device not paired");
				}
				else{
					try{
						if(bt.isDiscovering())
							bt.cancelDiscovery();
						Log.d("SOCKET","Connecting");
						socket.connect();
						Log.d("SOCKET", "connected");
					}
					catch(IOException e){
						try{
							Log.e("BT",e.toString());
							socket.close();
						}
						catch(IOException e1){
							Log.e("BT",e1.toString());
						}
						is_send = true;
						return;
					}
					try{
						os = socket.getOutputStream();
					}
					catch(IOException e){
						os = null;					
					}
					ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
					String text_in_clip = clipboard.getText().toString();
					Log.d("CLIPBOARD",text_in_clip);
					PrintWriter pw = new PrintWriter(os,true);
					pw.println(text_in_clip);
					//pw.close();
					try{
						socket.close();
					}
					catch(IOException e){
						Log.e("BT",e.toString());
					}
				}
			}
			is_send = true;
			Log.d("DONE","Done");
			return;			
		}
	
	private void configureBluetooth() {
		bt = BluetoothAdapter.getDefaultAdapter();
		if(bt == null){
			finish();
			return;
		}
		
	}

	public void onDestroy(){
		super.onDestroy();
		bt.disable();
		//unregisterReceiver(ActionFoundReceiver);
	}
	
	public void onResume(){
		super.onResume();
		
	}
	
	public void onPause(){
		super.onPause();
		}
	
	public void onListen(View view){
		Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(enabler,DISCOVERY_REQUEST);
	}
	
	public void onClient(View view){
		is_send = false;
		Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(enabler,DISCOVERY_REQUEST);
		
	}
	
	@Override
	protected void onActivityResult(int requestCode,int resultCode,Intent intent){ 
		boolean isDiscoverable = (resultCode > 1);
		if(isDiscoverable && is_send){
					serverworker.start();
					alertdialogbuilder = new AlertDialog.Builder(this.context);
					alertdialogbuilder.setTitle("Server started");
					alertdialogbuilder.setMessage("Listening for incoming connection");
					alertdialogbuilder.setPositiveButton("Click to close", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							serverworker.interrupt();
							serverworker = null;
							//finish();
							
						}
					});
					alert = alertdialogbuilder.create();
					alertdialogbuilder.show();
		}
		else if(isDiscoverable && !is_send){
			//btArrayAdapter.clear();
			//bt.startDiscovery();
			clientworker.start();
			alertdialogbuilder = new AlertDialog.Builder(this.context);
			alertdialogbuilder.setTitle("Starting Client..");
			alertdialogbuilder.setMessage("Sending clipboard contents");
			alertdialogbuilder.setPositiveButton("Click to close", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					clientworker.interrupt();
					clientworker = null;
					//finish();
					
				}
			});
			alert = alertdialogbuilder.create();
			alertdialogbuilder.show();
			
		}
		else{
			
			return;
		}
	}
	
	protected void listen(){
		try{
			BluetoothServerSocket btserver = bt.listenUsingRfcommWithServiceRecord("btserver", uuid);
			socket = btserver.accept();
			btserver.close();
			
			String strinbuf = new String();
			if(socket != null){
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				strinbuf = in.readLine();
				socket.close();
			}
			
			File file = new File(Environment.getExternalStorageDirectory(),strinbuf);
			StringBuilder clip_contents = new StringBuilder();

			try {
			    BufferedReader br = new BufferedReader(new FileReader(file));
			    String line;

			    while ((line = br.readLine()) != null) {
			        clip_contents.append(line);
			        clip_contents.append('\n');
			    }
			}
			catch (IOException e) {
			    //You'll need to add proper error handling here
				Log.e("FILE_OPEN","error in opening file"+e);
			}
			
			ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(clip_contents);
			
		}
		catch(IOException e){
			Log.e("CLIPBOARD","clip service");
		}
		}
	/*	
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver(){

		  @Override
		  public void onReceive(Context context, Intent intent) {
		   String action = intent.getAction();
		   if(BluetoothDevice.ACTION_FOUND.equals(action)) {
		             BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		             btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		             btArrayAdapter.notifyDataSetChanged();
		         }
		  }};
		    
		*/
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
