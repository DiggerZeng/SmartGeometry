package com.sg.control;

public class UndoRedoStruct {

    private String path;

    private static String APP_PATH;

    private static int id = 0;

    public UndoRedoStruct() {
        path = APP_PATH + id;
        id++;
    }

    public String getPath() {
        return path;
    }

    public static String getAppPath() {
        return APP_PATH;
    }

    public static void setAppPath(String appPath) {
        APP_PATH = appPath + "/";
    }

//	private OperationType operationType;
//	private Graph graph;
//
//	public UndoRedoStruct(){
//		operationType = OperationType.NONE;
//		graph = null;
//	}
//
//	public UndoRedoStruct(OperationType operationType, Graph graph) {
//		super();
//		this.operationType = operationType;
//		this.graph = graph;
//	}
//
//
//	public OperationType getOperationType() {
//		return operationType;
//	}
//
//	public Graph getGraph() {
//		return graph;
//	}
//
//	public void setOperationType(OperationType data) {
//		this.operationType = data;
//	}
//
//	public void setGraph(Graph graph) {
//		this.graph = graph;
//	}
}