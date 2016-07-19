/*
 * 图形绘制控制类
 * 控制图像对象的绘制流程等
 * */

package com.sg.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Canvas;
import android.graphics.Color;

import com.sg.bluetooth.SynchronousThread;
import com.sg.object.Point;
import com.sg.object.constraint.ConstraintStruct;
import com.sg.object.constraint.ConstraintType;
import com.sg.object.graph.CurveGraph;
import com.sg.object.graph.Graph;
import com.sg.object.unit.CurveUnit;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.property.common.ThresholdProperty;
import com.sg.property.tools.Painter;
import com.sg.transformation.recognizer.Recognizer;

public class GraphControl {

    private ConcurrentHashMap<Long,Graph> graphList;
    private Collection<Graph> graphs;
    private List<Graph> constraintGraphs;

    private Painter painter;
    private Painter checkedPainter;

    private Recognizer recognizer;

    private SynchronousThread mSynchronousThread;

    public void changeColor()
    {
        switch(painter.getColor()){
            case Color.BLUE:
                painter.setColor(Color.BLACK);
                break;
            case Color.BLACK:
                painter.setColor(Color.GREEN);
                break;
            case Color.GREEN:
                painter.setColor(Color.YELLOW);
                break;
            case Color.YELLOW:
                painter.setColor(Color.BLUE);
                break;
            default:
                break;
        }
    }

    public GraphControl() {
        graphList = new ConcurrentHashMap<Long,Graph>();
        constraintGraphs = new ArrayList<Graph>();
        recognizer = new Recognizer();
        painter = new Painter(Color.BLACK, ThresholdProperty.DRAW_WIDTH);
        checkedPainter = new Painter(Color.RED, ThresholdProperty.DRAW_WIDTH);
    }

    public void initSynchronousThread(SynchronousThread mSynchronousThread) {
        this.mSynchronousThread = mSynchronousThread;
    }

    /*
     * 在画板canvas上绘制对象列表
     * */
    public void drawGraphList(Canvas canvas) {

        graphs = graphList.values();
        for(Graph graph : graphs) {
            drawGraph(graph, canvas);
        }

    }

    /*
     * 在画板canvas上绘制graph对象
     * */
    public void drawGraph(Graph graph, Canvas canvas) {
        if(graph != null) {
            if(graph.isChecked()) {
                graph.draw(canvas, checkedPainter);
            } else {
                graph.draw(canvas, painter);
            }
        }
    }

    /**
     * 获取选中图形，并把相关约束图形选中
     * 无选中 返回null
     * @param curPoint
     * @return
     */
    public Graph getCheckedGraph(Point curPoint) {
        graphs = graphList.values();
        for(Graph graph : graphs) {
            if(graph.isInGraph(curPoint)) {
                Graph tempGraph = graph;
                checkedGraph(graph, 0, true);
                return tempGraph;
            }
        }
        return null;
    }

    public void checkedGraph(Graph graph, long lastGraphKey, boolean state) {
        if(graph == null)
            return;
        graph.setChecked(state);
        if(mSynchronousThread.isStart()) {
            mSynchronousThread.writeCheckGraph(graph, state);
        }
        if(graph.isGraphConstrainted()) {
            List<ConstraintStruct> constraintStructs = graph.getConstraintStruct();
            for(ConstraintStruct cons : constraintStructs) {
                if(cons.getConstraintGraphKey() != lastGraphKey) {
                    Graph g = graphList.get(cons.getConstraintGraphKey());
                    checkedGraph(g, graph.getID(), state);
                }

            }
        }
    }

    public Graph createGraph(List<Point> pList) {
        return recognizer.recognize(pList);
    }

    /*
     * 添加对象到绘制列表
     * */
    public void addGraph(Graph graph) {
        if(graph != null && !graphList.containsKey(graph.getID())) {
            graphList.put(graph.getID(), graph);
        }
    }

    /**
     * 删除单个图形
     * @param graph
     */
    public void deleteGraph(Graph graph) {
        if(graph != null) {
            if(mSynchronousThread.isStart()) {
                mSynchronousThread.writeDeleteGraph(graph);
            }
            graphList.remove(graph.getID());
        }
    }

    /**
     * 删除约束图形
     * @param graph
     * @param lastGraphKey
     */
    public void deleteConstraintedGraph(Graph graph, long lastGraphKey) {
        if(graph == null)
            return;
        deleteGraph(graph);
        if(graph.isGraphConstrainted()) {
            List<ConstraintStruct> constraintStructs = graph.getConstraintStruct();
            for(ConstraintStruct cons : constraintStructs) {
                if(cons.getConstraintGraphKey() != lastGraphKey) {
                    Graph g = graphList.get(cons.getConstraintGraphKey());
                    deleteConstraintedGraph(g, graph.getID());
                }

            }
        }
    }

    public void replaceGraph(Graph graph) {
        if(graph != null)
            graphList.replace(graph.getID(), graph);
    }

    public void clearGraph() {
        graphList.clear();
    }

    public Graph getGraph(long key) {
        return graphList.get(key);
    }

    public Collection<Graph> getGraphList() {
        return graphList.values();
    }

    public ConcurrentHashMap<Long,Graph> getConcurrentHashMap() {
        return graphList;
    }

    public void setConcurrentHashMap(ConcurrentHashMap<Long,Graph> graphList) {
        this.graphList = graphList;
    }

    /**
     * 获取所有约束图形 使用前需checkedGraphs.clear()
     * @param graph
     * @param lastGraphKey
     */
    private void getConstraintGraphs(Graph graph, long lastGraphKey) {
        if(graph == null)
            return;
        constraintGraphs.add(graph);
        if(graph.isGraphConstrainted()) {
            List<ConstraintStruct> constraintStructs = graph.getConstraintStruct();
            for(ConstraintStruct cons : constraintStructs) {
                if(cons.getConstraintGraphKey() != lastGraphKey) {
                    Graph g = graphList.get(cons.getConstraintGraphKey());
                    getConstraintGraphs(g, graph.getID());
                }

            }
        }
    }

    /**
     * 返回所有约束图形
     * @param graph
     * @return
     */
    public List<Graph> getConstraintGraphs(Graph graph) {
        constraintGraphs.clear();
        getConstraintGraphs(graph, 0);
        return constraintGraphs;
    }

    /**
     * 获取点中选择约束图形中的哪个图形和图元
     * objects[0]图形
     * objects[1]图元
     * @param graph
     * @param point
     * @return
     */
    public Object[] getCurGraphCurUnit(Graph graph, Point point) {
        constraintGraphs.clear();
        getConstraintGraphs(graph, 0);
        Object[] objects = new Object[2];
        objects[0] = null;
        objects[1] = null;
        for(Graph g : constraintGraphs) {
            if(g.isInGraph(point)) {
                objects[0] = g;
                if(objects[1] == null) {
                    for(GUnit u : g.getGraph()) {
                        if(u instanceof PointUnit){
                            if(((PointUnit)u).isInLine() && !((PointUnit)u).isCommonConstrainted())
                                continue;
                            if(u.isInUnit(point)){
                                objects[1] = u;
                                return objects;
                            }
                        }
                        if(u instanceof CurveUnit) {
                            PointUnit startPointUnit = ((CurveUnit) u).getStartPoint();
                            PointUnit endPointUnit = ((CurveUnit) u).getEndPoint();
                            if(startPointUnit != null) {
                                if(startPointUnit.isInUnit(point)) {
                                    objects[1] = startPointUnit;
                                    return objects;
                                }
                            }
                            if(endPointUnit != null) {
                                if(endPointUnit.isInUnit(point)) {
                                    objects[1] = endPointUnit;
                                    return objects;
                                }
                            }
                        }
                    }
                }

            }
        }
        return objects;
    }

    public boolean isInConstraint(Graph graph, Graph curGraph) {
        constraintGraphs.clear();
        getConstraintGraphs(graph, 0);
        for(Graph g : constraintGraphs) {
            if(g.getID() == curGraph.getID())
                return true;
        }
        return false;
    }

    /**
     * 获取点中选中约束图形的哪个圆形
     * @param point
     * @return
     */
    public Graph getCenterGraph(Point point) {
        for(Graph g : constraintGraphs) {
            if(g instanceof CurveGraph) {
                if(((CurveUnit) g.getGraph().get(0)).isInCircle(point)) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * 平移约束图形
     * @param graph
     * @param transMatrix
     * @param lastGraphKey
     */
    public void translateGraph(Graph graph, float[][] transMatrix, long lastGraphKey) {
        if(graph == null)
            return;
        graph.translate(transMatrix);
        if(mSynchronousThread.isStart()) {
            mSynchronousThread.writeGraph(graph);
        }
        if(graph.isGraphConstrainted()) {
            List<ConstraintStruct> constraintStructs = graph.getConstraintStruct();
            for(ConstraintStruct cons : constraintStructs) {
                if(cons.getConstraintGraphKey() != lastGraphKey) {
                    Graph g = graphList.get(cons.getConstraintGraphKey());
                    if(graph instanceof CurveGraph && (cons.getConstraintType() == ConstraintType.HypotenuseOfCircle || cons.getConstraintType() == ConstraintType.TangentOfCircle)) {
                        PointUnit tangPoint;
                        for(GUnit unit : g.getGraph()) {
                            if(unit instanceof PointUnit) {
                                if(((PointUnit) unit).isInLine() || ((PointUnit) unit).isInCurve()) {
                                    unit.translate(transMatrix);
                                }
                            } else {
                                if(unit instanceof LineUnit){
                                    tangPoint = ((LineUnit) unit).getTangentPoint();
                                    if(tangPoint != null) {
                                        tangPoint.translate(transMatrix);
                                    }
                                }
                            }
                        }
                        if(mSynchronousThread.isStart()) {
                            mSynchronousThread.writeGraph(g);
                        }
                    } else {
                        translateGraph(g, transMatrix, graph.getID());
                    }
                }

            }
        }
    }

    /**
     * 缩放约束图形
     * @param graph
     * @param scaleMatrix
     * @param centerGraph
     * @param lastGraphKey
     */
    public void scaleGraph(Graph graph, float[][] scaleMatrix, Point translationCenter, long lastGraphKey) {
        if(graph == null)
            return;
        graph.scale(scaleMatrix, translationCenter);
        if(mSynchronousThread.isStart()) {
            mSynchronousThread.writeGraph(graph);
        }
        if(graph.isGraphConstrainted()) {
            List<ConstraintStruct> constraintStructs = graph.getConstraintStruct();
            for(ConstraintStruct cons : constraintStructs) {
                if(cons.getConstraintGraphKey() != lastGraphKey) {
                    Graph g = graphList.get(cons.getConstraintGraphKey());
                    if(graph instanceof CurveGraph && (cons.getConstraintType() == ConstraintType.HypotenuseOfCircle || cons.getConstraintType() == ConstraintType.TangentOfCircle)) {
                        PointUnit tangPoint;
                        for(GUnit unit : g.getGraph()) {
                            if(unit instanceof PointUnit) {
                                if(((PointUnit) unit).isInLine() || ((PointUnit) unit).isInCurve()) {
                                    unit.scale(scaleMatrix, translationCenter);
                                }
                            } else {
                                if(unit instanceof LineUnit){
                                    tangPoint = ((LineUnit) unit).getTangentPoint();
                                    if(tangPoint != null) {
                                        tangPoint.scale(scaleMatrix, translationCenter);
                                    }
                                }
                            }
                        }
                        if(mSynchronousThread.isStart()) {
                            mSynchronousThread.writeGraph(g);
                        }
                    } else {
                        scaleGraph(g, scaleMatrix ,translationCenter, graph.getID());
                    }
                }

            }
        }
    }

    /**
     * 旋转约束图形
     * @param graph
     * @param rotateMatrix
     * @param translationCenter
     * @param lastGraphKey
     */
    public void rotateGraph(Graph graph, float[][] rotateMatrix, Point translationCenter, long lastGraphKey) {
        if(graph == null)
            return;
        graph.rotate(rotateMatrix, translationCenter);
        if(mSynchronousThread.isStart()) {
            mSynchronousThread.writeGraph(graph);
        }
        if(graph.isGraphConstrainted()) {
            List<ConstraintStruct> constraintStructs = graph.getConstraintStruct();
            for(ConstraintStruct cons : constraintStructs) {
                if(cons.getConstraintGraphKey() != lastGraphKey) {
                    Graph g = graphList.get(cons.getConstraintGraphKey());
                    if(graph instanceof CurveGraph && (cons.getConstraintType() == ConstraintType.HypotenuseOfCircle || cons.getConstraintType() == ConstraintType.TangentOfCircle)) {
                        PointUnit tangPoint;
                        for(GUnit unit : g.getGraph()) {
                            if(unit instanceof PointUnit) {
                                if(((PointUnit) unit).isInLine() || ((PointUnit) unit).isInCurve()) {
                                    unit.rotate(rotateMatrix, translationCenter);
                                }
                            } else {
                                if(unit instanceof LineUnit){
                                    tangPoint = ((LineUnit) unit).getTangentPoint();
                                    if(tangPoint != null) {
                                        tangPoint.rotate(rotateMatrix, translationCenter);
                                    }
                                }
                            }
                        }
                        if(mSynchronousThread.isStart()) {
                            mSynchronousThread.writeGraph(g);
                        }
                    } else {
                        rotateGraph(g, rotateMatrix ,translationCenter, graph.getID());
                    }
                }

            }
        }
    }

}
