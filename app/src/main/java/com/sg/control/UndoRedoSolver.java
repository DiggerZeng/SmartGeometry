/*
 * 撤销恢复类
 * 该类用于图形的撤销及恢复操作
 * */
package com.sg.control;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import com.sg.object.graph.Graph;

public class UndoRedoSolver {

    private static UndoRedoSolver instance = new UndoRedoSolver();
    private Stack<UndoRedoStruct> UndoStack;
    private Stack<UndoRedoStruct> RedoStack;

    private UndoRedoSolver(){
        UndoStack = new Stack<UndoRedoStruct>();
        RedoStack = new Stack<UndoRedoStruct>();
    }

    public void EnUndoStack(ConcurrentHashMap<Long,Graph> graphList){
        UndoRedoStruct data = new UndoRedoStruct();
        UndoStack.push(data);
        Log.v("UndoStack", UndoStack.size() + "");
        saveFile(data, graphList);
    }

    private void saveFile(UndoRedoStruct data, ConcurrentHashMap<Long,Graph> graphList) {
        File file = new File(data.getPath());
        try {
            if(file.exists()) {
                file.delete();
            }
            file.createNewFile();
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
            os.writeObject(graphList);
            os.flush();
            os.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

//	public void EnRedoStack(UndoRedoStruct data){
//		RedoStack.push(data);
//	}

    public UndoRedoStruct popUndoStack() {
        RedoStack.push(UndoStack.peek());
        return UndoStack.pop();
    }

    public UndoRedoStruct peekUndoStack(){
        return UndoStack.peek();
    }

    public UndoRedoStruct popRedoStack() {
        UndoStack.push(RedoStack.peek());
        return RedoStack.pop();
    }

    public static UndoRedoSolver getInstance() {
        return instance;
    }

    public void RedoStackClear() {
        deleteFile(RedoStack);
        RedoStack.clear();
    }

    public void UndoStackClear() {
        deleteFile(UndoStack);
        UndoStack.clear();
    }

    private void deleteFile(Stack<UndoRedoStruct> stack) {
        File file;
        for (UndoRedoStruct struct : stack) {
            file = new File(struct.getPath());
            file.delete();
        }
    }

    public boolean isRedoStackEmpty() {
        return RedoStack.empty();
    }

    public boolean isUndoStackEmpty() {
        return UndoStack.empty();
    }

//	public Graph getFrontGraph(Graph graph){
//		Graph temp = null;
//		//找到改变前的图形
//		/*
//		for(UndoRedoStruct struct : UndoStack){
//			if(struct.getGraph().getID() == graph.getID()){
//				temp = struct.getGraph();
//			}
//		}
//		*/
//		//不需要全遍历一遍，只要从后向前找到第一个即可
//		int num = UndoStack.size();
//		for(int index = num - 1; index >= 0; index--){
//			temp = UndoStack.get(index).getGraph();
//			if(temp.getID() == graph.getID()){
//				return temp;
//			}
//		}
//		return temp;
//	}
}