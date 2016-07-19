package com.sg.bluetooth;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import android.R.integer;
import android.util.Log;

import com.sg.logic.common.CurveType;
import com.sg.object.Point;
import com.sg.object.graph.Graph;
import com.sg.object.graph.PointGraph;
import com.sg.object.graph.Sketch;
import com.sg.object.unit.CurveUnit;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.transformation.collection.Stroke;

public class SynchronousThread implements Runnable {

    private BluetoothService mBluetoothService;
    private boolean isRun;
    private boolean isStart;
    private Queue<String> messages;
    private Collection<Graph> graphList = null;

    public SynchronousThread(BluetoothService bluetoothService) {
        mBluetoothService = bluetoothService;
        messages = new LinkedList<String>();
        isRun = true;
        isStart = false;
        new Thread(this).start();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        while(isRun) {
            try {
                Thread.sleep(16);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if(isStart) {
                if(mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                    if(!messages.isEmpty()) {
                        try {
                            mBluetoothService.write(messages.poll().getBytes("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if(graphList != null) {
                        writeGraphList();
                        graphList = null;
                    }
                }
            }
        }
    }

    public boolean isStart() {
        return isStart;
    }

    public synchronized void sendMessage(String s) {
        messages.offer(s);
    }


    public synchronized void sendMessage(Collection<Graph> graphList) {
        // TODO Auto-generated method stub
        this.graphList = graphList;
    }

    private synchronized void writeGraphList() {
        //StringBuilder s = new StringBuilder();
        try {
            for(Graph graph : graphList) {
                writeGraph(graph);
//				if (graph instanceof Sketch) {
//					s.append(writeSketch((Sketch) graph));
//
//				} else {
//					if (graph instanceof PointGraph) {
//
//						s.append(writePoint(graph));
//					} else {
//
//						s.append(writeComGraph(graph));
//					}
//				}
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.v("writeGraphList", e.toString());
            return;
        }

        //messages.offer(s.toString());
    }

    public synchronized void writeGraph(Graph graph) {
        StringBuilder s = new StringBuilder();
        if (graph instanceof Sketch) {
            s = (writeSketch((Sketch) graph));

        } else {
            if (graph instanceof PointGraph) {

                s = (writePoint(graph));
            } else {

                s = (writeComGraph(graph));
            }
        }
        messages.offer(s.toString());
    }

    public synchronized void writeDeleteGraph(Graph graph) {
        StringBuilder s = new StringBuilder();
        s.append("AR");
        s.append(writeGraphID(graph));
        s.append("Z");
        messages.offer(s.toString());
    }

    public synchronized void writeCheckGraph(Graph graph, boolean state) {
        StringBuilder s = new StringBuilder();
        s.append("A");
        if(state) {
            s.append("Y");
        } else {
            s.append("N");
        }
        s.append(writeGraphID(graph));
        s.append("Z");
        messages.offer(s.toString());
    }

    private StringBuilder writeGraphID(Graph graph) {
        StringBuilder s = new StringBuilder();
        if (graph instanceof Sketch) {
            s.append(graph.getID() + "E");
        } else {
            if (graph instanceof PointGraph) {

                s.append(graph.getGraph().get(0).getID() + "E");
            } else {

                List<GUnit> units = graph.getGraph();
                for (GUnit unit : units) {
                    if (unit instanceof LineUnit || unit instanceof CurveUnit) {
                        s.append(unit.getID() + "E");
                        if (unit instanceof CurveUnit && ((CurveUnit) unit).getCurveType() == CurveType.Circle) {
                            s.append(((CurveUnit) unit).getCenter().getID() + "E");
                        }
                    }
                }
            }
        }
        return s;
    }

    private StringBuilder writeSketch(Sketch sketch) {
        StringBuilder s = new StringBuilder();
        s.append("AS");
        if(sketch.isChecked())
            s.append("Y");
        else
            s.append("N");
        s.append(sketch.getID() + ",");
        List<Point> pList = sketch.getPList();
        for (Point point : pList) {
            s.append(point.getX() + "," + point.getY() + ",");
        }
        int size = s.length();
        s.setCharAt(size-1, 'E');
        s.append("Z");
        return s;
    }

    private StringBuilder writePoint(Graph pointGraph) {
        StringBuilder s = new StringBuilder();
        s.append("A");
        s.append("P");
        if(pointGraph.isChecked())
            s.append("Y");
        else
            s.append("N");
        PointUnit pointUnit = (PointUnit) pointGraph.getGraph().get(0);
        s.append(pointUnit.getID() + ",");
        s.append(pointUnit.getX() + "," + pointUnit.getY() + "E");
        s.append("Z");
        return s;
    }

    private StringBuilder writeComGraph(Graph graph) {
        StringBuilder s = new StringBuilder();
        List<GUnit> units = graph.getGraph();
        s.append("A");
        String state;
        if(graph.isChecked())
            state = "Y";
        else
            state = "N";
        for (GUnit unit : units) {
            if (unit instanceof LineUnit) {
                s.append("L");
                s.append(state);
                s.append(unit.getID() + ",");
                s.append(((LineUnit) unit).getStartPointUnit().getX() + "," + ((LineUnit) unit).getStartPointUnit().getY() + ","
                        + ((LineUnit) unit).getEndPointUnit().getX() + "," +((LineUnit) unit).getEndPointUnit().getY() + "E");
            } else if (unit instanceof CurveUnit) {
                if(((CurveUnit) unit).getCurveType() == CurveType.Circle) {
                    s.append("P");
                    s.append(state);
                    PointUnit pointUnit = ((CurveUnit) unit).getCenter();
                    s.append(pointUnit.getID() + ",");
                    s.append(pointUnit.getX() + "," + pointUnit.getY() + "E");
                }
                s.append("S");
                s.append(state);
                s.append(unit.getID() + ",");
                List<Point> pList = ((CurveUnit) unit).getPList();
                for (int i = ((CurveUnit) unit).getStartIndex(); i <= ((CurveUnit) unit).getEndIndex(); i++) {
                    s.append(pList.get(i).getX() + "," + pList.get(i).getY() + ",");
                }
                int size = s.length();
                s.setCharAt(size-1, 'E');
            }
        }
        s.append("Z");
        return s;
    }

    public void pause() {
        isStart = false;
    }

    public void start() {
        isStart = true;
    }

    public void stop() {
        isRun = false;
    }

}
