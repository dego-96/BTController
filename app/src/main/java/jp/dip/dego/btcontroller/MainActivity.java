package jp.dip.dego.btcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "BTController";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private SeekBar mSteerSeekBar;
    private SeekBar mThrottleSeekBar;

    private int mSteerValue = 7;       // 初期値は中央
    private int mThrottleValue = 7;     // 初期値は停止
    private int mWay = 0;               // 初期値は前進
    private static final int MIN_STEER_POSITION = 300;
    private static final int MAX_STEER_POSITION = 80;
    private static final int MAX_THROTTLE = 255;
    private static final int COMMAND_SPAN = 100; // 100ms
    private long mLastCommandTime = 0;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // BluetoothAdapterの取得
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 端末がBluetoothに対応していない場合
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "端末がBluetoothに対応していません", Toast.LENGTH_LONG).show();
            finish(); // 強制終了
        }

        // Bluetoothが有効になっているか確認
        if (mBluetoothAdapter.isEnabled()) { // 有効の場合
            selectDevice();
        } else { // 無効の場合
            Intent btEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btEnableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mBluetoothAdapter.isEnabled() && mChatService == null) {
            setupController();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null) {
            mChatService.stop();
        }
    }

    /*
    * *********************************
    * コントローラの設定
    * *********************************
     */
    private void setupController() {

        mSteerSeekBar = (SeekBar) findViewById(R.id.seekBar_Steer);
        mThrottleSeekBar = (SeekBar) findViewById(R.id.seekBar_Throttle);

        // イベントリスナの登録（サーボモータ）
        mSteerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /* 複数byteで通信する場合 *
                if (progress == 0) {
                    mSteerValue = 7;
                } else {
                    mSteerValue = progress;
                }
                // */
                /* 1byteで通信する場合 */
                mSteerValue = progress;
                // */

                sendMessage(false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // nothing to do
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                /* 1byteで通信する場合 */
                mSteerValue = seekBar.getProgress();
                // */

                sendMessage(true);
            }
        });

        // イベントリスナの登録（DCモータ）
        mThrottleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /* 複数byteで通信する場合 *
                if (progress == 255) {
                    mThrottleValue = 0;
                } else if (progress > 255) {  // 前進
                    mThrottleValue = progress - MAX_THROTTLE;
                    mWay = 0;
                } else { // 後進
                    mThrottleValue = MAX_THROTTLE - progress;
                    mWay = 1;
                }
                // */
                /* 1byteで通信する場合 */
                mThrottleValue = progress;
                // */

                sendMessage(false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // nothing to do
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                /* 1byteで通信する場合 */
                mThrottleValue = seekBar.getProgress();
                // */

                sendMessage(true);
            }
        });

        // Bluetooth通信用のServiceの生成
        mChatService = new BluetoothChatService(this, mHandler);
    }

    /**
     * ************************************
     * イベントハンドラの設定
     * ************************************
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String deviceName = "" + msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), deviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: // BT有効確認後
                if (resultCode == Activity.RESULT_OK) {
                    selectDevice();
                } else {
                    Toast.makeText(this, "Bluetoothが有効になっていません", Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_CONNECT_DEVICE_SECURE:
                connectDevice(data);
                break;
        }
    }

    /**
     * **********************************
     * Bluetoothデバイスを選択する
     * **********************************
     */
    private void selectDevice() {
        Intent deviceListIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(deviceListIntent, REQUEST_CONNECT_DEVICE_SECURE);
    }

    /**
     * **********************************
     * Bluetoothデバイスに接続する
     * **********************************
     */
    private void connectDevice(Intent data) {
        // MACアドレスを取得
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // Bluetoothデバイスの取得
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // Bluetoothデバイスに接続
        mChatService.connect(device, true);

        Toast.makeText(this, "デバイスの取得完了", Toast.LENGTH_SHORT).show();
    }

    /**
     * **********************************
     * Bluetoothデバイスにデータを送信する
     * **********************************
     */
    private void sendMessage(boolean forced) {
        // Bluetooth接続の確認
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//            Toast.makeText(this, "Bluetooth接続エラー", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Bluetooth接続エラー");
            Log.d(TAG, "BT State : " + mChatService.getState());
            return;
        }

        // 送信メッセージの作成
        String message;
        /* 複数byteで通信する場合 *
        String steerMsg = String.format("%1$02d", mSteerValue);
        String wayMsg = String.valueOf(mWay);
        String throttleMsg = String.format("%1$03d", mThrottleValue);
        message = "h" + steerMsg + wayMsg + throttleMsg;
        // */
        /* 1byteで通信する場合 */
        byte[] data;
        data = new byte[1];
        data[0] = (byte) ((mSteerValue << 4) | (mThrottleValue));
        Log.d(TAG, "send data : " + data[0]);
        // */

        long currentTime = System.currentTimeMillis();
        if (forced || (currentTime - mLastCommandTime) > COMMAND_SPAN) {
            // メッセージを送信
            /* 複数byteで通信する場合 *
            byte[] send = message.getBytes();
            mChatService.write(send);
            // */
            /* 1byteで通信する場合 */
            mChatService.write(data);
            // */
        }

        // 最終命令時間を更新
        mLastCommandTime = currentTime;
//        Log.d(TAG, "command : " + message);
    }
}
