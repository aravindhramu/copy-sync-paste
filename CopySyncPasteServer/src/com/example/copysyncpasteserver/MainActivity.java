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
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
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

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	  private static int DISCOVERY_REQUEST = 1;
	  private AlertDialog.Builder alertdialogbuilder;
	  private BluetoothAdapter bt;
	  final Context context = this;
	  private BluetoothSocket socket;
	  private UUID uuid = UUID.fromString("650093f0-93d5-11e2-9e96-0800200c9a66");



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		configureBluetooth();
	    //setupListenButton();
	}

	private Thread serverworker = new Thread(){
		public void run(){
			listen();
		}
	};

	private void configureBluetooth() {
		bt = BluetoothAdapter.getDefaultAdapter();
		if(bt == null){
			finish();
			return;
		}
		
	}

	public void onListen(View view){
		Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(enabler,DISCOVERY_REQUEST);
	}
	@Override
	protected void onActivityResult(int requestCode,int resultCode,Intent intent){
		boolean isDiscoverable = (resultCode > 1);
		if(isDiscoverable){
					serverworker.start();
					alertdialogbuilder = new AlertDialog.Builder(this.context);
					alertdialogbuilder.setTitle("Server started");
					alertdialogbuilder.setMessage("Listening for incoming connection");
					alertdialogbuilder.setPositiveButton("Click to close", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							serverworker.interrupt();
							serverworker = null;
							finish();
							
						}
					});
					alertdialogbuilder.show();
		}
		else{
			finish();
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
		
		
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
