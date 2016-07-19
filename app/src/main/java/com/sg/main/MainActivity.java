package com.sg.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TableLayout;
import android.widget.Toast;

import com.sg.bluetooth.BluetoothService;
import com.sg.bluetooth.DeviceListActivity;
import com.sg.bluetooth.SynchronousThread;
import com.sg.logic.common.CommonFunc;
import com.sg.property.R;
import com.sg.property.common.ThresholdProperty;

public class MainActivity extends Activity implements OnCheckedChangeListener{
    /** Called when the activity is first created. */

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean isBluetoothAvailable;
    private BluetoothService mBluetoothService = null;
    private SynchronousThread mSynchronousThread;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int FILE_EXPLORER_REQUEST = 10;
    private static final int CONNECT_DEVICE_REQUEST = 11;
    private static final int OPEN_BLUETOOTH_REQUEST = 12;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    private MainView mainView;

    private RadioGroup mRadioGroup;
    private RadioButton undo;
    private RadioButton redo;
    private RadioButton clear;
    private RadioButton save;
    private RadioButton open;
    private RadioButton pen;
    private RadioButton bluetooth;
    private RadioButton eraser;
    private RadioButton color;
    private RadioButton help;

    private HorizontalScrollView mHorizontalScrollView;//上面的水平滚动控件

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  //设置全屏
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //设置背光灯长亮
        initConfig();
//		mainView = new MainView(this);
        setContentView(R.layout.main);

        initMenu();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            isBluetoothAvailable = false;
        } else {
            isBluetoothAvailable = true;
//            if (!mBluetoothAdapter.isEnabled()) {
//            	mBluetoothAdapter.enable();
//        	}
        }

        mBluetoothService = new BluetoothService(mHandler);
        mSynchronousThread = new SynchronousThread(mBluetoothService);
        //mainView = new MainView(this, mSynchronousThread);
        mainView = (MainView) findViewById(R.id.myview);
        mainView.initSynchronousThread(mSynchronousThread);

        //读取关联文件
        Intent intent = getIntent();
        String action = intent.getAction();
        if(Intent.ACTION_VIEW.equals(action)){
            Uri uri = (Uri) intent.getData();
            String path = uri.getPath();
            mainView.open(path, false);
        }
    }

    private void initMenu() {
        mRadioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        LinearLayout.LayoutParams params = new
                LinearLayout.LayoutParams(ThresholdProperty.BUTTON_WIDTH, LayoutParams.WRAP_CONTENT);
        undo = (RadioButton)findViewById(R.id.undo);
        redo = (RadioButton)findViewById(R.id.redo);
        clear = (RadioButton)findViewById(R.id.clear);
        save = (RadioButton)findViewById(R.id.save);
        open = (RadioButton)findViewById(R.id.open);
        pen = (RadioButton)findViewById(R.id.pen);
        bluetooth = (RadioButton)findViewById(R.id.bluetooth);
        eraser = (RadioButton)findViewById(R.id.eraser);
        color = (RadioButton)findViewById(R.id.color);
        help = (RadioButton)findViewById(R.id.help);

        undo.setLayoutParams(params);
        redo.setLayoutParams(params);
        clear.setLayoutParams(params);
        save.setLayoutParams(params);
        open.setLayoutParams(params);
        bluetooth.setLayoutParams(params);
        eraser.setLayoutParams(params);
        pen.setLayoutParams(params);
        color.setLayoutParams(params);
        help.setLayoutParams(params);

        mHorizontalScrollView = (HorizontalScrollView)findViewById(R.id.horizontalScrollView);
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case FILE_EXPLORER_REQUEST:
                if(resultCode == RESULT_OK) {
                    String path = data.getStringExtra("path");
                    if(mainView.open(path, false)) {
                        Toast.makeText(this, "文件读取成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "文件读取失败", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case CONNECT_DEVICE_REQUEST:
                if(resultCode == RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case OPEN_BLUETOOTH_REQUEST:
                if(resultCode == RESULT_OK) {
                    chooseDevice();
                } else {

                }
            default:
                break;
        }
    }
    /*
     * 初始化 阈值配置文件
     * */
    private void initConfig() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        ThresholdProperty.set((float)dm.densityDpi / DisplayMetrics.DENSITY_MEDIUM, dm.widthPixels);
    }

    //返回键对话框
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            if (mHorizontalScrollView.getVisibility() == ViewGroup.GONE) {
                mHorizontalScrollView.setVisibility(ViewGroup.VISIBLE);
            } else {
                mHorizontalScrollView.setVisibility(ViewGroup.GONE);
            }

            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }

    private void clear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("清除图形");
        builder.setMessage("确认清除所有图形？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {
                mainView.clear();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }


    private void exit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出软件");
        builder.setMessage("确认退出？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {
                mainView.clear();
                closeBluetooth();
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void save(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存文件");
        LayoutInflater factory = LayoutInflater.from(this);
        final TableLayout saveForm = (TableLayout)factory.inflate(R.layout.save, null);
        builder.setView(saveForm);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText nameText = (EditText) saveForm.findViewById(R.id.editName);
                final String name = nameText.getText().toString();
                int state = mainView.save(name);
                if(state == 1) {
                    Toast.makeText(MainActivity.this, "文件保存成功", Toast.LENGTH_SHORT).show();
                }
                if(state == 2) {
                    replace(name);
                }
                if(state == 3) {
                    Toast.makeText(MainActivity.this, "文件保存失败", Toast.LENGTH_SHORT).show();
                }
                if(state == 4) {
                    Toast.makeText(MainActivity.this, "sdcard不存在或写保护", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        builder.create().show();
    }

    private void replace(final String name) {
        AlertDialog.Builder myBuilder = new AlertDialog.Builder(this);
        myBuilder.setTitle("警告");
        LayoutInflater factory = LayoutInflater.from(this);
        final TableLayout saveForm = (TableLayout)factory.inflate(R.layout.save, null);
        myBuilder.setView(saveForm);
        myBuilder.setMessage("文件已存在，确认覆盖？");
        myBuilder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //EditText nameText = (EditText) saveForm.findViewById(R.id.editName);
                //String name = nameText.getText().toString();
                mainView.replace(name);
                Toast.makeText(MainActivity.this, "文件已覆盖", Toast.LENGTH_SHORT).show();
            }
        });

        myBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "已取消覆盖，文件未保存",Toast.LENGTH_SHORT).show();
            }
        });
        myBuilder.create().show();
    }

    private void chooseDevice() {
        if (!isBluetoothAvailable) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.v("isOpenBlutTooth = 0;", "isOpenBlutTooth = 0;");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, OPEN_BLUETOOTH_REQUEST);
                return;
//            	mBluetoothAdapter.enable();
            }
//        	if (!mBluetoothAdapter.isEnabled()) {
//        		return;
//        	}

            if(mBluetoothService == null || mBluetoothService.getState() == BluetoothService.STATE_NONE
                    || mBluetoothService.getState() == BluetoothService.STATE_LISTEN) {
                Intent intent = new Intent(this,DeviceListActivity.class);
                startActivityForResult(intent, CONNECT_DEVICE_REQUEST);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //builder.setIcon(icon);
                builder.setTitle("关闭同步");
                builder.setMessage("确认关闭？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {
                        stopBluetoothService();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }

        }
    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        if(mBluetoothService == null) {
            mBluetoothService = new BluetoothService(mHandler);
        }
        mBluetoothService.start();
        mBluetoothService.connect(device);
    }

    private void closeBluetooth() {
        stopBluetoothService();
        if(mBluetoothAdapter != null)
            if(mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.disable();
    }

    private void stopBluetoothService() {
        if(mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Log.v("BluetoothService", "STATE_CONNECTED");
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Toast.makeText(MainActivity.this, "is connecting...",
                                    Toast.LENGTH_LONG).show();
                            break;
                        case BluetoothService.STATE_LISTEN:
                            Log.v("BluetoothService", "STATE_LISTEN");
                            break;
                        case BluetoothService.STATE_NONE:
                            Log.v("BluetoothService", "STATE_NONE");
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    mSynchronousThread.start();
                    mSynchronousThread.sendMessage("AB" + CommonFunc.getDriverWidth() + "E" + CommonFunc.getDriverHeight() + "EZ");
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    mainView.sendGraphList();
                    break;
                case MESSAGE_TOAST:
                    mSynchronousThread.pause();
                    Toast.makeText(MainActivity.this, msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // TODO Auto-generated method stub
        switch (checkedId) {
            case R.id.undo:
                mainView.Undo();
                undo.setChecked(false);
                break;
            case R.id.redo:
                mainView.Redo();
                redo.setChecked(false);
                break;
            case R.id.clear:
                clear();
                clear.setChecked(false);
                break;
            case R.id.save:
                save();
                save.setChecked(false);
                break;
            case R.id.open:
                open.setChecked(false);
                Intent intent = new Intent(this,FileExplorerActivity.class);
                startActivityForResult(intent, FILE_EXPLORER_REQUEST);
                break;
            case R.id.pen:
                if(mainView.isRecognize()) {
                    Toast.makeText(this, "图形识别已关闭", Toast.LENGTH_SHORT).show();
                    mainView.setRecognize(false);
                } else {
                    Toast.makeText(this, "图形识别已打开", Toast.LENGTH_SHORT).show();
                    mainView.setRecognize(true);
                }
                pen.setChecked(false);
                break;
            case R.id.bluetooth:
                chooseDevice();
                bluetooth.setChecked(false);
                break;
            //新增内容
            case R.id.eraser:
                if(mainView.alertEraser())
                    Toast.makeText(this, "选中图形已清除", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "无选中图形", Toast.LENGTH_SHORT).show();
                mainView.eraser();
                eraser.setChecked(false);
                break;
            case R.id.color:
                mainView.changeColor();
                color.setChecked(false);
                break;
            case R.id.help:
                help.setChecked(false);
                Intent intent2 = new Intent(this,Help.class);
                startActivity(intent2);
                break;
            default:
                break;
        }
    }


}