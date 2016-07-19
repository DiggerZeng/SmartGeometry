/*
 * 图形变换接口
 * */
package com.sg.logic.strategy;

import java.util.List;

import android.util.Log;

import com.sg.control.GraphControl;
import com.sg.logic.common.CommonFunc;
import com.sg.logic.common.CurveType;
import com.sg.object.Point;
import com.sg.object.graph.CurveGraph;
import com.sg.object.graph.Graph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.unit.CurveUnit;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;

public class TranslationStratery {

    //点在直线上移动
    public static void translatePointInLine(GraphControl graphControl, GUnit unit, Point transPoint){
        Graph graph = graphControl.getGraph(((PointUnit)unit).getKeyOfLineOrCurve());
        if(graph instanceof TriangleGraph){
            int[] ver = new int[2];
            List<GUnit> units = graph.getGraph();
            GUnit line = null;
            for(int i = 0;i < units.size();i++){
                if(units.get(i) == unit){
                    line = units.get(--i);
                    break;
                }
            }
            switch(((PointUnit)unit).getMark()){
                case 0:{
                    ver[0] = 2;
                    ver[1] = 4;
                    break;
                }
                case 2:{
                    ver[0] = 0;
                    ver[1] = 4;
                    break;
                }
                case 4:{
                    ver[0] = 0;
                    ver[1] = 2;
                    break;
                }
            }
            //获取移动点坐标
            double x1 = transPoint.getX();
            double y1 = transPoint.getY();
            Log.v("x1,y1",x1+","+y1);
            //获取三角形边的第一个顶点坐标
            double x2 = ((PointUnit)units.get(ver[0])).getX();
            double y2 = ((PointUnit)units.get(ver[0])).getY();
            Log.v("x2,y2",x2+","+y2);
            //获取三角形的第二个顶点坐标
            double x3 = ((PointUnit)units.get(ver[1])).getX();
            double y3 = ((PointUnit)units.get(ver[1])).getY();
            Log.v("x3,y3",x3+","+y3);
            //记录转换后的约束点坐标
            double xo,yo;

            //计算三角形边的斜率
            double k1 = (y2-y3)/(x2-x3);

            xo = (x1*(x2-x3)+(y2-k1*x2-y1)*(y3-y2))/(k1*(y2-y3)+x2-x3);
            yo = k1*(xo-x2)+y2;

            double distance1 = CommonFunc.distance(new Point((int)x2,(int)y2), new Point((int)xo,(int)yo));
            double distance2 = CommonFunc.distance(new Point((int)x3,(int)y3), new Point((int)xo,(int)yo));
            double a = distance1/distance2;
            if(CommonFunc.distance(new Point((int)x2,(int)y2), new Point((int)xo,(int)yo)) >
                    CommonFunc.distance(new Point((int)x2,(int)y2), new Point((int)x3,(int)y3)))
                a = CommonFunc.distance(new Point((int)x2,(int)y2), new Point((int)x3,(int)y3));
            if(CommonFunc.distance(new Point((int)x3,(int)y3), new Point((int)xo,(int)yo)) >
                    CommonFunc.distance(new Point((int)x2,(int)y2), new Point((int)x3,(int)y3)))
                a = 0;
            Log.v("xo,yo",xo+","+yo);
            //设置新的点坐标
		/*	((PointUnit)unit).setX((int)xo);
			((PointUnit)unit).setY((int)yo);*/
            ((LineUnit)line).setDrag(true);
            ((LineUnit)line).setProportion(a);
            ((LineUnit)line).getEndPointUnit().setX((float)xo);
            ((LineUnit)line).getEndPointUnit().setY((float)yo);
        } else {
            List<GUnit> units = graph.getGraph();
            LineUnit line = null;
            for(GUnit u : units) {
                if(u instanceof LineUnit && u.getID() == ((PointUnit)unit).getIdOfLineOrCurve()) {
                    line = (LineUnit) u;
                    break;
                }
            }
            if(line != null) {
                float x1 = transPoint.getX();
                float y1 = transPoint.getY();
                float x2 = line.getStartPointUnit().getX();
                float y2 = line.getStartPointUnit().getY();
                float x3 = line.getEndPointUnit().getX();
                float y3 = line.getEndPointUnit().getY();
                float k1 = (y2-y3)/(x2-x3);
                float x = (x1*(x2-x3)+(y2-k1*x2-y1)*(y3-y2))/(k1*(y2-y3)+x2-x3);
                float y = k1*(x-x2)+y2;
                ((PointUnit)unit).setX(x);
                ((PointUnit)unit).setY(y);
            }

        }
//
//		//cai
//		if(graph instanceof CurveGraph) {
//			List<GUnit> units = graph.getGraph();
//			LineUnit line = null;
//			for(GUnit u : units) {
//				if(u instanceof LineUnit) {
//					if(((LineUnit)u).getStartPointUnit() == unit || ((LineUnit)u).getEndPointUnit() == unit) {
//						line = (LineUnit) u;
//						break;
//					}
//
//				}
//			}
//			if(line != null) {
//				float x1 = transPoint.getX();
//				float y1 = transPoint.getY();
//				float x2 = line.getStartPointUnit().getX();
//				float y2 = line.getStartPointUnit().getY();
//				float x3 = line.getEndPointUnit().getX();
//				float y3 = line.getEndPointUnit().getY();
//				float k1 = (y2-y3)/(x2-x3);
//				float x = (x1*(x2-x3)+(y2-k1*x2-y1)*(y3-y2))/(k1*(y2-y3)+x2-x3);
//				float y = k1*(x-x2)+y2;
//				((PointUnit)unit).setX(x);
//				((PointUnit)unit).setY(y);
//			}
//
//		}
    }

    //点在曲线线上移动
    public static void translatePointInCurve(GraphControl graphControl, GUnit unit, Point transPoint){
//		CurveUnit curve = null;
//		for(GUnit u : graph.getGraph()) {
//			if(u instanceof CurveUnit && !((CurveUnit) u).isInternallyTangentCircleOfTriangle())
//				curve = (CurveUnit) u;
//		}
        CurveUnit curve = (CurveUnit) graphControl.getGraph(((PointUnit)unit).getKeyOfLineOrCurve()).getGraph().get(0);
        Point point = curve.getCurvePoint((PointUnit) unit, transPoint);
        if(point != null) {
            ((PointUnit)unit).setX(point.getX());
            ((PointUnit)unit).setY(point.getY());
        }

    }

    public static Point findTranslationCenter(Graph graph) {
        float x = 0, y = 0;
        int n = 0;  //n记录点元个数
        if(graph instanceof CurveGraph) {
            for(GUnit unit : graph.getGraph()) {
                if(unit instanceof CurveUnit) {
                    if(((CurveUnit)unit).getCurveType() == CurveType.Circle || ((CurveUnit)unit).getCurveType() == CurveType.Ellipse) {
                        PointUnit center = ((CurveUnit)unit).getCenter();
                        x += center.getX();
                        y += center.getY();
                        n++;
                    } else {
                        List<Point> pList= ((CurveUnit)unit).getPList();
                        n += 2;
                        x += pList.get(0).getX();
                        x += pList.get(pList.size() - 1).getX();
                        y += pList.get(0).getY();
                        y += pList.get(pList.size() - 1).getY();
                    }
                }
            }

        } else {
            for(GUnit unit : graph.getGraph()){
                if(unit instanceof PointUnit){
                    if(!((PointUnit) unit).isInLine() || !(graph instanceof TriangleGraph)) {
                        n ++;
                        x += ((PointUnit)unit).getX();
                        y += ((PointUnit)unit).getY();
                    }
                }
            }

        }
        x /= n;
        y /= n;
        return new Point(x, y);
    }
}
