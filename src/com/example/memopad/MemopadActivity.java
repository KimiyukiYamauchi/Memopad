package com.example.memopad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class MemopadActivity extends Activity {

	boolean memoChanged = false;
	String fn;
	String encode = "SHIFT-JIS";

	// Intent i;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		EditText et = (EditText) findViewById(R.id.editText1);
		SharedPreferences pref = getSharedPreferences("MemoPrefs", MODE_PRIVATE);
		et.setText(pref.getString("memo", ""));
		et.setSelection(pref.getInt("cursor", 0));
		memoChanged = pref.getBoolean("memoChanged", false);

		fn = pref.getString("fn", "");
		TextWatcher tw = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				memoChanged = true;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		};
		et.addTextChangedListener(tw);

	}

	@Override
	protected void onStop() {
		super.onStop();

		EditText et = (EditText) findViewById(R.id.editText1);
		SharedPreferences pref = getSharedPreferences("MemoPrefs", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("memo", et.getText().toString());
		editor.putInt("cursor", Selection.getSelectionStart(et.getText()));
		editor.putBoolean("memoChanged", memoChanged);
		editor.putString("fn", fn);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		EditText et = (EditText) findViewById(R.id.editText1);
		switch (item.getItemId()) {

		case R.id.menu_save:
			// Log.d(ACTIVITY_SERVICE, "R.id.menu_save");
			saveMemo();
			break;
		case R.id.menu_open:
			// Log.d(ACTIVITY_SERVICE, "R.id.menu_open");
			if (memoChanged)
				saveMemo();
			Intent i = new Intent(this, MemoList.class);
			startActivityForResult(i, 0);
			break;
		case R.id.menu_new:
			// Log.d(ACTIVITY_SERVICE, "R.id.menu_new");
			if (memoChanged)
				saveMemo();
			et.setText("");
			fn = "";
			break;
		case R.id.menu_import:
			// Log.d(ACTIVITY_SERVICE, "R.id.menu_import");
			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				// Log.d(ACTIVITY_SERVICE, "StorageState() OK");
				if (memoChanged)
					saveMemo();
				memoChanged = false;
				i = new Intent(this, FilePicker.class);
				startActivityForResult(i, 1);
			} else {
				Log.d(ACTIVITY_SERVICE, "Toast!!!");
				Toast toast = Toast.makeText(this,
						R.string.toast_no_external_storage, 1000);
				toast.show();
			}
			break;
		case R.id.menu_export:
			// Log.d(ACTIVITY_SERVICE, "R.id.menu_export");
			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				// Log.d(ACTIVITY_SERVICE,
				// "R.id.menu_export StorageState() OK");
				writeFile();
				memoChanged = false;
			} else {
				Toast toast = Toast.makeText(this,
						R.string.toast_no_external_storage, 1000);
				toast.show();
			}
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			EditText et = (EditText) findViewById(R.id.editText1);

			switch (requestCode) {
			case 0:
				et.setText(data.getStringExtra("text"));
				memoChanged = false;
				fn = "";
				break;
			case 1:
				fn = data.getStringExtra("fn");
				if (fn.length() > 0) {
					et.setText(readFile());
					memoChanged = false;
				}
				break;
			}

		}

	}

	void saveMemo() {
		EditText et = (EditText) this.findViewById(R.id.editText1);
		String title;
		String memo = et.getText().toString();

		if (memo.trim().length() > 0) {
			if (memo.indexOf("\n") == -1) {
				title = memo.substring(0, Math.min(memo.length(), 20));
			} else {
				title = memo.substring(0, Math.min(memo.indexOf("\n"), 20));
			}
			String ts = DateFormat.getDateTimeInstance().format(new Date());
			MemoDBHelper memos = new MemoDBHelper(this);
			SQLiteDatabase db = memos.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("title", title + "\n" + ts);
			values.put("memo", memo);
			db.insertOrThrow("memoDB", null, values);
			memos.close();
			memoChanged = false;
		}
	}

	String readFile() {
		String str = "";
		String l = null;

		if (fn != null) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(
						new FileInputStream(fn), encode));
				do {
					l = br.readLine();
					if (l != null)
						str = str + l + "\n";
				} while (l != null);
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return str;
	}

	void writeFile() {
		// Log.d(ACTIVITY_SERVICE, "writeFile() Enter");
		// Log.d(ACTIVITY_SERVICE, "fn = " + fn);
		EditText et = (EditText) findViewById(R.id.editText1);
		String memo = et.getText().toString();

		if (fn.length() == 0) {
			String dn = Environment.getExternalStorageDirectory() + "/text/";
			fn = memo.replaceAll("\\\\|\\.|\\/|:|\\*|\\?|\"|<|>|\\n|\\|", " ")
					.trim();
			fn = dn + fn.substring(0, Math.min(fn.length(), 12)) + ".txt";
			// Log.v(ACTIVITY_SERVICE, "fn = " + fn);
			File dir = new File(dn);
			if (!dir.exists())
				dir.mkdir();

		}

		BufferedWriter bw1;
		try {
			bw1 = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(fn), encode));
			bw1.write(memo.replace("\n", "\r\n"));
			bw1.close();
			Log.v(ACTIVITY_SERVICE, "writeFile() OK");
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}
