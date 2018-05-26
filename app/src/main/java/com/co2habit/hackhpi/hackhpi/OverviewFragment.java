package com.co2habit.hackhpi.hackhpi;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OverviewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OverviewFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    int number = 4;

    List<DataPoint> mockPoints = new ArrayList<DataPoint>();


    public OverviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OverviewFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OverviewFragment newInstance(String param1, String param2) {
        OverviewFragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //add mock data
        this.mockPoints.add(new DataPoint(2, 3));
        this.mockPoints.add(new DataPoint(2, -1));
        this.mockPoints.add(new DataPoint(4, 5));
        this.mockPoints.add(new DataPoint(4, -3));
        this.mockPoints.add(new DataPoint(6, 3));
        this.mockPoints.add(new DataPoint(6, -1));
        this.mockPoints.add(new DataPoint(8, 6));
        this.mockPoints.add(new DataPoint(8, -3));
     }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_overview, container, false);


        //GUI code here

        final GraphView graph = (GraphView) view.findViewById(R.id.graph);

        DataPoint[] points = new DataPoint[this.mockPoints.size()];
        points = this.mockPoints.toArray(points);

        final BarGraphSeries<DataPoint> series = new BarGraphSeries<>(points);


        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-4);
        graph.getViewport().setMaxY(7);

        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);


        graph.addSeries(series);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);

        applyStyling(series);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                appendLastDataSet(series, graph);

                final Handler handler2 = new Handler();
                handler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        appendLastDataSet(series, graph);
                    }
                }, 2000);

            }
        }, 3000);



        Log.w("test", "hallo" + "");

        // Inflate the layout for this fragment
        return view;
    }

    public void applyStyling(BarGraphSeries<DataPoint> series) {
        // styling
        series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint data) {
                if(data.getY() < 0){
                    return getResources().getColor(R.color.badRed);
                }
                else{
                    return getResources().getColor(R.color.goodGreen);
                }

            }
        });

        series.setSpacing(50);
        // draw values on top
        series.setDrawValuesOnTop(true);
        series.setValuesOnTopColor(Color.BLUE);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public void appendLastDataSet(BarGraphSeries<DataPoint> data, GraphView graph){
        if(this.number == 4) {
            data.appendData(new DataPoint(10, 5), false, 30);
            this.number++;
        }else{

            //copy global mockPoints
            List<DataPoint> localMockPoints = new ArrayList<DataPoint>(this.mockPoints);
            localMockPoints.add(new DataPoint(10, 6));

            DataPoint[] points = new DataPoint[localMockPoints.size()];
            points = localMockPoints.toArray(points);

            data.resetData(points);
        }

        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(12);
        graph.getViewport().setMinY(-4);
        graph.getViewport().setMaxY(8);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
