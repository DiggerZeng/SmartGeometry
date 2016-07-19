package com.sg.transformation.computeagent;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.sg.control.GraphControl;
import com.sg.control.OperationType;
import com.sg.control.UndoRedoSolver;
import com.sg.control.UndoRedoStruct;
import com.sg.logic.common.CommonFunc;
import com.sg.object.graph.Sketch;
import com.sg.object.graph.Graph;
import com.sg.object.graph.LineGraph;
import com.sg.object.graph.RectangleGraph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.graph.CurveGraph;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.property.common.ThresholdProperty;

public class Constrainter {

    private static Constrainter instance = new Constrainter();

    //private PointUnit constraintPointUnit;

    private UndoRedoSolver URSolver;
    private LinearCloseConstraint linearCloseConstraint;
    private LineToLineConstraint lineToLineConstraint;
    private CurveConstraint curveConstraint;
    private KeepConstrainter keepConstrainter;

    private Constrainter() {
        URSolver = UndoRedoSolver.getInstance();
        keepConstrainter = KeepConstrainter.getInstance();
        linearCloseConstraint = new LinearCloseConstraint();
        lineToLineConstraint = new LineToLineConstraint();
        curveConstraint = new CurveConstraint();
    }

    public static Constrainter getInstance() {
        return instance;
    }


    public Graph constraint(GraphControl graphControl, Graph curGraph) {
        Graph constraintGraph = null;
        Graph del = null;
        //不闭合直线 先与 不闭合直线约束识别
        if(curGraph instanceof LineGraph && !curGraph.isClosed()) {
            boolean isALine = (curGraph.getGraph().size() == 3);
            for(Graph graph : graphControl.getGraphList()) {
                if(graph instanceof LineGraph && !graph.isClosed() && graph != curGraph) {
                    if(!curGraph.isGraphConstrainted()) {
                        constraintGraph = lineToLineConstraint.lineToLineConstrain(graph, curGraph);
                        del = curGraph;
                        if(constraintGraph != null)
                            break;
                    } else {
                        if(!graph.isGraphConstrainted()) {
                            constraintGraph = lineToLineConstraint.lineToLineConstrain(curGraph, graph);
                            del = graph;
                            if(constraintGraph != null)
                                break;
                        }
                    }
                }
                //单条直线和三角形
                if(isALine && graph instanceof TriangleGraph || graph instanceof RectangleGraph){  //如果是三角形或四边形（只写了直线与三角形）
                    constraintGraph = linearCloseConstraint.linearConstraint(graph, curGraph);
                    del = curGraph;
                    if(constraintGraph != null)
                        break;
                }
            }
        }

        if(constraintGraph != null){   //如果有约束
            graphControl.deleteGraph(del);
            //如果约束后还是折线   第二次约束识别
            if(constraintGraph instanceof LineGraph && !constraintGraph.isClosed()) {
                Graph temp = constraintGraph;
                del = null;
                Graph cons = null;
                for(Graph graph : graphControl.getGraphList()) {
                    if(graph instanceof LineGraph && !graph.isClosed() && graph != temp) {
                        if(!temp.isGraphConstrainted()) {
                            cons = lineToLineConstraint.lineToLineConstrain(graph, temp);
                            del = temp;
                            if(cons != null)
                                break;
                        } else {
                            if(!graph.isGraphConstrainted()) {
                                cons = lineToLineConstraint.lineToLineConstrain(temp, graph);
                                del = graph;
                                if(cons != null)
                                    break;
                            }
                        }

                    }
                }
                if(cons != null){
                    graphControl.deleteGraph(del);
                    constraintGraph = cons;
                }
            }
            Graph temp = lineToLineConstraint.rebuildLinearClose(graphControl, constraintGraph);  //重构三角形，四边形
            if(temp == null)
                temp = constraintGraph;
            keepConstrainter.keepConstraint(temp);
//			if(curGraph.isChecked()){                     //如果curGraph是选中的图形 于其他图形有约束关系
////				temp.setChecked(false);
////				graphControl.checkedGraph(temp, 0, false);
//				URSolver.EnUndoStack(new UndoRedoStruct(OperationType.MOVEANDCONSTRAIN, temp.clone()));
//			}else{
//				URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, temp.clone()));
//			}
            Log.v("有约束", "有约束");
            return temp;
        }
        if(curGraph instanceof LineGraph && curGraph.getGraph().size() == 3) {      //对一条直线图进行约束识别
            for(Graph graph : graphControl.getGraphList()) {
//				if(graph instanceof LineGraph && !graph.isClosed() && graph != curGraph){ //如果图形是直线 不闭合图形
//					constraintGraph = lineToLineConstraint.lineToLineConstrain(graph, curGraph);
//					if(constraintGraph != null)
//						break;
//				}else{
//					if(graph instanceof TriangleGraph || graph instanceof RectangleGraph){  //如果是三角形或四边形（只写了直线与三角形）
//						constraintGraph = linearCloseConstraint.linearConstraint(graph, curGraph);
//						if(constraintGraph != null)
//							break;
//					} else {
                if(graph instanceof CurveGraph && !graphControl.isInConstraint(graph, curGraph)) { //直线和曲线
                    constraintGraph = curveConstraint.lineToCurveConstrain(graph, curGraph);
                    if(constraintGraph != null)
                        break;
                }
//					}
//				}
            }
        }

        //对只有一个曲线元的曲线进行约束识别
        if(curGraph instanceof CurveGraph && curGraph.getGraph().size() == 1) {
            for(Graph graph : graphControl.getGraphList()) {
                //圆与圆约束  不能是三角形的内切圆
                if(graph instanceof CurveGraph && graph != curGraph
                        && !((CurveGraph)curGraph).isTriangleConstraint()
                        && !((CurveGraph)graph).isTriangleConstraint()
                        && !graphControl.isInConstraint(graph, curGraph)){
                    constraintGraph = curveConstraint.curveToCurveConstrain(graph, curGraph);
                    if(constraintGraph != null)
                        break;
                } else {
                    if(graph instanceof LineGraph && !graphControl.isInConstraint(graph, curGraph)) {
                        constraintGraph = curveConstraint.curveToLineConstrain(graph, curGraph);
                        if(constraintGraph != null) {
//							constraintGraph.setID(graph.getID());
//							graphControl.deleteGraph(graph);
//							graphControl.addGraph(constraintGraph);
                            break;
                        }
                    }
                    if(graph instanceof TriangleGraph && !graphControl.isInConstraint(graph, curGraph)) {
                        constraintGraph = curveConstraint.curveToTriConstrain(graphControl, graph, curGraph);
                        if(constraintGraph != null)
                            break;
                    }
                    if(graph instanceof RectangleGraph && !graphControl.isInConstraint(graph, curGraph)) {
                        constraintGraph = curveConstraint.curveToRectConstrain(graph, curGraph);
                        if(constraintGraph != null)
                            break;
                    }
                }
            }
        }

        //三角形和曲线
        if(curGraph instanceof TriangleGraph) {
            for(Graph graph : graphControl.getGraphList()) {
                if(graph instanceof CurveGraph && !graphControl.isInConstraint(graph, curGraph)) {
                    constraintGraph = curveConstraint.triToCurveConstrain(graphControl, graph, curGraph);
                    if(constraintGraph != null) {
//						constraintGraph.setID(graph.getID());
//						graphControl.deleteGraph(graph);
//						graphControl.addGraph(constraintGraph);
                        break;
                    }
                }
            }
        }

        //四边形和曲线
        if(curGraph instanceof RectangleGraph) {
            for(Graph graph : graphControl.getGraphList()) {
                if(graph instanceof CurveGraph && !graphControl.isInConstraint(graph, curGraph)) {
                    constraintGraph = curveConstraint.rectToCurveConstrain(graph, curGraph);
                    if(constraintGraph != null)
                        break;
                }
            }
        }

        if(constraintGraph != null){   //如果有约束
            Graph temp = lineToLineConstraint.rebuildLinearClose(graphControl, constraintGraph);  //重构三角形，四边形
            if(temp == null)
                temp = constraintGraph;
            keepConstrainter.keepConstraint(temp);
            if(curGraph.isChecked()){
                graphControl.checkedGraph(curGraph, 0, false);
            }
//			if(curGraph.isChecked()){                     //如果curGraph是选中的图形 于其他图形有约束关系，则需删除curGraph
////				temp.setChecked(false);
//				graphControl.checkedGraph(temp, 0, false);
//				URSolver.EnUndoStack(new UndoRedoStruct(OperationType.MOVEANDCONSTRAIN, temp.clone()));
//			}else{
//				URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, temp.clone()));
//			}
            Log.v("有约束", "有约束");
            return temp;
        }else{                        //如果没有约束关系
//			if(!curGraph.isChecked()) {            //如果curGraph没被选中，即是刚画上去的，则在图形链表添加
//				//if(curGraph instanceof Sketch){
//				//	URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CREATE, curGraph));
//				//}else{
//				//cai 2013.8.29 ?
////				if (curGraph instanceof Sketch) {
////					curGraph.setID(GUnit.getStaticID());
////				}
//					URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CREATE, curGraph.clone()));
//				//}
//			}else{
//				URSolver.EnUndoStack(new UndoRedoStruct(OperationType.CHANGE, curGraph.clone()));
//			}
            Log.v("没有约束", "没有约束");
            return null;
        }
    }
}
