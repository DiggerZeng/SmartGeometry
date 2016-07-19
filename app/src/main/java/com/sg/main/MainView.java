package com.sg.main;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.sg.bluetooth.SynchronousThread;
import com.sg.control.FileService;
import com.sg.control.GraphControl;
import com.sg.control.OperationType;
import com.sg.control.UndoRedoSolver;
import com.sg.control.UndoRedoStruct;
import com.sg.logic.common.CommonFunc;
import com.sg.logic.common.VectorFunc;
import com.sg.logic.strategy.TranslationStratery;
import com.sg.object.Point;
import com.sg.object.graph.LineGraph;
import com.sg.object.graph.PointGraph;
import com.sg.object.graph.Sketch;
import com.sg.object.graph.Graph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.PointUnit;
import com.sg.object.unit.CurveUnit;
import com.sg.property.common.ThresholdProperty;
import com.sg.property.tools.Painter;
import com.sg.transformation.collection.PenInfo;
import com.sg.transformation.collection.PenInfoCollector;
import com.sg.transformation.computeagent.Constrainter;
import com.sg.transformation.computeagent.KeepConstrainter;
import com.sg.transformation.computeagent.Regulariser;
import com.sg.transformation.computeagent.UserIntentionReasoning;
import com.sg.transformation.recognizer.Recognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainView extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {

    private static final int SINGLE_POINT_TOUCH = 1;
    private static final int DOUBLE_POINT_TOUCH = 2;

    private SurfaceHolder mHolder;
    private boolean mLoop;

    private Graph curGraph;
    private Graph checkedGraph;
    private GUnit curUnit;
    private List<Point> pointList;

    private GraphControl graphControl;
    private PenInfoCollector collector; // 点信息收集器
    private Constrainter constrainter; // 图形约束
    private Regulariser regulariser; // 图形规整

    private long downTime;
    private long upTime;

    private boolean isEidt; // 编辑态 识别态
    //private boolean isRecognize; // 图形是否识别
    private boolean isChecked;
    private Canvas canvas;

    private UndoRedoSolver URSolver;

//	GraphControl graphControl;
//	private int color; // 画笔颜色 宽度
//	private int width;

    //private boolean isFirstUndoDelete; //是否是第一次undo 撤销删除

    private boolean isDoubleTouch;

    private FileService fileServicer;

    private KeepConstrainter keepConstrainter; //约束保持求解器

    private UserIntentionReasoning userIntentionReasoning; //用户意图推测器

    //2012-8-26 onion
    private Magnifier magnifier;

    //cai bluetooth
    private SynchronousThread mSynchronousThread;

    //cai 2013.9.14
    private boolean isRecognize;  //是否识别图形

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        setFocusable(true);
        mLoop = true; // 循环画图
        isEidt = false;
        isChecked = false;

        isDoubleTouch = false;

        isRecognize = true;


        curUnit = null;
        pointList = new ArrayList<Point>();

        graphControl = new GraphControl();
        collector = PenInfoCollector.getInstance();
        constrainter = Constrainter.getInstance();
        regulariser = Regulariser.getInstance();

        pointList = collector.getPenInfoList(); // 收集到的点信息，未处理过的

        URSolver = UndoRedoSolver.getInstance();

        fileServicer = new FileService();

        keepConstrainter = KeepConstrainter.getInstance();

        //mSynchronousThread = synchronousThread;

        userIntentionReasoning = new UserIntentionReasoning(context,
                regulariser, constrainter, keepConstrainter, URSolver, graphControl);

//		color = Color.BLACK; // 画笔颜色
//		width = 3; // 画笔宽度
        //graphControl = new GraphControl(graphList, color, width);

        //isFirstUndoDelete = true;

        //new Thread(this).start();

        //2012-8-26 onion
        magnifier = new Magnifier();

        //撤销恢复文件保存位置
        UndoRedoStruct.setAppPath(context.getFilesDir().getAbsolutePath());
    }

    protected void initSynchronousThread(SynchronousThread synchronousThread) {
        mSynchronousThread = synchronousThread;
        userIntentionReasoning.initSynchronousThread(mSynchronousThread);
        graphControl.initSynchronousThread(synchronousThread);
    }

    //***************************************
    public void eraser()
    {
        if (isChecked){
            graphControl.deleteGraph(curGraph);
            curGraph = null;
            isEidt = false;
            isChecked = false;
        }
    }

    public void changeColor()
    {
        graphControl.changeColor();
    }

    public boolean alertEraser()
    {
        return isChecked;
    }
    //***************************************

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mLoop = true; // 循环画图
        new Thread(this).start();

        //2012-8-26 onion
        CommonFunc.setDriverBound(this.getWidth(), this.getHeight());
        magnifier.Init();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mLoop = false;
        mSynchronousThread.stop();
    }

    @Override
    public void run() {
        while(mLoop) {
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized(mHolder) {  // 代表这个方法加锁
                onDraw();
            }
        }
    }

    protected void onDraw() {
        if(mHolder == null) {
            return;
        }

        //2012-8-26 onion
        //Canvas tCanvas = mHolder.lockCanvas();  // 锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了
        //原：canvas = mHolder.lockCanvas();  // 锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了

        canvas = magnifier.GetCacheCanvas();
        if(canvas == null) {
            return;
        }
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);    //画背景色
        if(curGraph instanceof Sketch){
            graphControl.drawGraph(curGraph, canvas); // 绘制当前勾画的草图
        }
        //graphControl.drawObj(curGraph, canvas); // 绘制当前勾画的草图
        //Log.v("draw size", graphList.size() + "");
        graphControl.drawGraphList(canvas); // 绘制已有的图像对象列表

        Canvas tCanvas = mHolder.lockCanvas();  // 锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了
        if(tCanvas == null) {
            return;
        }
        //2012-8-26 onion
        magnifier.Draw(tCanvas);

        //2012-8-26 onion
        mHolder.unlockCanvasAndPost(tCanvas); // 结束锁定画图，并提交改变
        //原：mHolder.unlockCanvasAndPost(canvas); // 结束锁定画图，并提交改变
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //isFirstUndoDelete = true;
        int action = event.getAction();
        if (MotionEvent.ACTION_CANCEL == action) {
            return false;
        }

        int multiPoint = event.getPointerCount();
        if (multiPoint == SINGLE_POINT_TOUCH) {

            //2012-8-26 onion
            magnifier.CollectPoint((int)event.getX(), (int)event.getY());
            if(event.getAction()==MotionEvent.ACTION_UP) {
                magnifier.EndShow();
            }
            //invalidate();	//调用完上面的一定要调用这个

            singleTouch(event);
        } else if (multiPoint == DOUBLE_POINT_TOUCH) {
            doubleTouch(event);
        }

        return true;
    }

    private void singleTouch(MotionEvent event) {
        int action = event.getAction();
        int touchX = (int) event.getX();
        int touchY = (int) event.getY();
        int num = 0;
        if(isEidt  && curGraph != null && !isDoubleTouch){  //编辑态
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
                    checkedGraph = curGraph;
                    isChecked = isEidt;
                    URSolver.RedoStackClear(); // 清空redo栈
                    downTime = new Date().getTime();
                    Point first = new Point(touchX, touchY);


                    Object[] objects = graphControl.getCurGraphCurUnit(curGraph, first);
                    centerGraph = (Graph) objects[0];
                    curUnit = (GUnit) objects[1];
                    if(centerGraph != null) {
                        curGraph = centerGraph;
                        checkedGraph = curGraph;
                    } else {
                        centerGraph = graphControl.getCenterGraph(first);
                        isEidt = false;  //如果点位置不在图形上 退出编辑态
                        curGraph = new Sketch(touchX, touchY);
                    }
                    if(centerGraph != null) {
                        checkedGraph = centerGraph;
                    }

                    collector.start(); // 启动收集器
                    collector.collect(touchX, touchY);
                    Log.v("onDown", touchX + ", " + touchY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    collector.collect(touchX, touchY); // 收集点
                    num = pointList.size();
                    //平移
                    float[][] transMatrix = {{1, 0, touchX - pointList.get(num - 2).getX()}, {0, 1, touchY - pointList.get(num - 2).getY()}, {0, 0, 1}};
                    if(curUnit != null){

                        //拖拽在直线上的点
                        if(((PointUnit)curUnit).isInLine()) {
//						//拖动切线的端点
//						if(curGraph instanceof CurveGraph)
//							;
//						else
//							//拖动三角形一般约束线的点
                            TranslationStratery.translatePointInLine(graphControl, curUnit,new Point(touchX, touchY));

                        } else {
                            if(((PointUnit)curUnit).isInCurve()) {
                                //拖拽在曲线上的点
                                TranslationStratery.translatePointInCurve(graphControl, curUnit, new Point(touchX, touchY));
                                keepConstrainter.keepInternallyTangentCircleOfTriangle(graphControl, curGraph);
                            } else {
                                if(curGraph instanceof TriangleGraph && ((TriangleGraph)curGraph).isCurveConstrainted()) {
                                    //拖动与圆外切的三角形的顶点
                                    curUnit.translate(transMatrix);
                                    keepConstrainter.keepInternallyTangentCircleOfTriangle(graphControl, curGraph);
                                }else {
                                    curGraph.setEqualAngleToF();
                                    curGraph.setRightAngleToF();
                                    curUnit.translate(transMatrix);
                                }
                            }

                        }

                        keepConstrainter.keepConstraint(curGraph);
                        if(mSynchronousThread.isStart()) {
                            mSynchronousThread.writeGraph(curGraph);
                        }
                        Log.v("拖拽点", "拖拽点");
                    }else{
//					curGraph.translate(curGraph, transMatrix);
                        graphControl.translateGraph(curGraph, transMatrix, 0);
                    }


//				//cai 2013.4.21 蓝牙传输
//				if(mSynchronousThread.isStart()) {
//					mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(curGraph));
//				}

                    Log.v("onMove", touchX + ", " + touchY);
                    Log.v("translate", (touchX - pointList.get(num - 2).getX()) + ", " + (touchY - pointList.get(num - 2).getY()));
                    break;
                case MotionEvent.ACTION_UP:
                    centerGraph = null;
                    Log.v("onUp", touchX + ", " + touchY);
                    //只有拖动顶点才重新进行图形规整,拖动直线上的点重新识别约束
                    if(curUnit != null) {
                        if(!((PointUnit)curUnit).isInLine() && !((PointUnit)curUnit).isInCurve()) {
                            if(curGraph instanceof TriangleGraph && ((TriangleGraph)curGraph).isCurveConstrainted())
                                ;
                            else
                                curGraph = regulariser.regularise(graphControl, curGraph);  //图形规整
                        } else {
                            if(((PointUnit)curUnit).isCommonConstrainted()) {
                                curGraph = keepConstrainter.rebuildTriangleConstraint(curGraph);
                            }
                        }
                    }
                    curUnit = null;

                    //cai 2013.4.21
//				if(!curGraph.isGraphConstrainted()) {
                    Graph tempGraph = constrainter.constraint(graphControl, curGraph);
                    if(tempGraph != null){   //动态约束
                        //动态约束线删除原来图形 再传约束后的图形
                        if(mSynchronousThread.isStart()) {
//							mSynchronousThread.writeDeleteGraph(curGraph);
                            mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(tempGraph));
                        }

                        if(tempGraph instanceof TriangleGraph && tempGraph.isGraphConstrainted() && curGraph instanceof LineGraph) {
                            //约束线用户意图推测
                            userIntentionReasoning.constraintReasoning(this,tempGraph, touchX, touchY);
                        }

                        curGraph = null;
                        checkedGraph = null;
                        isEidt = false;
                        isChecked = false;
                    }
//				}

                    if(curGraph != null){
                        keepConstrainter.keepConstraint(curGraph);
                        if(mSynchronousThread.isStart()) {
                            mSynchronousThread.writeGraph(curGraph);
                        }
                    }
                    //URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, curGraph.clone()));  //改变图形，undo栈添加
                    URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
                    URSolver.RedoStackClear(); //清空redo
                    collector.release(); // 释放收集器
                    break;
                default:
                    break;
            }

        }else{  //识别态
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
                    URSolver.RedoStackClear(); // 清空redo栈
                    downTime = new Date().getTime();
                    collector.start(); // 启动收集器
                    collector.collect(touchX, touchY);
                    curGraph = new Sketch(touchX, touchY);
                    Log.v("onDown", touchX + ", " + touchY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    //处理两点手势 一点弹起
                    if(isDoubleTouch){
                        event1Points.clear();
                        event2Points.clear();
                        collector.release();
                        curGraph = null;
                        isRotate = true;
                        isScale = true;
                        //isDoubleTouch = false;
                    }
                    collector.collect(touchX, touchY); // 收集点
                    if(curGraph != null){
                        curGraph.move(touchX, touchY);
                        //cai 2013.4.21
                        if(mSynchronousThread.isStart()) {
                            mSynchronousThread.sendMessage("AM" + touchX +"," + touchY + "EZ");
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    centerGraph = null;
                    //处理两点手势 弹起
                    if(isDoubleTouch){
                        isRotate = true;
                        isScale = true;
                        event1Points.clear();
                        event2Points.clear();
                        collector.release();
                        Log.v("两点手势", "两点手势");
                        isDoubleTouch = false;
                        if(isChecked){
                            isEidt = true;
                            isChecked = true;
                            curGraph = checkedGraph;
                            //cai 2013.4.21
                            if(!curGraph.isGraphConstrainted()) {
                                Graph tempGraph = constrainter.constraint(graphControl, curGraph);
                                if(tempGraph != null){   //动态约束
                                    //动态约束线删除原来图形 再传约束后的图形
                                    if(mSynchronousThread.isStart()) {
//									mSynchronousThread.writeDeleteGraph(curGraph);
                                        mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(tempGraph));
                                    }

                                    if(tempGraph instanceof TriangleGraph && tempGraph.isGraphConstrainted() && curGraph instanceof LineGraph) {
                                        //约束线用户意图推测
                                        userIntentionReasoning.constraintReasoning(this,tempGraph, touchX, touchY);
                                    }


                                    curGraph = null;
                                    checkedGraph = null;
                                    isEidt = false;
                                    isChecked = false;
                                }
                            }
//						if(curGraph != null){
//							//keepConstrainter.keepConstraint(curGraph);
//							if(mSynchronousThread.isStart()) {
//								mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(curGraph));
//							}
//						}
                        }
                        //URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, checkedGraph.clone())); //改变图形，undo栈添加
                        URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
                        //cai 2013.4.22
//					if(mSynchronousThread.isStart()) {
//						//mSynchronousThread.sendMessage("AU" + touchX +"," + touchY + "EZ");
//					}
                        break;
                    }
                    Log.v("onUp", touchX + ", " + touchY);
                    upTime = new Date().getTime();
                    PenInfo penInfo = new PenInfo(pointList);
                    //选中图形
                    if (upTime - downTime > ThresholdProperty.PRESS_TIME_SHORT
                            && penInfo.isFixedPoint()) { // 选中对象
                        Graph graph = graphControl.getCheckedGraph(new PointUnit(pointList).getPoint());
                        if (graph != null && graph != checkedGraph) {
                            //graph.setChecked(true); // 选中图形
                            if(isChecked){    //如果已有选中图形
                                Log.v("已选中图形", "已选中图形");
//							checkedGraph.setChecked(false);
                                graphControl.checkedGraph(checkedGraph, 0, false);

//							//cai 2013.4.24
//							if(mSynchronousThread.isStart()) {
//								mSynchronousThread.writeDisselectGraph(graphControl.getConstraintGraphs(checkedGraph));
//							}
//							URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, checkedGraph.clone())); //改变图形，undo栈添加
                            }
//						//cai 2013.4.24
//						if(mSynchronousThread.isStart()) {
//							mSynchronousThread.writeSelectGraph(graphControl.getConstraintGraphs(graph));
//						}
                            isEidt = true; // 进入编辑态
                            isChecked = true;
                            curGraph = graph;
                            checkedGraph = graph;
                            collector.release(); // 释放收集器
//						URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, curGraph.clone())); //改变图形，undo栈添加
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
                            URSolver.RedoStackClear(); //清空redo
                            //选中图形后显示用户意图推测
                            userIntentionReasoning.regulariseReasoning(this, graph, touchX, touchY);
//						//cai 2013.4.22
//						if(mSynchronousThread.isStart()) {
//							mSynchronousThread.sendMessage("AU" + touchX +"," + touchY + "EZ");
//						}
                            break;
                        }
                    }

                    //图形识别
                    Graph graph = graphControl.createGraph(penInfo.getNewPenInfo()); // 传入预处理后的点信息给图形工厂去识别，然后返回识别后的对象

                    //cai 2013.9.14  是否识别图形
                    if(!isRecognize) {
                        graph = null;
                    }

                    if (graph != null) {
                        curGraph = graph.clone();
                    }

                    //删除手势,撤销选中
                    if(isChecked){
                        isChecked = false;
                        //删除手势
//					checkedGraph.setChecked(false);
                        graphControl.checkedGraph(checkedGraph, 0, false);

//					//cai 2013.4.24
//					if(mSynchronousThread.isStart()) {
//						mSynchronousThread.writeDisselectGraph(graphControl.getConstraintGraphs(checkedGraph));
//					}
//					URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, checkedGraph.clone())); //改变图形，undo栈添加
                        //如果在选中图形外点一点 则识别为撤销选中
                        if(penInfo.isFixedPoint()){
                            URSolver.RedoStackClear(); //清空redo
                            curGraph = null;
                            checkedGraph = null;
                            collector.release(); // 释放收集器
                            //cai 2013.4.22
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.sendMessage("AU" + touchX +"," + touchY + "EZ");
                            }
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
                            break;
                        }
                        //如果是删除手势
                        if(Recognizer.isDeleteGesture(pointList)){
                            Log.v("删除图形", "删除图形");

//						//cai 2013.4.21
//						if(mSynchronousThread.isStart()) {
//							//mSynchronousThread.sendMessage("AU" + touchX +"," + touchY + "EZ");
//							mSynchronousThread.writeDeleteGraph(graphControl.getConstraintGraphs(checkedGraph));
//						}

//						graphControl.deleteGraph(checkedGraph);
                            graphControl.deleteConstraintedGraph(checkedGraph, 0);

//						URSolver.EnUndoStack(new UndoRedoStruct(OperationType.DELETE, checkedGraph.clone())); //改变图形，undo栈添加
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap());

                            URSolver.RedoStackClear(); //清空redo
                            curGraph = null;
                            checkedGraph = null;
                            collector.release(); // 释放收集器
                            break;
                        }else{
                            //URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, checkedGraph.clone())); //改变图形，undo栈添加
                        }
                    }

                    //不要识别点
                    if(curGraph instanceof PointGraph) {
                        URSolver.RedoStackClear(); //清空redo
                        curGraph = null;
                        checkedGraph = null;
                        collector.release(); // 释放收集器
                        break;
                    }

                    //cai 2013.4.21
                    Graph tempGraph = curGraph = regulariser.regularise(graphControl, curGraph);  //图形规整

                    //cai 2013.9.3
                    graphControl.addGraph(curGraph);


                    //图形约束
                    curGraph = constrainter.constraint(graphControl, curGraph);  // 对新构造的图形进一步约束识别
                    if(curGraph != null) { //有约束
                        if(curGraph instanceof TriangleGraph && curGraph.isGraphConstrainted() && graph instanceof LineGraph) {
                            //约束线用户意图推测
                            userIntentionReasoning.constraintReasoning(this,curGraph, touchX, touchY);
                        }
                        //cai 2013.4.21
                        tempGraph = curGraph;
                    }

                    //cai 2013.4.21
                    if(mSynchronousThread.isStart()) {
                        //mSynchronousThread.sendMessage("AU" + touchX +"," + touchY + "EZ");
                        mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(tempGraph));
                    }
                    URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
                    URSolver.RedoStackClear(); //清空redo
                    curGraph = null;
                    checkedGraph = null;
                    collector.release(); // 释放收集器
                    break;
                default:
                    break;
            }
        }
    }

    private List<Point> event1Points = new ArrayList<Point>();
    private List<Point> event2Points = new ArrayList<Point>();
    private static final int SAMPLE_COUNT = 5;   //每5点判断一次
    private int flagCount = 0;
    //private Point AB; // 向量AB
    private boolean isNarrowOrEnlarge ;   //放大 or 缩小
    private float cosA = 0, sinA = 0;
    private boolean isclockwise;       //顺时针 or 逆时针
    //private boolean isTouchUp = true;
    //private int a = 0, b = 1;

    //cwq
//	private CurveUnit centerCurve = null;


    private Graph centerGraph = null;
    private boolean isRotate = true, isScale = true;
    /*
     * 双点触摸， 缩放手势，旋转手势，删除手势，还未完成，暂时不用去管
     */
    private void doubleTouch(MotionEvent event) {
        isDoubleTouch = true;
        if(isChecked){
            isEidt = false;

            int action = event.getAction();
            int firstX = (int) event.getX(0);
            int firstY = (int) event.getY(0);
            int secondX = (int) event.getX(1);
            int secondY = (int) event.getY(1);

            //Log.v("1,2",firstX+","+firstY+"   "+secondX+","+secondY);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
                    collector.release();
                    break;
                case MotionEvent.ACTION_MOVE:
                    event1Points.add(new Point(firstX, firstY));
                    event2Points.add(new Point(secondX, secondY));
                    if (event1Points.size() == SAMPLE_COUNT) {
                        if (flagCount != 0) { // 首部5个采样点不处理
                            Point baseVector = VectorFunc.subtract(event2Points.get(0),
                                    event1Points.get(0));
                            Point vector1 = VectorFunc.subtract(
                                    event1Points.get(SAMPLE_COUNT - 1),
                                    event1Points.get(0));
                            Point vector2 = VectorFunc.subtract(
                                    event2Points.get(SAMPLE_COUNT - 1),
                                    event2Points.get(0));
                            isNarrowOrEnlarge = CommonFunc.NarrowOrEnlarge(event1Points.get(0), event2Points.get(0), event1Points.get(SAMPLE_COUNT - 1), event2Points.get(SAMPLE_COUNT - 1));
                            int type = VectorFunc.translation(baseVector, vector1, vector2);

                            Point translationCenter;
                            if(centerGraph != null) {
                                translationCenter = TranslationStratery.findTranslationCenter(centerGraph);
                            } else {
                                translationCenter = TranslationStratery.findTranslationCenter(checkedGraph);
                            }
                            if (type == 1 && isScale) {
                                isRotate = false;
                                if(isNarrowOrEnlarge){
                                    float[][] transMatrixscalenarrow = {{(float) 0.9, 0, 0},
                                            {0, (float) 0.9, 0},
                                            {0, 0, 1}}; //缩小矩阵
//									checkedGraph.scale(checkedGraph, transMatrixscalenarrow, centerCurve);
                                    graphControl.scaleGraph(checkedGraph, transMatrixscalenarrow, translationCenter, 0);
                                }else{
                                    float[][] transMatrixscaleenlarge = {{(float) 1.13, 0, 0},
                                            {0, (float) 1.13, 0},
                                            {0, 0, 1}};	//放大矩阵
//									checkedGraph.scale(checkedGraph, transMatrixscaleenlarge, centerCurve);
                                    graphControl.scaleGraph(checkedGraph, transMatrixscaleenlarge, translationCenter, 0);
                                }
                                if(centerGraph != null) {
                                    keepConstrainter.keepCurveConstraint(checkedGraph, centerGraph);
                                }
//								//cai 2013.4.22
//								if(mSynchronousThread.isStart()) {
//									mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(checkedGraph));
//								}
                                Log.v("translate", "scale");
                            } else if (type == 2 && isRotate) {
                                isScale = false;
                                cosA = (float) CommonFunc.rotatecos(baseVector, vector1, vector2);
                                sinA = (float) Math.sqrt(1-cosA*cosA);
                                isclockwise = CommonFunc.isClockWise(baseVector, vector1, vector2);
                                if(!isclockwise){
                                    float[][] rotateMatrix = {{cosA, -sinA, 0},
                                            {sinA, cosA, 0},
                                            {0, 0, 1}};	 //逆时针旋转矩阵
//									checkedGraph.rotate(checkedGraph, rotateMatrix, centerCurve);
                                    graphControl.rotateGraph(checkedGraph, rotateMatrix, translationCenter, 0);
                                    Log.v("translate", "alongrotate");
                                }else{
                                    float[][] rotateMatrix = {{cosA, sinA, 0},
                                            {-sinA, cosA, 0},
                                            {0, 0, 1}};	//顺时针旋转矩阵
//									checkedGraph.rotate(checkedGraph, rotateMatrix, centerCurve);
                                    graphControl.rotateGraph(checkedGraph, rotateMatrix, translationCenter, 0);
                                    Log.v("translate", "wiserrotate");
                                }
//								//cai 2013.4.22
//								if(mSynchronousThread.isStart()) {
//									mSynchronousThread.sendMessage(graphControl.getConstraintGraphs(checkedGraph));
//								}
                            } else {
                                Log.v("translate", "null");
                            }
                        }
                        //cai 貌似放大缩小，旋转不要保持约束
                        //keepConstrainter.keepConstraint(checkedGraph);
                        flagCount++;
                        event1Points.clear();
                        event2Points.clear();
                        collector.release();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(mSynchronousThread.isStart()) {
                        mSynchronousThread.sendMessage("AU" + secondX +"," + secondY + "EZ");
                    }
                    event1Points.clear();
                    event2Points.clear();
                    collector.release();
                    centerGraph = null;
                    isRotate = true;
                    isScale = true;
//					isTouchUp = true;
                    break;
                default:
                    break;
            }
        }
    }

    //cai 2013.4.22
    public void sendGraphList() {
        if(mSynchronousThread.isStart()) {
            mSynchronousThread.sendMessage("AXZ");
            mSynchronousThread.sendMessage(graphControl.getGraphList());
        }
    }


    public boolean isRecognize() {
        return isRecognize;
    }

    public void setRecognize(boolean isRecognize) {
        this.isRecognize = isRecognize;
    }

    //功能
    public void clear(){
        if(mSynchronousThread.isStart()) {
            mSynchronousThread.sendMessage("AXZ");
        }
        userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
        isEidt = false;
        isChecked = false;
        curGraph = null;
        checkedGraph = null;
        //isFirstUndoDelete = true;

        graphControl.clearGraph();
        URSolver.RedoStackClear();  //清空reod undo栈
        URSolver.UndoStackClear();
//    	URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
    }

    public void Undo() {
        userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
        curGraph = null;
        if(URSolver.isUndoStackEmpty()) {
            Log.v("Undo", "empty");
            return;
        }
        UndoRedoStruct temp = URSolver.popUndoStack();
        if(!URSolver.isUndoStackEmpty()) {
            temp = URSolver.peekUndoStack();
            open(temp.getPath(), true);
        } else {
            isEidt = false;
            isChecked = false;
            curGraph = null;
            checkedGraph = null;
            graphControl.clearGraph();
        }
        sendGraphList();
    }

    public void Redo() {
        userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
        if(URSolver.isRedoStackEmpty()) {
            Log.v("Redo", "empty");
            return;
        }
        UndoRedoStruct temp = URSolver.popRedoStack();
        open(temp.getPath(), true);
        sendGraphList();
    }

    public int save(String name){
        userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
        return fileServicer.save(graphControl.getConcurrentHashMap(), name);
    }

    public void replace(String name) {
        userIntentionReasoning.dismiss();
        fileServicer.replace(graphControl.getConcurrentHashMap(), name);
    }


    public boolean open(String path, boolean isUndo){
        Object temp = fileServicer.read(path);
        if(temp != null){
            userIntentionReasoning.dismiss(); //如果没点选图标，则popupwindow消失
            graphControl.setConcurrentHashMap((ConcurrentHashMap<Long,Graph>)temp);
            if(!isUndo) {
                URSolver.RedoStackClear();  //清空reod undo栈
                URSolver.UndoStackClear();
                URSolver.EnUndoStack(graphControl.getConcurrentHashMap());
            }
            if(mSynchronousThread.isStart()) {
                sendGraphList();
            }
            for(Graph graph : graphControl.getGraphList()){
                if(graph.isChecked()){
                    isEidt = true;
                    isChecked = true;
                    curGraph = graph;
                    checkedGraph = graph;
                    return true;
                }
            }
            isEidt = false;
            isChecked = false;
            curGraph = null;
            checkedGraph = null;
            return true;
        } else {
            return false;
        }
    }
}
