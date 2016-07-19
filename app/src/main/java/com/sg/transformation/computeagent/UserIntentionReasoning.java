package com.sg.transformation.computeagent;
/*
 * 用户意图推测器
 */
import java.util.List;

import com.sg.bluetooth.SynchronousThread;
import com.sg.control.GraphControl;
import com.sg.control.OperationType;
import com.sg.control.UndoRedoSolver;
import com.sg.control.UndoRedoStruct;
import com.sg.logic.common.CommonFunc;
import com.sg.object.graph.Graph;
import com.sg.object.graph.RectangleGraph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.property.R;
import com.sg.property.common.ThresholdProperty;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

public class UserIntentionReasoning {
    private LayoutInflater mLayoutInflater;
    private View popView;
    private PopupWindow mPop;
    private Context context;
    private Graph myGraph;

    private Regulariser regulariser; // 图形规整
    private KeepConstrainter keepConstrainter; //约束保持求解器
    private UndoRedoSolver URSolver;
    private LinearCloseConstraint linearCloseConstraint;

    //2013.2.22 cai bluetooth
    private SynchronousThread mSynchronousThread;

    private GraphControl graphControl;

    public UserIntentionReasoning(Context context, Regulariser regulariser,
                                  Constrainter constrainter, KeepConstrainter keepConstrainter, UndoRedoSolver URSolver, GraphControl graphControl) {
        this.context = context;
        this.regulariser = regulariser;
        this.keepConstrainter = keepConstrainter;
        this.URSolver = URSolver;
        this.graphControl = graphControl;
        linearCloseConstraint = new LinearCloseConstraint();
        mLayoutInflater = (LayoutInflater)
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void initSynchronousThread(SynchronousThread synchronousThread) {
        mSynchronousThread = synchronousThread;
    }

    public void regulariseReasoning(View parent, Graph graph, int x, int y) {
        myGraph = graph;
        if(graph instanceof TriangleGraph && !((TriangleGraph) graph).isCurveConstrainted()){
            popView =  mLayoutInflater.inflate(R.layout.popup_for_tri, null);
            mPop = new PopupWindow(popView, ThresholdProperty.TRI_WIDTH, ThresholdProperty.POP_HEIGTH);
            mPop.setOutsideTouchable(true);
            popView.findViewById(R.id.isoceles_tri).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            changeToIsocelesTri(myGraph);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });

            popView.findViewById(R.id.right_angled_tri).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            PointUnit point1 = (PointUnit)(myGraph.getGraph().get(0));
                            PointUnit point2 = (PointUnit)(myGraph.getGraph().get(2));
                            PointUnit point3 = (PointUnit)(myGraph.getGraph().get(4));
                            double distance1 = CommonFunc.distance(point1,point2);
                            double distance2 = CommonFunc.distance(point2,point3);
                            double distance3 = CommonFunc.distance(point3,point1);
                            double cos1 = (CommonFunc.square(distance1)+CommonFunc.square(distance3)-CommonFunc.square(distance2)) / (2 * distance1 * distance3);
                            double cos2 = (CommonFunc.square(distance1)+CommonFunc.square(distance2)-CommonFunc.square(distance3)) / (2 * distance1 * distance2);
                            double cos3 = (CommonFunc.square(distance2)+CommonFunc.square(distance3)-CommonFunc.square(distance1)) / (2 * distance2 * distance3);
                            if(Math.abs(cos1) <= Math.abs(cos2) && Math.abs(cos1) <= Math.abs(cos3)){
                                regulariser.changeToApeak(point3 , point1 , point2);
                            }
                            if(Math.abs(cos2) <= Math.abs(cos1) && Math.abs(cos2) <= Math.abs(cos3)){
                                regulariser.changeToApeak(point1 , point2 , point3);
                            }
                            if(Math.abs(cos3) <= Math.abs(cos1) && Math.abs(cos3) <= Math.abs(cos2)){
                                regulariser.changeToApeak(point2 , point3 , point1);
                            }
                            Log.v("直角三角形","意图推测");
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap());; //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.right_angled_isosceles_tri).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            PointUnit point1 = (PointUnit)(myGraph.getGraph().get(0));
                            PointUnit point2 = (PointUnit)(myGraph.getGraph().get(2));
                            PointUnit point3 = (PointUnit)(myGraph.getGraph().get(4));
                            double distance1 = CommonFunc.distance(point1,point2);
                            double distance2 = CommonFunc.distance(point2,point3);
                            double distance3 = CommonFunc.distance(point3,point1);
                            double cos1 = (CommonFunc.square(distance1)+CommonFunc.square(distance3)-CommonFunc.square(distance2)) / (2 * distance1 * distance3);
                            double cos2 = (CommonFunc.square(distance1)+CommonFunc.square(distance2)-CommonFunc.square(distance3)) / (2 * distance1 * distance2);
                            double cos3 = (CommonFunc.square(distance2)+CommonFunc.square(distance3)-CommonFunc.square(distance1)) / (2 * distance2 * distance3);
                            if(Math.abs(cos1) <= Math.abs(cos2) && Math.abs(cos1) <= Math.abs(cos3)){
                                point1.setRightAngle(true);
                                regulariser.changeToEquation(point3 , point1 , point2);
                            }
                            if(Math.abs(cos2) <= Math.abs(cos1) && Math.abs(cos2) <= Math.abs(cos3)){
                                point2.setRightAngle(true);
                                regulariser.changeToEquation(point1 , point2 , point3);
                            }
                            if(Math.abs(cos3) <= Math.abs(cos1) && Math.abs(cos3) <= Math.abs(cos2)){
                                point3.setRightAngle(true);
                                regulariser.changeToEquation(point2 , point3 , point1);
                            }
                            Log.v("等腰直角三角形","意图推测");
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.equilateral_tri).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            ((PointUnit)(myGraph.getGraph().get(2))).setEqualAngle(true);
                            regulariser.changeToEquation((PointUnit)(myGraph.getGraph().get(0)) , (PointUnit)(myGraph.getGraph().get(2)) ,
                                    (PointUnit)(myGraph.getGraph().get(4)));
                            Log.v("等边三角形","意图推测");
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();//cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            mPop.showAtLocation(parent,  Gravity.LEFT | Gravity.TOP, x, y);
        }
        if(graph instanceof RectangleGraph){
            popView =  mLayoutInflater.inflate(R.layout.popup_for_rect, null);
            mPop = new PopupWindow(popView, ThresholdProperty.REC_WIDTH, ThresholdProperty.POP_HEIGTH);
            mPop.setOutsideTouchable(true);
            popView.findViewById(R.id.parallelogram).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            Log.v("普通平行四边形",	1+ ","+1);
                            ((PointUnit)(myGraph.getGraph().get(6))).setX((int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getX()+((PointUnit)(myGraph.getGraph().get(4))).getX()-((PointUnit)(myGraph.getGraph().get(2))).getX())));
                            ((PointUnit)(myGraph.getGraph().get(6))).setY((int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getY()+((PointUnit)(myGraph.getGraph().get(4))).getY()-((PointUnit)(myGraph.getGraph().get(2))).getY())));
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.rect).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            regulariser.changeToApeak((PointUnit)(myGraph.getGraph().get(0)) , (PointUnit)(myGraph.getGraph().get(2)) ,
                                    (PointUnit)(myGraph.getGraph().get(4)));
                            ((PointUnit)(myGraph.getGraph().get(6))).setX((int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getX()+((PointUnit)(myGraph.getGraph().get(4))).getX()-((PointUnit)(myGraph.getGraph().get(2))).getX())));
                            ((PointUnit)(myGraph.getGraph().get(6))).setY( (int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getY()+((PointUnit)(myGraph.getGraph().get(4))).getY()-((PointUnit)(myGraph.getGraph().get(2))).getY())));
                            ((PointUnit)(myGraph.getGraph().get(0))).setRightAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(4))).setRightAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(6))).setRightAngle(true);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.squre).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            ((PointUnit)(myGraph.getGraph().get(2))).setRightAngle(true);
                            regulariser.changeToEquation((PointUnit)(myGraph.getGraph().get(0)) , (PointUnit)(myGraph.getGraph().get(2)) ,
                                    (PointUnit)(myGraph.getGraph().get(4)));
                            ((PointUnit)(myGraph.getGraph().get(6))).setX((int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getX()+((PointUnit)(myGraph.getGraph().get(4))).getX()-((PointUnit)(myGraph.getGraph().get(2))).getX())));
                            ((PointUnit)(myGraph.getGraph().get(6))).setY( (int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getY()+((PointUnit)(myGraph.getGraph().get(4))).getY()-((PointUnit)(myGraph.getGraph().get(2))).getY())));
                            ((PointUnit)(myGraph.getGraph().get(0))).setRightAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(4))).setRightAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(6))).setRightAngle(true);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.diamond).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            regulariser.changeToEquation((PointUnit)(myGraph.getGraph().get(0)) , (PointUnit)(myGraph.getGraph().get(2)) ,
                                    (PointUnit)(myGraph.getGraph().get(4)));
                            ((PointUnit)(myGraph.getGraph().get(6))).setX((int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getX()+
                                    ((PointUnit)(myGraph.getGraph().get(4))).getX()-((PointUnit)(myGraph.getGraph().get(2))).getX())));
                            ((PointUnit)(myGraph.getGraph().get(6))).setY( (int)(Math.round(((PointUnit)(myGraph.getGraph().get(0))).getY()+
                                    ((PointUnit)(myGraph.getGraph().get(4))).getY()-((PointUnit)(myGraph.getGraph().get(2))).getY())));
                            ((PointUnit)(myGraph.getGraph().get(0))).setEqualAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(2))).setEqualAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(4))).setEqualAngle(true);
                            ((PointUnit)(myGraph.getGraph().get(6))).setEqualAngle(true);
                            Log.v("等边三角形","意图推测");
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.trapezoid).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            PointUnit point1 = (PointUnit)(myGraph.getGraph().get(0));
                            PointUnit point2 = (PointUnit)(myGraph.getGraph().get(2));
                            PointUnit point3 = (PointUnit)(myGraph.getGraph().get(4));
                            PointUnit point4 = (PointUnit)(myGraph.getGraph().get(6));
                            double distance1 = CommonFunc.distance(point1,point2);
                            double distance2 = CommonFunc.distance(point2,point3);
                            double distance3 = CommonFunc.distance(point3,point4);
                            double distance4 = CommonFunc.distance(point4,point1);
                            double catercorner1 = CommonFunc.distance(point1,point3);
                            double catercorner2 = CommonFunc.distance(point2,point4);
                            double cos1 = (CommonFunc.square(distance1)+CommonFunc.square(distance4)-CommonFunc.square(catercorner2))/(2*distance1*distance4);
                            double cos2 = (CommonFunc.square(distance1)+CommonFunc.square(distance2)-CommonFunc.square(catercorner1))/(2*distance1*distance2);
                            double cos3 = (CommonFunc.square(distance2)+CommonFunc.square(distance3)-CommonFunc.square(catercorner2))/(2*distance2*distance3);
                            if(cos1 > 0){
                                if(distance1 > distance4){
                                    regulariser.beTrapezium(point4 , point1 , point2 , point3 , true);
                                }else{
                                    regulariser.beTrapezium(point2 , point1 , point4 , point3 , true);
                                }
                            }else if(cos2 > 0){
                                if(distance1 > distance2){
                                    regulariser.beTrapezium(point3 , point2 , point1 , point4 , true);
                                }else{
                                    regulariser.beTrapezium(point1 , point2 , point3 , point4 , true);
                                }
                            }else if(cos3 > 0){
                                if(distance2 > distance3){
                                    regulariser.beTrapezium(point4 , point3, point2 , point1 , true);
                                }else{
                                    regulariser.beTrapezium(point2 , point3 , point4 , point1 , true);
                                }
                            }else{
                                if(distance3 > distance4){
                                    regulariser.beTrapezium(point1 , point4 , point3 , point2 , true);
                                }else{
                                    regulariser.beTrapezium(point3 , point4 , point1 , point2 , true);
                                }
                            }
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.right_trap).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            PointUnit point1 = (PointUnit)(myGraph.getGraph().get(0));
                            PointUnit point2 = (PointUnit)(myGraph.getGraph().get(2));
                            PointUnit point3 = (PointUnit)(myGraph.getGraph().get(4));
                            PointUnit point4 = (PointUnit)(myGraph.getGraph().get(6));
                            double distance1 = CommonFunc.distance(point1,point2);
                            double distance2 = CommonFunc.distance(point2,point3);
                            double distance3 = CommonFunc.distance(point3,point4);
                            double distance4 = CommonFunc.distance(point4,point1);
                            double catercorner1 = CommonFunc.distance(point1,point3);
                            double catercorner2 = CommonFunc.distance(point2,point4);
                            double cos1 = (CommonFunc.square(distance1)+CommonFunc.square(distance4)-CommonFunc.square(catercorner2))/(2*distance1*distance4);
                            double cos2 = (CommonFunc.square(distance1)+CommonFunc.square(distance2)-CommonFunc.square(catercorner1))/(2*distance1*distance2);
                            double cos3 = (CommonFunc.square(distance2)+CommonFunc.square(distance3)-CommonFunc.square(catercorner2))/(2*distance2*distance3);
                            if(cos1 > 0){
                                if(distance1 > distance4){
                                    regulariser.changeToApeak(point4 , point1 , point2);
                                    regulariser.beTrapezium(point4 , point1 , point2 , point3 , true);
                                }else{
                                    regulariser.changeToApeak(point2 , point1 , point4);
                                    regulariser.beTrapezium(point2 , point1 , point4 , point3 , true);
                                }
                            }else if(cos2 > 0){
                                if(distance1 > distance2){
                                    regulariser.changeToApeak(point3 , point2 , point1);
                                    regulariser.beTrapezium(point3 , point2 , point1 , point4 , true);
                                }else{
                                    regulariser.changeToApeak(point1 , point2 , point3);
                                    regulariser.beTrapezium(point1 , point2 , point3 , point4 , true);
                                }
                            }else if(cos3 > 0){
                                if(distance2 > distance3){
                                    regulariser.changeToApeak(point4 , point3 , point2);
                                    regulariser.beTrapezium(point4 , point3, point2 , point1 , true);
                                }else{
                                    regulariser.changeToApeak(point2 , point3 , point4);
                                    regulariser.beTrapezium(point2 , point3 , point4 , point1 , true);
                                }
                            }else{
                                if(distance3 > distance4){
                                    regulariser.changeToApeak(point1 , point4 , point3);
                                    regulariser.beTrapezium(point1 , point4 , point3 , point2 , true);
                                }else{
                                    regulariser.changeToApeak(point3 , point4 , point1);
                                    regulariser.beTrapezium(point3 , point4 , point1 , point2 , true);
                                }
                            }
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.isosceles_trap).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            myGraph.setEqualAngleToF();
                            myGraph.setRightAngleToF();
                            PointUnit point1 = (PointUnit)(myGraph.getGraph().get(0));
                            PointUnit point2 = (PointUnit)(myGraph.getGraph().get(2));
                            PointUnit point3 = (PointUnit)(myGraph.getGraph().get(4));
                            PointUnit point4 = (PointUnit)(myGraph.getGraph().get(6));
                            double distance1 = CommonFunc.distance(point1,point2);
                            double distance2 = CommonFunc.distance(point2,point3);
                            double distance3 = CommonFunc.distance(point3,point4);
                            double distance4 = CommonFunc.distance(point4,point1);
                            double catercorner1 = CommonFunc.distance(point1,point3);
                            double catercorner2 = CommonFunc.distance(point2,point4);
                            double cos1 = (CommonFunc.square(distance1)+CommonFunc.square(distance4)-CommonFunc.square(catercorner2))/(2*distance1*distance4);
                            double cos2 = (CommonFunc.square(distance1)+CommonFunc.square(distance2)-CommonFunc.square(catercorner1))/(2*distance1*distance2);
                            double cos3 = (CommonFunc.square(distance2)+CommonFunc.square(distance3)-CommonFunc.square(catercorner2))/(2*distance2*distance3);
                            if(cos1 > 0){
                                if(distance1 > distance4){
                                    point1.setEqualAngle(true);
                                    point2.setEqualAngle(true);
                                    regulariser.beTrapezium(point4 , point1 , point2 , point3 , true);
                                }else{
                                    point1.setEqualAngle(true);
                                    point4.setEqualAngle(true);
                                    regulariser.beTrapezium(point2 , point1 , point4 , point3 , true);
                                }
                            }else if(cos2 > 0){
                                if(distance1 > distance2){
                                    point1.setEqualAngle(true);
                                    point2.setEqualAngle(true);
                                    regulariser.beTrapezium(point3 , point2 , point1 , point4 , true);
                                }else{
                                    point2.setEqualAngle(true);
                                    point3.setEqualAngle(true);
                                    regulariser.beTrapezium(point1 , point2 , point3 , point4 , true);
                                }
                            }else if(cos3 > 0){
                                if(distance2 > distance3){
                                    point2.setEqualAngle(true);
                                    point3.setEqualAngle(true);
                                    regulariser.beTrapezium(point4 , point3, point2 , point1 , true);
                                }else{
                                    point3.setEqualAngle(true);
                                    point4.setEqualAngle(true);
                                    regulariser.beTrapezium(point2 , point3 , point4 , point1 , true);
                                }
                            }else{
                                if(distance3 > distance4){
                                    point3.setEqualAngle(true);
                                    point4.setEqualAngle(true);
                                    regulariser.beTrapezium(point1 , point4 , point3 , point2 , true);
                                }else{
                                    point1.setEqualAngle(true);
                                    point4.setEqualAngle(true);
                                    regulariser.beTrapezium(point3 , point4 , point1 , point2 , true);
                                }
                            }
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            mPop.showAtLocation(parent,  Gravity.LEFT | Gravity.TOP, x, y);
        }
    }

    //约束用户意图推测
    public void constraintReasoning(View parent, Graph graph, int x, int y) {
        myGraph = graph;
        List<GUnit> units = myGraph.getGraph();
        int size = units.size();
        //获取刚约束好的约束线
        LineUnit constraintLine;
        if(units.get(size - 1) instanceof LineUnit) {
            constraintLine = (LineUnit) units.get(size - 1);
        } else {
            constraintLine = (LineUnit) units.get(size - 2);
        }
        //是三角形 并且约束线不是中位线
        if(graph instanceof TriangleGraph && constraintLine.getType() != 2){
            popView =  mLayoutInflater.inflate(R.layout.popup_for_tri_constrain, null);
            mPop = new PopupWindow(popView, ThresholdProperty.TRI_WIDTH, ThresholdProperty.POP_HEIGTH);
            mPop.setOutsideTouchable(true);
            popView.findViewById(R.id.vertical_line).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            linearCloseConstraint.changeToVerticalLine(myGraph);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });

            popView.findViewById(R.id.angle_bisector).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            linearCloseConstraint.changeToAngleBisector(myGraph);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.midline).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            linearCloseConstraint.changeToMidLine(myGraph);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            popView.findViewById(R.id.command_line).setOnClickListener(
                    new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            linearCloseConstraint.changeToCommonLine(myGraph);
                            keepConstrainter.keepConstraint(myGraph); //约束保持
                            URSolver.EnUndoStack(graphControl.getConcurrentHashMap()); //改变图形，undo栈添加
                            URSolver.RedoStackClear(); //清空redo
                            // 关闭Popup窗口
                            mPop.dismiss();
                            //cai 2013.4.22 蓝牙传输
                            if(mSynchronousThread.isStart()) {
                                mSynchronousThread.writeGraph(myGraph);
                            }
                        }
                    });
            mPop.showAtLocation(parent,  Gravity.LEFT | Gravity.TOP, x, y);
        }
    }

    public void changeToIsocelesTri(Graph graph){
        double length1 , length2 ,length3;//用来存放两边的长度差的绝对值。
        PointUnit point1 = (PointUnit)(myGraph.getGraph().get(0));
        PointUnit point2 = (PointUnit)(myGraph.getGraph().get(2));
        PointUnit point3 = (PointUnit)(myGraph.getGraph().get(4));
        double distance1 = CommonFunc.distance(point1,point2);
        double distance2 = CommonFunc.distance(point2,point3);
        double distance3 = CommonFunc.distance(point3,point1);
        length1 = Math.abs(distance1 - distance2);
        length2 = Math.abs(distance2 - distance3);
        length3 = Math.abs(distance3 - distance1);
        if(length1 <= length2 && length1 <= length3)
            regulariser.changeToEquation(point1 , point2 , point3);
        if(length2 <= length1 && length2 <= length3)
            regulariser.changeToEquation(point2 , point3 , point1);
        if(length3 <= length1 && length3 <= length2)
            regulariser.changeToEquation(point3 , point1 , point2);
        Log.v("等腰三角形","意图推测");
    }

    public void dismiss(){
        if(mPop != null){
            mPop.dismiss();
        }
    }
}
