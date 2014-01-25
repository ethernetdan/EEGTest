package com.ampvita.eegtest;

import java.util.ArrayList;

/**
 * Created by mrgubbels on 1/25/14.
 */
public class BrainStateModel {
    private ArrayList<Integer> averagePoints; // The average of all the captured points
    private ArrayList<ArrayList<Integer>> capturedPoints; // All of the captured points
    private String label; // The label to associate with the model
}
