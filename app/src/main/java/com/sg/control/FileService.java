/*
 * 使用Java串行化技术存储图形对象列表
 * 该类需扩展，文件存储读取功能都可以，稍微修改即可满足项目所需的要求
 * */

package com.sg.control;

import android.os.Environment;
import android.util.Log;

import com.sg.object.graph.Graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {

    private static final String SUFFIX = ".sg";
    private static final String MYFILE = "/SG";
//	private Context context;
//
//	public FileService(Context context) {
//		this.context = context;
//	}

    public FileService() {

    }

    /**
     * 1文件保存成功 2文件已存在 3文件保存失败 4sdcard不存在或写保护
     * @param graphList
     * @param name
     * @return
     */
    public int save(ConcurrentHashMap<Long,Graph> graphList, String name) {
        if(!name.endsWith(SUFFIX)) {   //后缀名
            name += SUFFIX;
        }
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //sd卡位置
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            //String path = new File(Environment.getExternalStorageDirectory(), name).getAbsolutePath();
            //文件夹
            File myFile = new File(sdPath + MYFILE);
            if(!myFile.exists()){
         //       myFile.mkdirs();
            }
            //Log.v("path", path);
            ObjectOutputStream os = null;
            //File file = new File(path);
            File file = new File(myFile, name);
            try {
                if(!file.exists()) {
            //        file.createNewFile();
                } else {
//					Toast.makeText(context, "文件已存在", Toast.LENGTH_SHORT).show();
                    return 2;
                }
//				fos = context.openFileOutput(path, Context.MODE_PRIVATE);
                os = new ObjectOutputStream(new FileOutputStream(file));
                os.writeObject(graphList);
                os.flush();
                os.close();          //读取文件流用完之后一定要记得关闭，否则就会造成内存泄露
//				Toast.makeText(context, "文件保存成功", Toast.LENGTH_SHORT).show();
                return 1;
            } catch (Exception e) {
                Log.e(e.toString(), e.toString());
//				Toast.makeText(context, "文件保存失败", Toast.LENGTH_SHORT).show();
                return 3;
            }
        } else {
//			 Toast.makeText(context, "sdcard不存在或写保护", Toast.LENGTH_SHORT).show();
            return 4;
        }
    }

    public void replace(ConcurrentHashMap<Long, Graph> graphList, String name) {
        if(!name.endsWith(SUFFIX)) {
            name += SUFFIX;  //后缀名
        }
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myFile = new File(sdPath + MYFILE);
            ObjectOutputStream os = null;
            File file = new File(myFile, name);
            try {
                if (file.exists()) {
                    file.delete();
                    os = new ObjectOutputStream(new FileOutputStream(file));
                    os.writeObject(graphList);
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                Log.e(e.toString(), e.toString());
            }
        }
    }

    public Object read(String path) {
        Object object = null;
        ObjectInputStream in = null;
        File file = new File(path);
        if(!file.exists()) {     //文件不存在
//			Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            in = new ObjectInputStream(new FileInputStream(file));
            object = in.readObject();
            in.close();          //读取文件流用完之后一定要记得关闭，否则就会造成内存泄露
        } catch (Exception e) {
            Log.e(e.toString(), e.toString());
//			Toast.makeText(context, "文件读取失败", Toast.LENGTH_SHORT).show();
            return null;
        }
//		Toast.makeText(context, "文件读取成功", Toast.LENGTH_SHORT).show();
        return object;
    }
}
